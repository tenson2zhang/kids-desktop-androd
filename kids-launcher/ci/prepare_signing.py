#!/usr/bin/env python3
"""Create a new release keystore, or reuse the one saved on the apk release."""

from __future__ import annotations

import argparse
import secrets
import shutil
import subprocess
import sys
from pathlib import Path

ALIAS = "kidsdesktop"
DNAME = "CN=Kids Desktop, OU=Kids Desktop, O=Kids Desktop, L=Shanghai, ST=Shanghai, C=CN"
JKS_NAME = "kids-desktop.jks"
PROPS_NAME = "keystore.properties"


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


def write_properties(path: Path, store_password: str, alias: str = ALIAS) -> None:
    path.write_text(
        "storeFile=keystore/kids-desktop.jks\n"
        f"storePassword={escape_property(store_password)}\n"
        f"keyAlias={escape_property(alias)}\n"
        f"keyPassword={escape_property(store_password)}\n",
        encoding="utf-8",
    )
    path.chmod(0o600)


def read_password(properties: Path) -> str:
    values: dict[str, str] = {}
    for line in properties.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    password = values.get("storePassword", "")
    if not password:
        raise ValueError("missing storePassword")
    return password


def keytool_accepts(keystore: Path, password: str) -> bool:
    password_file = keystore.with_suffix(".pass")
    password_file.write_text(password, encoding="utf-8")
    password_file.chmod(0o600)
    try:
        result = subprocess.run(
            [
                "keytool",
                "-list",
                "-keystore",
                str(keystore),
                "-storepass:file",
                str(password_file),
                "-alias",
                ALIAS,
            ],
            capture_output=True,
            text=True,
        )
        return result.returncode == 0
    finally:
        password_file.unlink(missing_ok=True)


def generate(keystore: Path, properties: Path) -> None:
    password = secrets.token_urlsafe(24)
    keystore.parent.mkdir(parents=True, exist_ok=True)
    password_file = keystore.with_suffix(".pass")
    password_file.write_text(password, encoding="utf-8")
    password_file.chmod(0o600)
    try:
        subprocess.run(
            [
                "keytool",
                "-genkeypair",
                "-keystore",
                str(keystore),
                "-storetype",
                "PKCS12",
                "-alias",
                ALIAS,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "10000",
                "-storepass:file",
                str(password_file),
                "-keypass:file",
                str(password_file),
                "-dname",
                DNAME,
            ],
            check=True,
            capture_output=True,
            text=True,
        )
    finally:
        password_file.unlink(missing_ok=True)
    keystore.chmod(0o600)
    write_properties(properties, password)
    print("已生成新签名。")


def reuse(source: Path, keystore: Path, properties: Path) -> None:
    src_jks = source / JKS_NAME
    src_props = source / PROPS_NAME
    password = read_password(src_props)
    if not keytool_accepts(src_jks, password):
        raise ValueError("saved keystore cannot be opened")
    keystore.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src_jks, keystore)
    shutil.copyfile(src_props, properties)
    keystore.chmod(0o600)
    properties.chmod(0o600)
    print("已沿用 Release 里的签名。")


def prepare(source: Path, keystore: Path, properties: Path) -> None:
    src_jks = source / JKS_NAME
    src_props = source / PROPS_NAME
    if src_jks.is_file() and src_props.is_file():
        try:
            reuse(source, keystore, properties)
            return
        except (ValueError, OSError):
            print("Release 里的签名打不开，改为生成新签名。", file=sys.stderr)
    generate(keystore, properties)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--properties", type=Path, required=True)
    args = parser.parse_args()
    try:
        prepare(args.source, args.keystore, args.properties)
    except subprocess.CalledProcessError as exc:
        message = (exc.stderr or exc.stdout or "").strip()
        print(f"生成签名失败：{message}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
