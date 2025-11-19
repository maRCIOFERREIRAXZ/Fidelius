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


"use strict";

async function postJson(url, body) {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  return res;
}

async function getJson(url) {
  const res = await fetch(url, { method: "GET" });
  return res;
}

function assertCrypto() {
  if (!window.FIDELIUS_CRYPTO || typeof window.FIDELIUS_CRYPTO.encryptText !== "function" || typeof window.FIDELIUS_CRYPTO.decryptToText !== "function") {
    throw new Error("Encryption/Decryption module not available.");
  }
}

/**
 * Create a secret:
 * - encrypts plaintext with window.FIDELIUS_CRYPTO.encryptText
 * - POSTs ciphertext/nonce to /api/v1/create
 * - returns { id, key, link }
 */
export async function createSecret(plaintext) {
  assertCrypto();
  // encrypt locally
  const { key, ciphertext, iv } = await window.FIDELIUS_CRYPTO.encryptText(plaintext);
  const res = await postJson("/api/v1/create", { ciphertext, nonce: iv });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error("Server error: " + res.status + " " + body);
  }
  const j = await res.json();
  if (!j || !j.id) throw new Error("Invalid server response");

  const id = j.id;
  const link = location.origin + "/s/" + encodeURIComponent(id) + "#" + key;
  return { id, key, link };
}

/**
 * Fetch a secret's ciphertext and nonce by id.
 * Throws for 404 or non-ok responses (caller may handle specially).
 */
export async function fetchSecret(id) {
  const res = await getJson("/api/v1/secrets/" + encodeURIComponent(id));
  if (res.status === 404) {
    const err = new Error("Not Found");
    err.code = 404;
    throw err;
  }
  if (!res.ok) {
    const err = new Error("Server error");
    err.code = res.status;
    throw err;
  }
  const j = await res.json();
  if (!j || !j.ciphertext || !j.nonce) {
    const err = new Error("Bad server response");
    err.code = 502;
    throw err;
  }
  return { ciphertext: j.ciphertext, nonce: j.nonce };
}

/**
 * Fetches and decrypts a secret:
 * - fetches ciphertext/nonce for id
 * - uses window.FIDELIUS_CRYPTO.decryptToText(key, ciphertext, nonce)
 * - returns plaintext (or throws)
 */
export async function fetchAndDecrypt(id, key) {
  assertCrypto();
  const { ciphertext, nonce } = await fetchSecret(id);
  const plain = await window.FIDELIUS_CRYPTO.decryptToText(key, ciphertext, nonce);
  return plain;
}