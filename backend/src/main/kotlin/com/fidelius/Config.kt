/**
 * MIT License
 *
 * Copyright (c) [2025 - Present] Stɑrry Shivɑm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.fidelius

import kotlin.math.ceil

/**
 * Configuration object to hold application settings
 */
object Config {
    val port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val dataDirPath: String = System.getenv("DATA_DIR") ?: "./data"
    val jdbcUrl: String = "jdbc:sqlite:${dataDirPath.trimEnd('/')}/secrets.db"

    val keepMaxDays: Long = System.getenv("KEEP_MAX_DAYS")?.toLongOrNull() ?: 30L
    val secretMaxChars: Int = System.getenv("SECRET_MAX_CHARS")?.toIntOrNull() ?: 5000
    val rateLimitPerMinute: Int = System.getenv("RATE_LIMIT_PER_MINUTE")?.toIntOrNull() ?: 10
    val cleanupIntervalMinutes: Long = System.getenv("CLEANUP_INTERVAL_MINUTES")?.toLongOrNull() ?: 60L

    // Calculate the maximum possible length of the ciphertext in Base64 encoding
    val maxCiphertextBase64Length: Int
        get() {
            val maxPlainBytes = secretMaxChars * 4 // worst-case UTF-8 bytes per char
            val tagBytes = 16
            val ivBytes = 12
            val raw = maxPlainBytes + tagBytes + ivBytes
            val base64Len = 4 * ceil(raw / 3.0).toInt()
            return base64Len + 128 // safety margin
        }
}