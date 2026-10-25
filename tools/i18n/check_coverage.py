#!/usr/bin/env python3
"""Translation coverage report + sanity checks for Dolphy locales.

Run from the repository root:
    python tools/i18n/check_coverage.py            # report
    python tools/i18n/check_coverage.py --strict   # exit 1 on broken XML/parity

Checks
------
1. Every `res/values-*/strings.xml` parses as XML.
2. No duplicate keys inside a file.
3. Coverage: how many of the English keys each locale provides.
4. Format arguments (`%1$s`, `%d`…) match the English source — a mismatch is a
   crash waiting to happen (`IllegalFormatConversionException`).
5. Keys declared in LanguageCatalog.kt have a matching `values-<tag>` folder.
6. Keys never referenced from Kotlin/XML (dead strings, safe to delete).
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
SRC = ROOT / "app/src/main/java"
CATALOG = SRC / "com/droid/dolphy/LanguageCatalog.kt"

FORMAT_RE = re.compile(r"%(?:\d+\$)?[a-zA-Z]")


def parse(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    out: dict[str, str] = {}
    dupes: list[str] = []
    for node in tree.getroot():
        if node.tag != "string":
            continue
        key = node.get("name", "")
        if key in out:
            dupes.append(key)
        out[key] = node.text or ""
    if dupes:
        print(f"  ! {path.parent.name}: duplicate keys {dupes}")
    return out


def main(argv: list[str]) -> int:
    strict = "--strict" in argv

    base_path = RES / "values/strings.xml"
    if not base_path.exists():
        print("missing res/values/strings.xml", file=sys.stderr)
        return 1
    base = parse(base_path)

    # Keys referenced anywhere in the code.
    blob = []
    for p in list(SRC.rglob("*.kt")) + list(RES.rglob("*.xml")):
        try:
            blob.append(p.read_text(encoding="utf-8", errors="ignore"))
        except OSError:
            pass
    haystack = "\n".join(blob)

    unused = sorted(k for k in base if f"R.string.{k}" not in haystack and f'@string/{k}"' not in haystack)

    print(f"English (default): {len(base)} keys\n")
    print(f"{'locale':10} {'keys':>6} {'cover':>7}  {'bad args':>8}")
    print("-" * 40)

    locales = sorted(p for p in RES.glob("values-*") if (p / "strings.xml").exists())
    problems = 0
    for folder in locales:
        tag = folder.name.removeprefix("values-")
        try:
            data = parse(folder / "strings.xml")
        except ET.ParseError as exc:
            print(f"{tag:10} {'—':>6} {'—':>7}  XML ERROR: {exc}")
            problems += 1
            continue

        unknown = [k for k in data if k not in base]
        if unknown:
            print(f"  ! {tag}: keys not in English file: {unknown[:5]}")
            problems += 1

        bad_args = []
        for key, value in data.items():
            if key not in base:
                continue
            if sorted(FORMAT_RE.findall(base[key])) != sorted(FORMAT_RE.findall(value)):
                bad_args.append(key)
        if bad_args:
            print(f"  ! {tag}: format-argument mismatch on {bad_args[:5]}")
            problems += len(bad_args)

        coverage = 100.0 * len(data) / max(1, len(base))
        print(f"{tag:10} {len(data):>6} {coverage:>6.1f}%  {len(bad_args):>8}")

    # Languages advertised in Kotlin but with no resources.
    if CATALOG.exists():
        tags = re.findall(r'AppLanguage\("([a-z]{2}(?:-[A-Za-z]+)?)"', CATALOG.read_text(encoding="utf-8"))
        missing_dirs = [t for t in tags if t != "en" and not (RES / f"values-{t}").exists()]
        if missing_dirs:
            print(f"\n! in LanguageCatalog.kt but no resources: {missing_dirs}")
            problems += len(missing_dirs)
        print(f"\nlanguages in catalog: {len(tags)}")

    if unused:
        print(f"\nunused English keys ({len(unused)}): {', '.join(unused[:15])}"
              f"{'…' if len(unused) > 15 else ''}")

    try:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        from core_translations import CORE_KEYS
        core_done = 0
        for folder in locales:
            data = parse(folder / "strings.xml")
            if all(k in data for k in CORE_KEYS):
                core_done += 1
        print(f"\ncore UI strings ({len(CORE_KEYS)} keys): complete in {core_done}/{len(locales)} locales")
    except Exception:
        pass

    print(f"\nproblems: {problems}")
    return 1 if (strict and problems) else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
