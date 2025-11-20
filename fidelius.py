#!/usr/bin/env python3
#
# A small CLI tool for the Fidelius server that lets you create secrets from the command line.
# It serves as an alternative to the web UI and uses the cryptography library to encrypt secrets
# with AES-GCM before sending them to the server.
#
# fidelius.py --server <url> [--text "your secret"] [--file path/to/file]
#
# Options:
#   --server, -s   The base URL of your Fidelius server (required)
#   --text, -t     Provide plaintext directly on the command line
#   --file, -f     Read plaintext from a file
#
# ----------------------------------------------------------------------------------------------

import argparse
import base64
import json
import os
import sys
import urllib.request
import urllib.error

# Colors
GREEN = "\033[92m"
CYAN = "\033[96m"
YELLOW = "\033[93m"
RESET = "\033[0m"

# check if Cryptography is installed, exit otherwise.
try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except ModuleNotFoundError:
    print(f"{YELLOW}[ERROR]{RESET} Missing dependency: {CYAN}cryptography{RESET}")
    print("Install it with:\n  pip install cryptography\n")
    sys.exit(1)

# ----------------------------------------

def gen_key() -> bytes:
    return os.urandom(32)

def key_fragment(key: bytes) -> str:
    return base64.urlsafe_b64encode(key).decode().rstrip("=")

def encrypt(key: bytes, data: bytes):
    aes = AESGCM(key)
    nonce = os.urandom(12)
    ct = aes.encrypt(nonce=nonce, data=data, associated_data=None)
    return (
        base64.b64encode(ct).decode(),
        base64.b64encode(nonce).decode(),
    )

def post_json(url: str, payload: dict):
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        method="POST"
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except Exception as e:
        print(f"{YELLOW}[ERROR]{RESET} Network error: {e}", file=sys.stderr)
        sys.exit(1)

def post_secret(server: str, ciphertext: str, nonce: str):
    url = server.rstrip("/") + "/api/v1/create"
    status, body = post_json(url, {"ciphertext": ciphertext, "nonce": nonce})

    if status != 200:
        print(f"{YELLOW}[ERROR]{RESET} Server responded with {status}: {body.decode()}", file=sys.stderr)
        sys.exit(1)

    try:
        j = json.loads(body.decode())
    except:
        print(f"{YELLOW}[ERROR]{RESET} Invalid JSON response from server.", file=sys.stderr)
        sys.exit(1)

    sid = j.get("id")
    if not sid:
        print(f"{YELLOW}[ERROR]{RESET} No 'id' returned by server.", file=sys.stderr)
        sys.exit(1)

    return sid

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--server", "-s", required=True, help="Server base URL, e.g. https://example.com")
    p.add_argument("--text", "-t", help="Plaintext")
    p.add_argument("--file", "-f", help="File containing plaintext")
    args = p.parse_args()

    if args.text and args.file:
        print(f"{YELLOW}[ERROR]{RESET} Use --text OR --file, not both.", file=sys.stderr)
        sys.exit(1)

    # load secret from file or text
    if args.file:
        try:
            with open(args.file, "rb") as f:
                data = f.read()
        except Exception as e:
            print(f"{YELLOW}[ERROR]{RESET} Failed to read file: {e}", file=sys.stderr)
            sys.exit(1)
    elif args.text:
        data = args.text.encode()
    else:
        # show error and exit if no input method is provided
        print(f"{YELLOW}[ERROR]{RESET} No input provided. Use --text or --file.", file=sys.stderr)
        sys.exit(1)

    # encrypt the secret
    print(f"{CYAN}Encrypting secret...{RESET}")
    key = gen_key()
    ct, nonce = encrypt(key, data)
    print(f"{CYAN}Posting secret to server...{RESET}")
    sid = post_secret(args.server, ct, nonce)

    # build result url
    url = f"{args.server.rstrip('/')}/s/{sid}#{key_fragment(key)}"

    # format output nicely
    print(f"{GREEN}Secret Created Successfully{RESET}\n")
    print(f"{CYAN}Server:{RESET}    {args.server}")
    print(f"{CYAN}Secret ID:{RESET} {sid}\n")
    print(f"{CYAN}View URL:{RESET}")
    print(f"{YELLOW}{url}{RESET}\n")

if __name__ == "__main__":
    main()