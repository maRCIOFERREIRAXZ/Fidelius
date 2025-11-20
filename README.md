<table>
  <tr>
    <td width="150">
      <img src="https://github.com/user-attachments/assets/a8ae2799-1d32-448d-be78-613b00e77767" width="120" />
    </td>
    <td>
      <h1>Fidelius: Zero-Knowledge Secret Sharing</h1>
    </td>
  </tr>
</table>


Fidelius is a minimal, privacy-focused, one-time secret-sharing app.
Its name is inspired by the Fidelius Charm from *Harry Potter*, a spell so powerful that a secret becomes invisible to
all
but the intended recipient.

In the same spirit, only the chosen recipient can reveal the secret.
The server stores nothing but encrypted ciphertext, and the decryption key is never sent or saved on the server. Only
the bearer of the link can unlock the message.

#### Purpose & Use Cases

There are many moments when we need to share sensitive information, like passwords, card details, or other private data
with friends or family. Traditional messaging apps don’t feel safe for this purpose; it’s impossible to know how many
servers a message might pass through or where copies might linger. Fidelius was created to solve this problem by giving
you a simple, secure way to share secrets without leaving a trace.

#### Demo: https://secrets.krsh.dev
---

## Highlights

- All encryption happens locally in the browser using AES-256-GCM via the Web Crypto API.
- The server never sees the plaintext secret or the encryption key.
- The decryption key is stored in the URL fragment and is never sent to the server.
- Secrets can be retrieved only once and are atomically deleted from the server during retrieval.
- Enforces strict CSP policies to protect against XSS and clickjacking attacks.
- No accounts, no user data, and no complicated setup required.

---

## How it works

#### Creating a secret

1. The user enters plaintext into the browser UI.
2. The browser generates a random 256-bit `AES-GCM` key.
3. The browser generates a random 12-byte `AES-GCM` nonce (`IV`).
4. The browser encrypts the plaintext using `AES-GCM` with the key + nonce → ciphertext.
5. The browser sends only {`ciphertext`, `nonce`} to the server.
6. The server stores the ciphertext, nonce, and metadata under a generated secret ID.
7. The browser creates a share link: `https://host/s/<id>#<key>`
8. The browser shows this link to the user, the key remains only in the URL fragment.

#### Viewing a secret

1. The recipient opens the link: `https://host/s/<id>#<key>`
2. The browser loads the `/s/<id>` page without sending `#<key>` to the server.
3. The browser extracts the decryption key from the URL fragment.
4. The browser requests `GET /api/v1/secrets/<id>` from the server.
5. The server returns {`ciphertext`, `nonce`} and atomically deletes the stored secret.
6. The browser decrypts the ciphertext using `AES-GCM` with the `key` + `nonce`.
7. The browser displays the plaintext only in memory; it is never saved or re-sent.
8. Refreshing the page loses the key, and the server copy is already deleted.

---

## Self Hosting

The easiest way to host Fidelius is via Docker Compose. Simply clone or download this repository, then run:

```shell
cd Fidelius
docker compose up -d --build
```

> Note that subsequent `docker compose up -d` commands do not require `--build`.
> Make any changes to the configuration variables in `docker-compose.yml` as needed before starting.

---

## Environment Variables

| Variable                   | Default | Description                                                                                        |
|----------------------------|---------|----------------------------------------------------------------------------------------------------|
| `KEEP_MAX_DAYS`            | `30`    | How many days to keep secrets. useful to prevent the server from filling with never-opened secrets |
| `SECRET_MAX_CHARS`         | `5000`  | Maximum allowed secret text size                                                                   |
| `RATE_LIMIT_PER_MINUTE`    | `10`    | Per-IP rate limit per minute                                                                       |
| `CLEANUP_INTERVAL_MINUTES` | `60`    | How often the background cleanup worker should run to delete expired secrets                       |

---

## License

[MIT License][license] © [Stɑrry Shivɑm][github]

[license]: /LICENSE

[github]: https://github.com/starry-shivam

```
MIT License

Copyright (c) [2025 - Present] Stɑrry Shivɑm

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

