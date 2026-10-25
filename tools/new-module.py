#!/usr/bin/env python3
"""Scaffolds a new native module (screen + route + catalog entry + strings).

Usage (from the repository root):

    python tools/new-module.py --id spectrum_analyzer --title "Spectrum Analyzer" \
        --desc "Live 2.4 GHz band activity" --section NETWORK --package network

It writes the screen file, adds `<id>_title` / `<id>_desc` to the English
strings, and prints the two snippets you still have to paste (catalog entry and
nav route). The snippets are printed instead of patched because both live inside
large hand-edited blocks where an automated insert would be fragile.

After running it:
    python tools/i18n/add_locale.py && python tools/i18n/check_coverage.py
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/droid/dolphy"
STRINGS = ROOT / "app/src/main/res/values/strings.xml"

SECTIONS = {
    "INFRARED": "OtherSections.INFRARED",
    "BLUETOOTH": "OtherSections.BLUETOOTH",
    "NFC": "OtherSections.NFC",
    "NETWORK": "OtherSections.NETWORK",
    "MEDIA": "OtherSections.MEDIA",
    "TOOLS": "OtherSections.OTHER",
}

SCREEN_TEMPLATE = '''package com.droid.dolphy.{package}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droid.dolphy.R
import com.droid.dolphy.SectionTopBar

/**
 * {title} — scaffolded by tools/new-module.py.
 *
 * Route: other/{id}
 * Strings: {id}_title, {id}_desc
 */
@Composable
fun {name}Screen(navController: NavController) {{
    Column(modifier = Modifier.fillMaxSize()) {{
        SectionTopBar(
            title = stringResource(R.string.{id}_title),
            onBack = {{ navController.popBackStack() }},
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {{
            Text(
                text = stringResource(R.string.{id}_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // TODO: implement the module.
        }}
    }}
}}
'''


def pascal(value: str) -> str:
    return "".join(part.capitalize() for part in re.split(r"[^a-zA-Z0-9]+", value) if part)


def main() -> int:
    parser = argparse.ArgumentParser(description="Scaffold a Dolphy module")
    parser.add_argument("--id", required=True, help="snake_case id, e.g. spectrum_analyzer")
    parser.add_argument("--title", required=True, help="human title (English)")
    parser.add_argument("--desc", required=True, help="one-line description (English)")
    parser.add_argument("--section", default="TOOLS", choices=sorted(SECTIONS))
    parser.add_argument("--package", default="", help="sub-package under com.droid.dolphy")
    args = parser.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9_]*", args.id):
        print("--id must be snake_case (a-z, 0-9, _)", file=sys.stderr)
        return 1

    name = pascal(args.id)
    pkg_dir = JAVA / args.package if args.package else JAVA
    pkg_dir.mkdir(parents=True, exist_ok=True)
    target = pkg_dir / f"{name}Screen.kt"

    if target.exists():
        print(f"{target} already exists — pick another --id", file=sys.stderr)
        return 1

    target.write_text(
        SCREEN_TEMPLATE.format(
            package=("com.droid.dolphy." + args.package) if args.package else "com.droid.dolphy",
            title=args.title,
            name=name,
            id=args.id,
        ),
        encoding="utf-8",
    )

    # Add the two strings, keeping the file valid XML.
    text = STRINGS.read_text(encoding="utf-8")
    for key, value in ((f"{args.id}_title", args.title), (f"{args.id}_desc", args.desc)):
        if f'name="{key}"' in text:
            print(f"string '{key}' already exists, skipped")
            continue
        escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'")
        text = text.replace(
            "</resources>",
            f'    <string name="{key}">{escaped}</string>\n</resources>',
        )
    STRINGS.write_text(text, encoding="utf-8")

    import_path = f"com.droid.dolphy.{args.package}.{name}Screen" if args.package else f"com.droid.dolphy.{name}Screen"

    print(f"created {target.relative_to(ROOT)}")
    print(f"added strings {args.id}_title / {args.id}_desc\n")
    print("1) FunctionCatalog.kt — add inside the section list:")
    print(
        f'   FunctionDestination(Icons.Default.Extension, stringResource(R.string.{args.id}_title), '
        f'stringResource(R.string.{args.id}_desc), "other/{args.id}", {SECTIONS[args.section]}),'
    )
    print("\n2) MainActivity.kt — add a route next to the other `composable(\"other/...\")` entries:")
    print(f'   composable("other/{args.id}") {{ {name}Screen(screenNavController) }}')
    print(f"\n3) import {import_path}")
    print("\n4) python tools/i18n/add_locale.py && python tools/i18n/check_coverage.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
