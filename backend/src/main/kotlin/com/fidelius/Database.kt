package com.fidelius

import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager
import java.time.Instant

/**
 * Database object to manage SQLite operations
 */
object Database {
    private lateinit var jdbcUrl: String
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Initialize DB directory and SQLite file.
     */
    fun init(jdbc: String) {
        jdbcUrl = jdbc

        val filePath = jdbc.removePrefix("jdbc:sqlite:")
        val dbFile = File(filePath)
        val dir = dbFile.parentFile

        // Ensure directory exists
        if (dir != null && !dir.exists()) {
            logger.info("Creating data directory: ${dir.absolutePath}")
            dir.mkdirs()
        }

        // Writability check (for Docker bind mounts)
        if (dir != null && (!dir.canWrite() || !dir.canRead())) {
            throw IllegalStateException(
                "Data directory not writable: ${dir.absolutePath}. " +
                        "Fix your Docker bind mount permissions."
            )
        }

        try {
            DriverManager.getConnection(jdbcUrl).use { c ->
                c.createStatement().use { st ->
                    st.execute(
                        """
                        CREATE TABLE IF NOT EXISTS secrets (
                            id TEXT PRIMARY KEY,
                            ciphertext TEXT NOT NULL,
                            nonce TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            expires_at INTEGER
                        )
                        """.trimIndent()
                    )
                }
            }
            logger.info("Database initialized successfully at $filePath")
        } catch (e: Exception) {
            logger.error("Failed to initialize SQLite database at $filePath", e)
            throw e
        }
    }

    /**
     * Insert a new secret into the database.
     *
     * @param id Unique identifier for the secret
     * @param ciphertext Encrypted secret data
     * @param nonce Nonce used for encryption
     * @param expiresAtEpochSec Expiration time in epoch seconds
     */
    fun insertSecret(id: String, ciphertext: String, nonce: String, expiresAtEpochSec: Long) {
        DriverManager.getConnection(jdbcUrl).use { c ->
            c.prepareStatement(
                "INSERT INTO secrets(id,ciphertext,nonce,created_at,expires_at) VALUES(?,?,?,?,?)"
            ).use { ps ->
                ps.setString(1, id)
                ps.setString(2, ciphertext)
                ps.setString(3, nonce)
                ps.setLong(4, Instant.now().epochSecond)
                ps.setLong(5, expiresAtEpochSec)
                ps.executeUpdate()
            }
        }
    }

    /**
     * Fetch and delete a secret by its ID.
     *
     * @param id Unique identifier for the secret
     * @return Pair of ciphertext and nonce if found and not expired, null otherwise
     */
    fun fetchAndDelete(id: String): Pair<String, String>? {
        DriverManager.getConnection(jdbcUrl).use { c ->
            c.autoCommit = false
            try {
                val select = c.prepareStatement("SELECT ciphertext, nonce, expires_at FROM secrets WHERE id = ?")
                select.setString(1, id)
                val rs = select.executeQuery()
                if (!rs.next()) {
                    c.rollback()
                    return null
                }

                val expires = rs.getLong("expires_at")
                val now = Instant.now().epochSecond

                if (expires in 1..<now) {
                    c.prepareStatement("DELETE FROM secrets WHERE id = ?").use { d ->
                        d.setString(1, id)
                        d.executeUpdate()
                    }
                    c.commit()
                    return null
                }

                val ciphertext = rs.getString("ciphertext")
                val nonce = rs.getString("nonce")

                c.prepareStatement("DELETE FROM secrets WHERE id = ?").use { d ->
                    d.setString(1, id)
                    d.executeUpdate()
                }

                c.commit()
                return ciphertext to nonce
            } catch (t: Throwable) {
                c.rollback()
                throw t
            }
        }
    }

    /**
     * Cleanup expired secrets from the database.
     */
    fun cleanupExpired() {
        DriverManager.getConnection(jdbcUrl).use { c ->
            c.prepareStatement("DELETE FROM secrets WHERE expires_at < ?").use { ps ->
                ps.setLong(1, Instant.now().epochSecond)
                val deleted = ps.executeUpdate()
                if (deleted > 0) logger.info("Cleaned up $deleted expired secrets.")
            }
        }
    }
}