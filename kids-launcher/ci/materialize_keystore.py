#!/usr/bin/env python3
"""Write the release keystore from GitHub Actions secrets."""

from __future__ import annotations

import base64
import os
import re
import sys
from pathlib import Path

_INVISIBLE = re.compile(r"[\u200b\u200c\u200d\ufeff]")
_RUN = re.compile(r"[A-Za-z0-9+/=_-]+")


def declared_der_length(data: bytes) -> int | None:
    if len(data) < 2 or data[0] != 0x30:
        return None
    marker = data[1]
    if marker < 0x80:
        return 2 + marker
    count = marker - 0x80
    if count == 0 or len(data) < 2 + count:
        return None
    return 2 + count + int.from_bytes(data[2 : 2 + count], "big")


def looks_like_keystore(data: bytes) -> bool:
    if len(data) < 16:
        return False
    if data[:4] == b"\xfe\xed\xfe\xed":
        return True
    expected = declared_der_length(data)
    return expected is not None and expected == len(data)


def _decode_chunk(chunk: str) -> bytes | None:
    if ("-" in chunk or "_" in chunk) and "+" not in chunk and "/" not in chunk:
        body = chunk
        urlsafe = True
    else:
        body = chunk.replace("-", "").replace("_", "")
        urlsafe = False
    body = body.rstrip("=")
    if len(body) < 16:
        return None
    body += "=" * ((-len(body)) % 4)
    try:
        if urlsafe:
            return base64.urlsafe_b64decode(body)
        return base64.b64decode(body, validate=True)
    except Exception:
        return None


def decode_keystore(raw: str) -> bytes:
    text = _INVISIBLE.sub("", raw).replace("`", "").replace('"', "").replace("'", "")
    marker = text.lower().find("base64,")
    if 0 <= marker < 80:
        text = text[marker + len("base64,") :]

    compact = "".join(text.split())
    candidates = [compact]
    for match in _RUN.finditer(text):
        chunk = match.group(0)
        if len(chunk) >= 64:
            candidates.append(chunk)
    for match in re.finditer("MII", compact):
        run = _RUN.match(compact, match.start())
        if run is not None and len(run.group(0)) >= 64:
            candidates.append(run.group(0))

    decoded: list[bytes] = []
    truncated: list[bytes] = []
    for chunk in candidates:
        data = _decode_chunk(chunk)
        if data is None:
            continue
        if looks_like_keystore(data):
            decoded.append(data)
        elif data[:1] == b"\x30":
            truncated.append(data)
    if decoded:
        return max(decoded, key=len)

    if truncated:
        data = max(truncated, key=len)
        expected = declared_der_length(data)
        print(
            "::error::ANDROID_KEYSTORE_BASE64 不完整。"
            f"解出 {len(data)} 字节，密钥库头声明总长 {expected} 字节。"
            f"Secret 原始长度 {len(raw)}。"
            "请整段重新粘贴 kids-desktop.jks 的 Base64，不要只复制屏幕上露出的一部分。",
            file=sys.stderr,
        )
        raise ValueError(f"keystore-truncated actual={len(data)} expected={expected}")

    print(
        "::error::ANDROID_KEYSTORE_BASE64 解出来不是密钥库。"
        f"原始长度 {len(raw)}，去掉空白后 {len(compact)}。"
        "请把 kids-desktop.jks 的 Base64 整段放进这个 Secret，口令放在 ANDROID_KEYSTORE_PASSWORD。",
        file=sys.stderr,
    )
    raise ValueError("keystore-decode-failed")


def escape_property(value: str) -> str:
    return (
        value.replace("\\", "\\\\")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("=", "\\=")
        .replace(":", "\\:")
        .replace("#", "\\#")
        .replace("!", "\\!")
    )


def main() -> int:
    raw = os.environ.get("ANDROID_KEYSTORE_BASE64", "")
    store_password = os.environ.get("ANDROID_KEYSTORE_PASSWORD", "")
    if not raw.strip() or not store_password:
        print(
            "::error::缺少签名密钥。请到仓库 Settings → Secrets and variables → Actions，"
            "添加 ANDROID_KEYSTORE_BASE64 和 ANDROID_KEYSTORE_PASSWORD，然后重新运行「构建安装包」。",
            file=sys.stderr,
        )
        return 1

    try:
        keystore_bytes = decode_keystore(raw)
    except ValueError:
        return 1

    keystore = Path("kids-launcher/keystore/kids-desktop.jks")
    keystore.parent.mkdir(parents=True, exist_ok=True)
    keystore.write_bytes(keystore_bytes)
    keystore.chmod(0o600)

    alias = os.environ.get("ANDROID_KEY_ALIAS") or "kidsdesktop"
    key_password = os.environ.get("ANDROID_KEY_PASSWORD") or store_password
    Path("kids-launcher/keystore.properties").write_text(
        "storeFile=keystore/kids-desktop.jks\n"
        f"storePassword={escape_property(store_password)}\n"
        f"keyAlias={escape_property(alias)}\n"
        f"keyPassword={escape_property(key_password)}\n",
        encoding="utf-8",
    )
    print(f"已写入签名库，{len(keystore_bytes)} 字节。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
