#!/usr/bin/env python3
"""Checks for the keystore Base64 parser. No secrets."""

from __future__ import annotations

import base64
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import materialize_keystore as ks


def sample() -> bytes:
    body = bytes([0x0A, 0xDE, 0xFB, 0x3F]) + os.urandom(64)
    return b"\x30\x82" + len(body).to_bytes(2, "big") + body


def check(name: str, cond: bool) -> None:
    if not cond:
        raise SystemExit(f"FAIL {name}")
    print(f"ok {name}")


def main() -> None:
    payload = sample()
    encoded = base64.b64encode(payload).decode("ascii")
    check("clean", ks.decode_keystore(encoded) == payload)

    wrapped = "\n".join(encoded[i : i + 76] for i in range(0, len(encoded), 76))
    check("wrapped", ks.decode_keystore(wrapped) == payload)

    messy = "\ufeff```\n" + encoded[:8] + "\u200b" + encoded[8:] + "\n```"
    check("junk", ks.decode_keystore(messy) == payload)

    check("padding", ks.decode_keystore(encoded.rstrip("=")) == payload)

    labeled = "ANDROID_KEYSTORE_BASE64\n" + wrapped
    check("label", ks.decode_keystore(labeled) == payload)

    urlsafe = base64.urlsafe_b64encode(payload).decode("ascii")
    check("urlsafe", ks.decode_keystore(urlsafe) == payload)

    hyphenated = encoded[:20] + "-" + encoded[20:]
    check("stray-hyphen", ks.decode_keystore(hyphenated) == payload)

    try:
        ks.decode_keystore("not-a-keystore-value")
    except ValueError:
        check("reject-short", True)
    else:
        check("reject-short", False)

    dirty = encoded[:40] + "!\n" + encoded[40:]
    check("ignore-junk-char", ks.decode_keystore(dirty) == payload)

    half = encoded[: len(encoded) // 2]
    try:
        ks.decode_keystore(half)
    except ValueError as exc:
        check("reject-truncated", "truncated" in str(exc) or "decode-failed" in str(exc))
    else:
        check("reject-truncated", False)

    real = Path(__file__).resolve().parents[1] / "keystore" / "kids-desktop.jks"
    if real.is_file():
        raw = real.read_bytes()
        real_b64 = base64.b64encode(raw).decode("ascii")
        check("local-jks", ks.decode_keystore("请粘贴\n" + real_b64) == raw)
        try:
            ks.decode_keystore(real_b64[: len(real_b64) // 2])
        except ValueError as exc:
            check("reject-real-truncated", "truncated" in str(exc))
        else:
            check("reject-real-truncated", False)

    print("ALL PASSED")


if __name__ == "__main__":
    main()
