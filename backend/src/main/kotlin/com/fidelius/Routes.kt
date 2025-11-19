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
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

fun Application.registerRoutes() {
    val limiter = RateLimiter(Config.rateLimitPerMinute)

    routing {
        post("/api/v1/create") {
            val ip = call.request.headers["x-forwarded-for"] ?: call.request.local.remoteHost
            if (!limiter.tryConsume(ip)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "rate_limit"))
                return@post
            }

            val req = try {
                call.receive<CreateRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_json"))
                return@post
            }

            if (req.ciphertext.isBlank() || req.nonce.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_payload"))
                return@post
            }

            if (req.ciphertext.length > Config.maxCiphertextBase64Length) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ciphertext_too_large"))
                return@post
            }

            val id = UUID.randomUUID().toString().replace("-", "").take(24)
            val expires = Instant.now().plus(Duration.ofDays(Config.keepMaxDays)).epochSecond

            Database.insertSecret(id, req.ciphertext, req.nonce, expires)
            call.respond(mapOf("id" to id))
        }

        get("/api/v1/secrets/{id}") {
            val ip = call.request.headers["x-forwarded-for"] ?: call.request.local.remoteHost
            if (!limiter.tryConsume(ip)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "rate_limit"))
                return@get
            }

            val id = call.parameters["id"] ?: run { call.respond(HttpStatusCode.BadRequest); return@get }
            val pair = Database.fetchAndDelete(id)
            if (pair == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.respond(mapOf("ciphertext" to pair.first, "nonce" to pair.second))
        }
    }
}

@Serializable
data class CreateRequest(val ciphertext: String, val nonce: String)
