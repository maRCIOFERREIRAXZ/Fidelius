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

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

private val logger = LoggerFactory.getLogger("Fidelius")

fun Application.setup() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = false })
    }

    // Inject strict CSP headers
    intercept(ApplicationCallPipeline.Setup) {
        Security.appendSecurityHeaders(call)
    }

    // Setup routing
    routing {
        WebRenderer.registerFrontend(this@setup)
        registerRoutes() // Register API routes
    }
}

fun main() {
    val port = Config.port

    logger.info("Initializing database at ${Config.jdbcUrl}")
    Database.init(Config.jdbcUrl)

    // Background cleanup job scope
    val bgScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val server = embeddedServer(Netty, host = "0.0.0.0", port = port) { setup() }

    logger.info("Starting background cleanup job every ${Config.cleanupIntervalMinutes} minutes")
    bgScope.launch {
        delay(Duration.ofSeconds(5).toMillis())
        while (isActive) {
            try {
                logger.info("Running cleanup of expired secrets")
                Database.cleanupExpired()
            } catch (t: Throwable) {
                System.err.println("[cleanup] error: ${t.message}")
            }
            delay(Config.cleanupIntervalMinutes * 60 * 1000L)
        }
    }

    server.start(wait = true)
}
