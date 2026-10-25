# Dolphy Enhanced — module roadmap

An honest, prioritised list of what to build next, and what is deliberately out
of scope. Ordered by value per hour of work, not by how cool it sounds.

Every line marked **educational / authorised targets only** means exactly that:
run it against hardware you own or are contracted to test.

## P0 — unblocks everything else

| # | Item | Why it matters |
|---|------|----------------|
| 1 | Extract the remaining ~355 hardcoded Russian literals from 37 Kotlin files into `strings.xml` | Last real localisation blocker. `FunctionCatalog.kt`, `OtherScreen.kt` and the two BLE/HID screens are done; the NFC trolls, IR and camera screens remain. |
| 2 | Move `assets/Flipper-IRDB-main` (94 MB) to a downloadable pack | Cuts the APK from 85 MB to ~15 MB. Ship a manifest + a first-run download. |
| 3 | Delete the 354 dead English keys reported by `tools/i18n/check_coverage.py` | Free translation work on every future change. |
| 4 | Unit tests for `WordlistStore`, `WordlistGenerator`, `CardCatalog`, `RootDetector` | Pure Kotlin logic, no Android deps — cheapest tests in the project. |

## P1 — features people will actually notice

| # | Item | Notes |
|---|------|-------|
| 5 | **Functions screen: sections, favourites, recent** | Search exists now; add a "recently used" row driven by `recordCardUsage`, and let users hide sections they never open. |
| 6 | **Plugin manager overhaul** | Current install flow is functional but rough: add permission review before install, a signature/hash badge, and an uninstall that actually cleans files. |
| 7 | **AI module generator** | OpenAI-compatible client: configurable base URL + key stored in `EncryptedSharedPreferences`, prompt → `.dolphyplugin` scaffold, mandatory human review before install. Needs a provider endpoint decided first. |
| 8 | **IR remote search by brand** | The 94 MB database is there but search is keyword-only; add brand → model → device-type browsing and a "last 10 used" row. |
| 9 | **NFC scan → card database lookup** | When a tag is read, resolve ATQA/SAK/AID against `assets/nfc/cards.json` and show the card identity automatically instead of raw hex. |
| 10 | **Wordlist packs in more languages** | The generator is Latin-only. Add Cyrillic and CJK keyboard patterns to `WordlistGenerator`. |

## P2 — deepening existing modules

| # | Item | Notes |
|---|------|-------|
| 11 | BLE scanner: OUI/company resolution | IEEE registry lookup on the device address; ship a curated top-500 prefix table in `assets/oui/`. |
| 12 | Wi-Fi scanner: band/channel/width detail | Already in the plugin example; promote it to a native screen. |
| 13 | QR tools: batch generation and history | |
| 14 | HID keyboard: layout picker (AZERTY/QUERTY/etc.) | Matters for every non-QWERTY user; currently QWERTY only. |
| 15 | Themes: per-section accent colours, saved theme presets | The engine exists; add save/import/export. |
| 16 | Widgets: configurable shortcut widget | Pick the module it opens instead of the fixed BLE spam one. |

## P3 — polish

| # | Item |
|---|------|
| 17 | Adaptive launcher icon themed to the accent colour (Android 13+) |
| 18 | Predictive back and shared-element transitions between the Functions grid and screens |
| 19 | Baseline profile + R8 rules for release builds (startup time) |
| 20 | Screenshot-friendly light theme pass: contrast audit on every screen |

## Scope

Dolphy Enhanced is distributed under GPL v3 and gated by a first-launch consent
screen: modules are meant for equipment you own or are explicitly authorised to
test. Keep that gate in place if you fork this further — it is the only thing
standing between "research tool" and "liability".
