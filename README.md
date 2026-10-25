# Dolphy Enhanced

An enhanced fork of [Dolphy](https://github.com/unvoiddd/Dolphy-App) — an Android
multi-tool for wireless protocol research, NFC/RFID work and hardware emulation.
No root required for most modules.

**Enhanced by [camzzz](https://github.com/cameleonnbss)** · original project by
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ Use only on devices and networks you own or are explicitly authorised to
> test. Jamming, disrupting third-party devices or intercepting other people's
> traffic is illegal in most countries and is **not** part of this build.

---

## What this fork adds

### 15 languages, properly wired
`values/` is English (the fallback), so a partially translated locale degrades to
English instead of Russian. A searchable language picker, native language names,
RTL support for Arabic, and a "follow the system" mode.

`English · Русский · Français · Español · Deutsch · Italiano · Português ·
Türkçe · Українська · Polski · العربية · हिन्दी · Bahasa Indonesia · 中文 ·
日本語 · 한국어`

### Wordlists that do not blow up the phone
- Bundled packs streamed line by line (`common`, `router_defaults`).
- Imported lists are normalised once and stored as files, not in
  SharedPreferences — a 14M-line rockyou import no longer OOMs.
- **Generated patterns**: masks produce hundreds of thousands of realistic
  candidates (8-digit space with an even stride, word+year, word+4 digits,
  vendor+suffix) with zero APK weight.

### Card database
A browsable reference of ~37 NFC/RFID families — MIFARE Classic/Plus/DESFire,
NTAG, ICODE, FeliCa, Calypso, EMV AIDs (Visa, Mastercard, Amex, UnionPay, JCB,
MIR), HID iCLASS/Seos, LEGIC, City Union / T-Union transit, ePassport, hotel
keys, and the 125 kHz credentials a phone *cannot* read. Searchable by name,
standard, ATQA/SAK or AID.

### Root detection
`RootDetector` reports whether the device is rooted, which manager is installed
(Magisk / KernelSU / APatch / SuperSU), the `su` path and version — cached for a
minute and bounded by a timeout, so a denied Magisk prompt cannot hang the UI.

### Developer tooling
- `tools/i18n/` — parity, generation and coverage checks for translations.
- `tools/new-module.py` — scaffolds a screen, its strings, and prints the catalog
  and nav snippets to paste.
- `docs/ADD-A-MODULE.md` — the three extension paths (plugin / native / assets).

---

## Build

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS (no wrapper script in this repo — use a local Gradle 8.11+)
gradle assembleDebug
```

Requires JDK 17+ and Android SDK 36. The output APK lands in
`app/build/outputs/apk/debug/`.

Release builds are signed with the debug key by default — replace
`signingConfigs` before publishing.

## Project layout

```
app/src/main/java/com/droid/dolphy/     Kotlin sources (Compose UI + engines)
app/src/main/assets/                    IR database, BLE payloads, wordlists, NFC cards
app/src/main/res/values/                English strings (default)
app/src/main/res/values-<lang>/         Translations
tools/                                  i18n pipeline + module scaffolder
docs/                                   Module guide, changelog, roadmap
plugin-examples/                        .dolphyplugin samples
```

## Documentation

- [`docs/ENHANCED.md`](docs/ENHANCED.md) — what changed in 2.7, roadmap, and what
  is deliberately out of scope.
- [`docs/MODULE-ROADMAP.md`](docs/MODULE-ROADMAP.md) — the full module roadmap.
- [`docs/ADD-A-MODULE.md`](docs/ADD-A-MODULE.md) — extend the app.

This README is also available in:
[Français](README-fr.md) · [Español](README-es.md) · [Deutsch](README-de.md) ·
[Русский](README-ru.md) · [中文](README-zh.md)

## Releases

Debug builds are produced by CI on every push to `main` (see
`.github/workflows/build.yml`). To cut a tagged release:

```bash
git tag -a v2.9 -m "Dolphy Enhanced 2.9"
git push origin v2.9
```

Suggested release description:

> **Dolphy Enhanced 2.9**
> AI module generator (any OpenAI-compatible endpoint, review before install) ·
> first-launch usage gate · Functions screen search · new adaptive icon · OUI
> manufacturer database (335 prefixes) · 15 languages with English as the
> fallback · streaming wordlists + generated patterns · NFC/RFID card database
> (37 families) · root detection with manager and version · README in 5
> languages.
> Educational and authorised targets only.

## Licence

GNU GPL v3. See [`LICENSE`](LICENSE). The upstream attribution stays: it is a
licence requirement, not a courtesy.
