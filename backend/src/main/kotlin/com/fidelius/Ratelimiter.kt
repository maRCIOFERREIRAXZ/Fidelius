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


import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A simple token bucket rate limiter.
 * Allows `perMinute` requests per minute per IP address.
 */
class RateLimiter(private val perMinute: Int) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private data class Bucket(var tokens: Double, var lastRefillSec: Long)

    private val map = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(ip: String): Boolean {
        val now = Instant.now().epochSecond
        val bucket = map.computeIfAbsent(ip) { Bucket(perMinute.toDouble(), now) }
        synchronized(bucket) {
            val elapsed = now - bucket.lastRefillSec
            if (elapsed > 0) {
                val refill = elapsed * (perMinute / 60.0)
                bucket.tokens = (bucket.tokens + refill).coerceAtMost(perMinute.toDouble())
                bucket.lastRefillSec = now
            }
            return if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0
                true
            } else {
                logger.warn("Rate limit exceeded for IP: $ip")
                false
            }
        }
    }
}
