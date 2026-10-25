#!/usr/bin/env python3
"""Fail the build before aapt2 does.

aapt2 rejects a bare apostrophe inside a string resource with the famously
unhelpful message:

    Resource compilation failed (... values-it.xml.
    Cause: java.lang.IllegalStateException: Can not extract resource from
    com.android.aaptcompiler.ParsedResource@5d1dbbc8.)

Finding the offending string by hand across sixteen locales is miserable, so this
check reports file, key and line before anything is compiled.

An apostrophe must be written \\' (or the whole string wrapped in "...").

Usage:
    python tools/i18n/check_escaping.py [--strict]

--strict exits 1 on any finding; without it the script only prints.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

RES = Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "res"

STRING_RE = re.compile(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', re.S)
BARE_APOSTROPHE = re.compile(r"(?<!\\)'")


def scan(path: Path) -> list[tuple[str, int]]:
    text = path.read_text(encoding="utf-8")
    findings: list[tuple[str, int]] = []
    for match in STRING_RE.finditer(text):
        body = match.group(2)
        if not BARE_APOSTROPHE.search(body):
            continue
        line = text.count("\n", 0, match.start()) + 1
        findings.append((match.group(1), line))
    return findings


def main() -> int:
    strict = "--strict" in sys.argv
    files = sorted(RES.glob("values*/strings.xml"))
    total = 0
    for path in files:
        for key, line in scan(path):
            total += 1
            print(f"{path.relative_to(RES.parents[1])}:{line}: {key}: unescaped ' (use \\')")
    if total:
        print(f"\n{total} string(s) need escaping — aapt2 will refuse to build.")
        return 1 if strict else 0
    print(f"No unescaped apostrophes in {len(files)} string files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
