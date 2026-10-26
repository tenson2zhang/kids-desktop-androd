#!/usr/bin/env python3
"""Check that portable memory copies and session records stay consistent."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
START = "<!-- memory:standing:start -->"
END = "<!-- memory:standing:end -->"
COPIES = (
    "memory/MEMORY.md",
    "AGENTS.md",
    ".cursor/rules/portable-memory.mdc",
    ".trae/rules/portable-memory.md",
)
REQUIRED_KEYS = ("date", "title", "session")
FORBIDDEN = ("CODEBUDDY.md", "QWEN.md", ".goosehints")


def fail(message: str) -> None:
    print(f"FAIL {message}")
    sys.exit(1)


def extract_standing(path: Path) -> str:
    text = path.read_text(encoding="utf-8")
    if text.count(START) != 1 or text.count(END) != 1:
        fail(f"{path.relative_to(ROOT)} must contain exactly one standing block")
    inner = text.split(START, 1)[1].split(END, 1)[0]
    if not inner.startswith("\n") or not inner.endswith("\n"):
        fail(f"{path.relative_to(ROOT)} standing markers must sit on their own lines")
    return inner.strip("\n")


def parse_frontmatter(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        fail(f"{path.relative_to(ROOT)} is missing YAML frontmatter")
    end = text.find("\n---\n", 3)
    if end == -1:
        fail(f"{path.relative_to(ROOT)} frontmatter is not closed")
    fields: dict[str, str] = {}
    for line in text[4:end].splitlines():
        if not line.strip() or line.startswith("  - "):
            continue
        if line.startswith(" ") or ":" not in line:
            fail(f"{path.relative_to(ROOT)} has an unreadable frontmatter line: {line}")
        key, value = line.split(":", 1)
        fields[key.strip()] = value.strip()
    if not fields:
        fail(f"{path.relative_to(ROOT)} frontmatter is empty")
    return fields


def main() -> None:
    missing = [name for name in COPIES if not (ROOT / name).is_file()]
    if missing:
        fail("missing standing copy: " + ", ".join(missing))

    canonical = extract_standing(ROOT / "memory/MEMORY.md")
    if not canonical.startswith("## 长期记忆"):
        fail("canonical standing block lost its heading")
    for name in COPIES[1:]:
        other = extract_standing(ROOT / name)
        if other != canonical:
            fail(f"{name} standing block differs from memory/MEMORY.md")

    for name in ("CLAUDE.md", "GEMINI.md"):
        text = (ROOT / name).read_text(encoding="utf-8")
        if "@AGENTS.md" not in text:
            fail(f"{name} must import @AGENTS.md")

    aider = (ROOT / ".aider.conf.yml").read_text(encoding="utf-8")
    if "AGENTS.md" not in aider:
        fail(".aider.conf.yml must read AGENTS.md")

    for name in FORBIDDEN:
        if (ROOT / name).exists():
            fail(f"{name} would duplicate or replace AGENTS.md; do not add it")

    sessions = ROOT / "memory/sessions"
    index = (sessions / "INDEX.md").read_text(encoding="utf-8")
    for link in re.findall(r"\]\(([^)]+)\)", index):
        if "://" in link:
            continue
        if not (sessions / link).is_file():
            fail(f"INDEX.md links to a missing file: {link}")
    records = sorted(
        path
        for path in sessions.rglob("*.md")
        if path.name not in {"README.md", "INDEX.md", "_template.md"}
    )
    if not records:
        fail("no session records found")

    for path in records:
        rel = path.relative_to(sessions).as_posix()
        fields = parse_frontmatter(path)
        for key in REQUIRED_KEYS:
            if key not in fields or not fields[key]:
                fail(f"{rel} is missing frontmatter {key}")
        body = path.read_text(encoding="utf-8")
        if "### 用户" not in body or "### 代理" not in body:
            fail(f"{rel} must record raw user and agent turns")
        if not re.search(r"^## \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} UTC$", body, re.M):
            fail(f"{rel} must use millisecond timestamps")
        if re.search(r"^## \d+\s*$", body, re.M):
            fail(f"{rel} must not use sequence numbers")
        date = fields["date"]
        parts = rel.split("/")
        if len(parts) != 3 or parts[0:2] != date.split("-")[:2]:
            fail(f"{rel} is not stored under YYYY/MM matching {date}")
        if not path.name.startswith(date + "-"):
            fail(f"{rel} filename must start with {date}-")
        if rel not in index:
            fail(f"INDEX.md does not link {rel}")

    print(f"OK standing copies={len(COPIES)} sessions={len(records)}")


if __name__ == "__main__":
    main()
