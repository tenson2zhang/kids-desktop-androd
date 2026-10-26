#!/usr/bin/env python3
"""Checks for creating and reusing a release keystore. No old secrets."""

from __future__ import annotations

import hashlib
import shutil
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import prepare_signing as signing


def check(name: str, cond: bool) -> None:
    if not cond:
        raise SystemExit(f"FAIL {name}")
    print(f"ok {name}")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    if shutil.which("keytool") is None:
        raise SystemExit("keytool is required")

    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        keystore = root / "out" / signing.JKS_NAME
        properties = root / "out" / signing.PROPS_NAME
        signing.prepare(root / "empty", keystore, properties)
        check("created", keystore.is_file() and properties.is_file())
        password = signing.read_password(properties)
        check("opens", signing.keytool_accepts(keystore, password))
        check("password-stored", password in properties.read_text(encoding="utf-8"))

        saved = root / "saved"
        saved.mkdir()
        shutil.copyfile(keystore, saved / signing.JKS_NAME)
        shutil.copyfile(properties, saved / signing.PROPS_NAME)
        first = sha256(keystore)
        keystore.unlink()
        properties.unlink()
        signing.prepare(saved, keystore, properties)
        check("reused", sha256(keystore) == first)

        broken = root / "broken"
        broken.mkdir()
        (broken / signing.JKS_NAME).write_bytes(b"not a keystore")
        signing.write_properties(broken / signing.PROPS_NAME, "wrong-password")
        fresh = root / "fresh" / signing.JKS_NAME
        fresh_props = root / "fresh" / signing.PROPS_NAME
        signing.prepare(broken, fresh, fresh_props)
        check("replaces-broken", signing.keytool_accepts(fresh, signing.read_password(fresh_props)))
        check("new-after-broken", sha256(fresh) != first)

    print("ALL PASSED")


if __name__ == "__main__":
    main()
