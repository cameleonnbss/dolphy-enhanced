# Dolphy — what changed

Work done on top of `unvoiddd/Dolphy-App` (upstream 2.6). Everything below is in
this repository; nothing depends on a server or a paid service.

## 1. Translation was structurally broken — now fixed

**Before**

- `res/values/` was **Russian**, `res/values-en/` was English. That means every
  language you add falls back to *Russian*, not English.
- `AppLocale.kt` hardcoded two values:
  `if (language == "en") "en" else "ru"` — any other tag silently became Russian.
- The language switch was a 2-button toggle (Russian / English).
- 62 strings existed only in Russian, 16 only in English: those screens showed
  the wrong language whichever one you picked.
- One entry (`nfc_troll_no_desc`) had no closing tag and swallowed
  `wifi_print_title` — a key that simply did not exist in English.
- `wifi_passwords_count` used two positional `%d` in a non-positional format
  (AAPT warning, wrong output).
- 361 Russian string literals were hardcoded in Kotlin (37 files), so those
  screens could never be translated at all.

**After**

- `values/` is now **English** (the fallback), `values-ru/` holds Russian.
  A partially translated locale degrades to English, never to Russian.
- `LanguageCatalog.kt` + rewritten `AppLocale.kt`: any BCP-47 tag, explicit
  layout direction (RTL works for Arabic), "follow the system" mode, and a
  normaliser that accepts `en-US`, `zh_CN`, `zh-rCN`…
- New language picker: searchable dialog, native language names, `AUTO` badge
  for system default.
- **15 locales** ship: English, Русский, Français, Español, Deutsch, Italiano,
  Português, Türkçe, Українська, Polski, العربية, हिन्दी, Bahasa Indonesia,
  中文, 日本語, 한국어 — 73 core UI strings each (navigation, settings, buttons,
  sections, wordlist titles).
- English and Russian now have **exact key parity** (1108 keys each), the
  malformed entry is repaired, and format arguments are positional.

**Tooling** (`tools/i18n/`)

```bash
python tools/i18n/patch_parity.py     # keep EN/RU in sync, repair broken entries
python tools/i18n/add_locale.py       # regenerate every values-XX/strings.xml
python tools/i18n/check_coverage.py   # coverage, broken XML, %arg mismatches, dead keys
```

`check_coverage.py` also reports **353 English keys that are never referenced**
(dead weight, safe to delete) and verifies that every language declared in
`LanguageCatalog.kt` has a matching resource folder.

## 2. Wordlists: from "will OOM" to constant memory

**Before** (`NetworkDiagnosticHubScreen.kt`)

- 60 passwords hardcoded in the UI file, unsorted, with duplicates.
- Import did `readText()` → `split(Regex("\\s+"))` → `distinct()` → **then saved
  the entire list as JSON inside SharedPreferences**. Importing rockyou
  (~14M lines) meant a ~140 MB String, a 14M-element list, and a multi-hundred
  megabyte XML write into preferences.
- `allPasswords()` returned a full copy and was called three times per start.

**After**

- `network/WordlistStore.kt`: line-by-line streaming, normalisation (BOM, blank
  lines, binary noise, >128 chars), cross-list de-duplication with one HashSet,
  hard cap of 5M lines per import.
- Imported lists are written to `filesDir/wordlists/<id>.txt`; only a small
  `index.json` (id, title, count, date) is kept. Preferences never grow.
- Two bundled packs in `assets/wordlists/` (`common.txt` 264 entries,
  `router_defaults.txt` 118), declared in `manifest.json`, read lazily.
- The brute-force loop now consumes a `Sequence<String>`: memory stays flat and
  the progress line shows `[n/~planned]`.

## 3. Easier to add new things

- `tools/new-module.py` scaffolds a Compose screen + strings and prints the
  catalog entry and nav route to paste.
- `docs/ADD-A-MODULE.md` documents the three extension paths (plugin, native
  screen, asset pack) with the conventions the existing screens follow.

## 4. Verified

```
./gradlew :app:compileDebugKotlin   → BUILD SUCCESSFUL
python tools/i18n/check_coverage.py → problems: 0
```

`check_coverage.py` also confirms 0 format-argument mismatches across all
locales, which is what usually crashes translated Android apps at runtime.

## Known limits

- The 14 new locales cover the **core UI** (73 keys). The remaining ~1035 keys
  are long module descriptions that still fall back to English. Adding them is
  the same loop: translate in `core_translations.py`, re-run `add_locale.py`.
- Hardcoded Russian in 37 Kotlin files is not migrated yet; those strings need
  to be extracted to resources one screen at a time (`docs/ADD-A-MODULE.md`
  explains the convention).
- `assets/Flipper-IRDB-main` is 94 MB and dominates the APK size. Moving it to a
  downloadable pack would cut the download by roughly 90%.
