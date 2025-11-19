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

/*
  Web Crypto wrapper for encryption/decryption of secrets in the browser.
  Exposes window.FIDELIUS_CRYPTO with these methods:
    - generateKeyRaw()
    - importKeyRaw(raw)
    - encryptText(plaintext) -> { key: b64url, ciphertext: b64url, iv: b64url }
    - decryptToText(keyB64url, cipherB64url, ivB64url) -> plaintext
*/

(function () {
  function b64EncodeChunked(buffer) {
    // buffer: ArrayBuffer or Uint8Array
    const bytes = buffer instanceof Uint8Array ? buffer : new Uint8Array(buffer);
    const CHUNK = 0x8000; // safe chunk size
    let str = "";
    for (let i = 0; i < bytes.length; i += CHUNK) {
      const chunk = bytes.subarray(i, i + CHUNK);
      str += String.fromCharCode.apply(null, chunk);
    }
    return btoa(str);
  }

  function b64DecodeToUint8Array(b64) {
    const binary = atob(b64);
    const len = binary.length;
    const arr = new Uint8Array(len);
    for (let i = 0; i < len; i++) arr[i] = binary.charCodeAt(i);
    return arr;
  }

  function toBase64Url(buffer) {
    // returns base64url (no padding)
    const b64 = b64EncodeChunked(buffer);
    return b64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=*$/, "");
  }

  function fromBase64Url(b64url) {
    // restore padding
    let b64 = b64url.replace(/-/g, "+").replace(/_/g, "/");
    const pad = b64.length % 4;
    if (pad === 2) b64 += "==";
    else if (pad === 3) b64 += "=";
    else if (pad !== 0) throw new Error("Invalid base64url string");
    return b64DecodeToUint8Array(b64);
  }


  // Ensure Web Crypto API is available otherwise throw so UI can handle it and
  // show an error message.
  function ensureSubtle() {
    if (typeof window === "undefined" || !window.crypto || !window.crypto.subtle) {
      throw new Error("Web Crypto API (crypto.subtle) not available in this context.");
    }
    return window.crypto.subtle;
  }

  function generateKeyRaw() {
    // 256-bit key
    return window.crypto.getRandomValues(new Uint8Array(32));
  }

  async function importKeyRaw(raw) {
    try {
      const subtle = ensureSubtle();
      let ab;
      if (raw instanceof ArrayBuffer) ab = raw;
      else if (raw instanceof Uint8Array) {
        // slice the region matching the view to avoid passing the entire underlying buffer
        ab = raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.byteLength);
      } else {
        throw new Error("importKeyRaw expects ArrayBuffer or Uint8Array");
      }
      return await subtle.importKey("raw", ab, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
    } catch (err) {
      throw new Error("Failed to import key using Web Crypto API.");
    }
  }

  async function encryptText(plaintext) {
    try {
      const subtle = ensureSubtle();

      const keyRaw = generateKeyRaw();
      const key = await importKeyRaw(keyRaw);

      const iv = window.crypto.getRandomValues(new Uint8Array(12));
      const enc = new TextEncoder().encode(plaintext);
      const cipherBuf = await subtle.encrypt(
        { name: "AES-GCM", iv, tagLength: 128 },
        key,
        enc
      );

      return {
        key: toBase64Url(keyRaw),
        ciphertext: toBase64Url(new Uint8Array(cipherBuf)),
        iv: toBase64Url(iv)
      };
    } catch (err) {
      throw new Error("Encryption failed.");
    }
  }

  async function decryptToText(keyB64url, cipherB64url, ivB64url) {
    try {
      const subtle = ensureSubtle();
      const keyRaw = fromBase64Url(keyB64url);
      const key = await importKeyRaw(keyRaw);
      const iv = fromBase64Url(ivB64url);
      const cipher = fromBase64Url(cipherB64url);
      const plainBuf = await subtle.decrypt({ name: "AES-GCM", iv, tagLength: 128 }, key, cipher.buffer);
      return new TextDecoder().decode(plainBuf);
    } catch (err) {
      throw new Error("Decryption failed: bad key or corrupted data");
    }
  }

  // Expose API
  window.FIDELIUS_CRYPTO = {
    generateKeyRaw,
    importKeyRaw,
    encryptText,
    decryptToText
  };
})();