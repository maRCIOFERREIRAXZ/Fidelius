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

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

/**
 * Setup Ktor application with necessary plugins and routes
 */
fun Application.setup() {

    // JSON serialization
    install(ContentNegotiation) {
        json(Json { prettyPrint = false })
    }

    // Handle 404 errors with custom page
    install(StatusPages) {
        status(HttpStatusCode.NotFound) { call, status ->
            // show err_404.html page for 404s
            call.respondText(
                WebRenderer.err404Html?.toString(Charsets.UTF_8)
                    ?: "404 Not Found",
                ContentType.Text.Html,
                status
            )
        }
    }

    // Inject strict security headers
    intercept(ApplicationCallPipeline.Setup) {
        Security.appendSecurityHeaders(call)
    }

    // Setup routing
    routing {
        WebRenderer.registerFrontend(this@setup)
        registerRoutes() // Register API routes
    }
}