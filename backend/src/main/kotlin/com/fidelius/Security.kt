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

import io.ktor.server.application.*

/**
 * Appends security headers to the HTTP response.
 *
 * @param call The application call to modify.
 */
object Security {

    /**
     * Appends various security headers and a CSP policy to the HTTP response.
     *
     * @param call The application call to modify.
     */
    fun appendSecurityHeaders(call: ApplicationCall) {
        call.response.headers.append("X-Content-Type-Options", "nosniff")
        call.response.headers.append("X-Frame-Options", "DENY")
        call.response.headers.append("Referrer-Policy", "no-referrer")
        call.response.headers.append("Permissions-Policy", "geolocation=()")
        // CSP policy to only allow scripts and styles from self and
        // Google Fonts for styles and fonts.
        val csp = buildString {
            append("default-src 'none'; ")
            append("script-src 'self'; ")
            append("style-src 'self' https://fonts.googleapis.com; ")
            append("font-src https://fonts.gstatic.com; ")
            append("connect-src 'self'; ")
            append("img-src 'self' data:; ")
            append("frame-ancestors 'none'; ")
            append("base-uri 'self'; ")
            append("form-action 'self';")
        }
        call.response.headers.append("Content-Security-Policy", csp)
    }
}