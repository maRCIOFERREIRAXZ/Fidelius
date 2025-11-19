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


import { createSecret, fetchAndDecrypt } from './api.js';
import {
  $, parseIntSafe, getServerMaxChars, formatBytes, showToast,
  createBanner, disableActions, enableActions, validateSecret,
  setTheme, currentTheme
} from './utils.js';

"use strict";

async function init() {
  const SECRET_MAX_CHARS = getServerMaxChars();
  const SECRET_MAX_BYTES = SECRET_MAX_CHARS * 4;

  // Cache DOM nodes
  const card = document.querySelector(".card") || document.querySelector(".page-wrap");
  const sizeHint = $("#sizeHint");
  const createBtn = $("#create");
  const viewBtn = $("#viewBtn");
  const themeBtn = $("#themeToggleBtn");
  const editorArea = $("#editorArea");
  const resultArea = $("#resultArea");
  const secretTextarea = $("#secret");
  const toast = $("#toast");

  const heroTitleCreate = $("#heroTitleCreate");
  const heroDescCreate  = $("#heroDescCreate");

  // Theme init
  const saved = (() => { try { return localStorage.getItem("fidelius-theme"); } catch (_) { return null; } })();
  if (saved) setTheme(saved);
  else setTheme((window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) ? "dark" : "light");

  if (themeBtn) {
    themeBtn.addEventListener("click", () => {
      const next = currentTheme() === "dark" ? "light" : "dark";
      setTheme(next);
      const icon = themeBtn.querySelector(".theme-icon");
      if (icon) {
        icon.classList.add("theme-icon-animate");
        setTimeout(() => icon.classList.remove("theme-icon-animate"), 160);
      }
    });
  }

  if (sizeHint) {
    sizeHint.textContent =
      `Max size ~${formatBytes(SECRET_MAX_BYTES)} (${SECRET_MAX_CHARS.toLocaleString()} chars).`;
  }

  function adaptCreate() {
    if (!createBtn) return;
    createBtn.textContent =
      window.innerWidth <= 420 ? "Create" : "Create secret";
  }
  adaptCreate();
  window.addEventListener("resize", adaptCreate);

  const cryptoAvailable = typeof window.crypto !== "undefined" && !!window.crypto.subtle;
  const secureContext = !!window.isSecureContext;

  if (!cryptoAvailable || !secureContext) {
    let reason = !cryptoAvailable
      ? "Encryption (Web Crypto API) is not available in this browser."
      : "Page not secure — encryption requires HTTPS or localhost.";

    if (card) {
      const banner = createBanner(
        reason +
        " To test on a phone use HTTPS (mkcert, ngrok) or open on localhost."
      );
      card.parentNode && card.parentNode.insertBefore(banner, card);
    }

    if (sizeHint) sizeHint.textContent = "Encryption not available on this connection.";
    disableActions({ createBtn, viewBtn });
    showToast(reason + " Create/view disabled.", 8000);
    return;
  }

  enableActions({ createBtn, viewBtn });

  /* -------------------------
     CREATE flow
  ------------------------- */
  if (createBtn && editorArea && resultArea && secretTextarea) {
    createBtn.addEventListener("click", async () => {
      const txt = secretTextarea.value || "";
      const v = validateSecret(txt, SECRET_MAX_CHARS, SECRET_MAX_BYTES);
      if (!v.ok) { showToast(v.reason); return; }

      createBtn.disabled = true;
      const original = createBtn.textContent;
      createBtn.textContent = "Encrypting…";

      try {
        const { id, key, link } = await createSecret(txt);

        // Hide editor, show result
        editorArea.classList.add("hidden");
        resultArea.classList.remove("hidden");

        // Change title + description when created
        if (heroTitleCreate) {
          heroTitleCreate.classList.remove("title-animate");
          void heroTitleCreate.offsetWidth;
          heroTitleCreate.textContent = "Secret Created!";
          heroTitleCreate.classList.add("title-animate");
        }
        if (heroDescCreate) {
          heroDescCreate.classList.add("hidden");
        }

        // Show link
        const shareLink = $("#shareLink");
        if (shareLink) {
          shareLink.innerHTML = "";
          const pre = document.createElement("pre");
          pre.className = "share-link mono-pre";
          pre.textContent = link;
          shareLink.appendChild(pre);
        }

        // Copy button
        const copyBtn = $("#copyLink");
        if (copyBtn) {
          const c2 = copyBtn.cloneNode(true);
          copyBtn.parentNode.replaceChild(c2, copyBtn);
          c2.addEventListener("click", async () => {
            try {
              await navigator.clipboard.writeText(link);
              showToast("Link copied");
            } catch (_) {
              showToast("Copy failed");
            }
          });
        }

        // New secret button
        const newBtn = $("#newSecret");
        if (newBtn) {
          const n2 = newBtn.cloneNode(true);
          newBtn.parentNode.replaceChild(n2, newBtn);

          n2.addEventListener("click", () => {
            // Reset fields
            secretTextarea.value = "";
            resultArea.classList.add("hidden");
            editorArea.classList.remove("hidden");

            if (createBtn) createBtn.disabled = false;
            adaptCreate();
            secretTextarea.focus();

            // Change title + description back
            if (heroTitleCreate) {
              heroTitleCreate.classList.remove("title-animate");
              void heroTitleCreate.offsetWidth;
              heroTitleCreate.textContent = "Create a new secret";
            }
            if (heroDescCreate) {
              heroDescCreate.classList.remove("hidden");
            }
          });
        }

        showToast("Secret encrypted");

      } catch (err) {
        showToast(err && err.message ? err.message : "Encryption failed");

      } finally {
        if (createBtn) {
          createBtn.disabled = false;
          adaptCreate();
          createBtn.textContent = original;
        }
      }
    });
  }

  /* -------------------------
     VIEW flow
  ------------------------- */
  if (viewBtn) {
    const id = window.location.pathname.split("/").pop();
    const secretIdRow = $("#secretIdRow");
    const controls = $("#controls");

    if (secretIdRow) secretIdRow.textContent = "Secret ID: " + id;
    if (controls) controls.classList.remove("hidden");

    viewBtn.addEventListener("click", async () => {
      window.onbeforeunload = (e) => { e.preventDefault(); e.returnValue = ""; };

      const key = location.hash ? location.hash.substring(1) : null;
      if (!key) { showToast("Missing decryption key in URL fragment."); return; }

      viewBtn.disabled = true;
      const orig = viewBtn.textContent;
      viewBtn.textContent = "Fetching…";

      try {
        const plain = await fetchAndDecrypt(id, key);

        const secretBox = $("#secretBox");
        const secretText = $("#secretText");
        if (secretText) secretText.textContent = plain;
        if (secretBox) secretBox.classList.remove("hidden");
        if (controls) controls.classList.add("hidden");
        if (secretIdRow) secretIdRow.textContent = "Secret loaded.";

        const copyBtn = $("#copyBtn");
        if (copyBtn) {
          const c2 = copyBtn.cloneNode(true);
          copyBtn.parentNode.replaceChild(c2, copyBtn);
          c2.addEventListener("click", async () => {
            try { await navigator.clipboard.writeText(plain); showToast("Copied"); }
            catch (_) { showToast("Copy failed"); }
          });
        }

        const closeBtn = $("#closeSecret");
        if (closeBtn) {
          const c2 = closeBtn.cloneNode(true);
          closeBtn.parentNode.replaceChild(c2, closeBtn);
          c2.addEventListener("click", () => {
            if (secretText) secretText.textContent = "";
            if (secretBox) secretBox.classList.add("hidden");
            if (secretIdRow) secretIdRow.textContent = "Closed — the secret has vanished from the server forever.";
            const rn = $("#refreshNotice");
            if (rn) rn.classList.add("hidden");
            try { history.replaceState(null, "", location.origin + "/"); } catch (e) {}
            window.onbeforeunload = null;
          });
        }
      } catch (err) {
        if (err && err.code === 404) {
          const secretIdRow = $("#secretIdRow");
          if (secretIdRow) {
            secretIdRow.textContent =
              "The secret is gone — already uncovered or expired.";
          }
          viewBtn.disabled = true;
          viewBtn.title = "This secret has already been viewed or has expired.";
          return;
        }

        const secretIdRowEl = $("#secretIdRow");
        if (secretIdRowEl) secretIdRowEl.textContent = "Decryption failed — wrong key?";
        showToast(err && err.message ? err.message : "Decryption failed");
        viewBtn.disabled = true;

      } finally {
        if (!($("#secretIdRow").textContent.toLowerCase().includes("loaded"))) {
          viewBtn.textContent = orig;
        }
      }
    });
  }

  if (toast) toast.addEventListener("click", () => toast.classList.remove("show"));

  window.addEventListener("pagehide", () => { window.onbeforeunload = null; });
}

if (document.readyState === "loading")
  document.addEventListener("DOMContentLoaded", init);
else
  init();