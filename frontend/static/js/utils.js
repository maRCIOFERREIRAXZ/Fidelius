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

/* DOM helper */
export const $ = (s) => document.querySelector(s);

/* Safe integer parse from possibly-non-numeric values */
export function parseIntSafe(v) {
  if (v == null) return null;
  const n = Number(String(v).replace(/[^\d]/g, ""));
  return Number.isFinite(n) ? n : null;
}

/* Read server-configured max chars (memoized) */
export const getServerMaxChars = (() => {
  let cached = null;
  return () => {
    if (cached !== null) return cached;
    const g = parseIntSafe(window.FIDELIUS_SECRET_MAX_CHARS);
    if (g && g > 0) { cached = g; return cached; }
    const meta = document.querySelector('meta[name="fidelius-secret-max-chars"]');
    if (meta) {
      const m = parseIntSafe(meta.getAttribute("content"));
      if (m && m > 0) { cached = m; return cached; }
    }
    cached = 5000;
    return cached;
  };
})();

/* Human-readable byte size */
export function formatBytes(n) {
  if (n >= 1024 * 1024) return (n / (1024 * 1024)).toFixed(2) + " MB";
  if (n >= 1024) return Math.ceil(n / 1024) + " KB";
  return n + " B";
}

/* Toast UI helper (uses #toast element if present) */
export function showToast(msg, ms = 3000) {
  const t = $("#toast");
  if (!t) return;
  t.textContent = msg;
  t.classList.add("show");
  clearTimeout(t._h);
  t._h = setTimeout(() => t.classList.remove("show"), ms);
}

/* Small alias to keep showToast file-local usage simple */
const _qs = (s) => document.querySelector(s);

/* Create a non-invasive banner for warnings (idempotent) */
export function createBanner(text) {
  const existing = document.querySelector(".banner-warning");
  if (existing) return existing;
  const banner = document.createElement("div");
  banner.className = "banner-warning";
  banner.textContent = text;
  return banner;
}

/* Enable/disable primary actions (accepts cached elements to avoid lookups) */
export function disableActions({ createBtn = $("#create"), viewBtn = $("#viewBtn") } = {}) {
  if (createBtn) createBtn.disabled = true;
  if (viewBtn) viewBtn.disabled = true;
}
export function enableActions({ createBtn = $("#create"), viewBtn = $("#viewBtn") } = {}) {
  if (createBtn) createBtn.disabled = false;
  if (viewBtn) viewBtn.disabled = false;
}

/* Secret validation (normalizes and checks length/byte-size) */
export function validateSecret(plaintext, maxChars, maxBytes) {
  try { plaintext = plaintext.normalize("NFC"); } catch (_) {}
  if (!plaintext || plaintext.length === 0) return { ok: false, reason: "Enter a secret" };
  if (plaintext.length > maxChars) return { ok: false, reason: `Maximum ${maxChars.toLocaleString()} characters allowed.` };
  const bytes = new TextEncoder().encode(plaintext).length;
  if (bytes > maxBytes) return { ok: false, reason: `Secret too large: max ~${formatBytes(maxBytes)}.` };
  return { ok: true };
}

/* Theme helpers */
export function setTheme(t) {
  document.documentElement.setAttribute("data-theme", t);
  try { localStorage.setItem("fidelius-theme", t); } catch (_) {}
  const btn = $("#themeToggleBtn");
  if (btn) btn.setAttribute("aria-pressed", t === "dark");
}
export function currentTheme() { return document.documentElement.getAttribute("data-theme") || "light"; }

/* Re-export querySelector for small internal usages in this module and others */
export const $$_internal = _qs;
