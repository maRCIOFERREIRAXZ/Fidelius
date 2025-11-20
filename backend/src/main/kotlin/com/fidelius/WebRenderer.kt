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
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Registers routes to serve the web frontend.
 */
object WebRenderer {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val classLoader = Thread.currentThread().contextClassLoader

    private fun load(path: String): ByteArray? =
        classLoader.getResourceAsStream(path)?.use { it.readBytes() }

    private val indexHtml: ByteArray? = load("static/index.html")
    private val viewHtml: ByteArray? = load("static/view.html")

    /**
     * Registers routes to serve the web frontend.
     *
     * @param app The Ktor application to register routes on.
     */
    fun registerFrontend(app: Application) {
        app.routing {

            // Serve static files under resources/static
            static("/static") {
                staticResources("", "static")
            }

            get("/") {
                val bytes = indexHtml
                if (bytes == null) {
                    call.respondText("Not Found", ContentType.Text.Plain, HttpStatusCode.NotFound)
                    logger.error("index.html not found in resources/static/")
                } else {
                    call.respondBytes(bytes, ContentType.Text.Html)
                }
            }

            get("/s/{id}") {
                val bytes = viewHtml
                if (bytes == null) {
                    call.respondText("Not Found", ContentType.Text.Plain, HttpStatusCode.NotFound)
                    logger.error("view.html not found in resources/static/")
                } else {
                    call.respondBytes(bytes, ContentType.Text.Html)
                }
            }

            // JS Config to expose server-side config to frontend
            get("/static/js/config.js") {
                val secretMaxChars = Config.secretMaxChars
                val js = "window.FIDELIUS_SECRET_MAX_CHARS = $secretMaxChars;"
                call.respondText(js, ContentType.Application.JavaScript)
            }
        }
    }
}