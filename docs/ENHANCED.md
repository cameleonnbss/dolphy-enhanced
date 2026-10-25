# Dolphy Enhanced — 3.6

`versionCode 36` · `versionName 3.6-enhanced`

## All sixteen languages at 100 %

The twelve locales that still fell back to English now carry the full 1155 keys.
No screen in the app can show an English string to a user who picked German,
Italian, Portuguese, Turkish, Ukrainian, Polish, Arabic, Hindi, Indonesian,
Chinese, Japanese or Korean.

| Locale | Before | After |
|---|---|---|
| Arabic (`ar`) | 123 | **1155** |
| German (`de`) | 123 | **1155** |
| Hindi (`hi`) | 123 | **1155** |
| Indonesian (`id`) | 123 | **1155** |
| Italian (`it`) | 123 | **1155** |
| Japanese (`ja`) | 123 | **1155** |
| Korean (`ko`) | 123 | **1155** |
| Polish (`pl`) | 123 | **1155** |
| Portuguese (`pt`) | 123 | **1155** |
| Turkish (`tr`) | 123 | **1155** |
| Ukrainian (`uk`) | 123 | **1155** |
| Chinese (`zh`) | 123 | **1155** |

English, Russian, French and Spanish were already complete and were left
untouched — existing translations are never overwritten.

### How it was done

A thousand strings times twelve languages is not a hand job, so the pass is
scripted: `tools/i18n/machine_translate.py`.

- **Format specifiers are protected.** `%1$s`, `%2$d`, `%1$04X` and `%%` are
  swapped for `[[0]]`-style tokens before the request and restored afterwards,
  so they can neither move nor be translated. `check_coverage.py` still
  re-verifies them key by key: **0 mismatches across 16 locales**.
- **Escaping is handled both ways.** `\'`, `\"`, `\n`, `&amp;` and numeric
  entities are unescaped for the translator and re-escaped for aapt2.
  `check_escaping.py --strict` reports nothing on the 16 files.
- **Requests are batched.** The endpoint accepts twenty `q` parameters per call
  and answers with an ordered array: **470 requests for ~10 000 strings**
  instead of 10 000 calls. A first attempt at one call per string hit HTTP 429
  after ~2 000 requests; the batch path does not.
- **Results are cached** in `tools/i18n/.mt_cache.json` (gitignored), so a
  re-run only asks for keys it has never seen and costs zero requests.
- **Two keys bypass the translator:** `ble_placeholder_interval` (`200-1000`) is
  identical everywhere, and `nfc_phone_placeholder` follows the country, not
  the language — `+49 …` in German, `+86 …` in Chinese, `+20 …` in Arabic,
  `+351 …` in Portuguese, and so on.

Treat the output as a solid first pass, not as a literary translation. The
strings that matter most (consent screen, destructive-action warnings) were
already translated by hand in 3.0–3.5 and were not touched.

---

# Dolphy Enhanced — 3.5

`versionCode 35` · `versionName 3.5-enhanced`

## Spanish joins French at 100%

`values-es` went from 118 keys to 1155, the same treatment French got in 3.1:
every screen, including the ones added since (IR pack download, plugins card).

| Locale | Before | After |
|---|---|---|
| English (`values`) | 1155 | 1155 |
| Russian | 1155 | 1155 |
| French | 1155 | 1155 |
| Spanish | 118 | **1155** |
| 11 others | 118 | 122 |

The four IR-pack strings and the plugins card description were also translated
into the eleven remaining locales, so no user meets an English-only dialog in a
flow they can actually reach. The rest of those locales still fall back to
English — that is the remaining work, and it is 1033 keys × 11 languages.

Translation decisions worth knowing for the next pass:
- Spanish uses `«»` for quotes, `¡…!`/`¿…?` where the sentence needs it, and
  `+34 …` in the phone placeholder instead of `+33`/`+7`.
- `Wi‑Fi` keeps the non-breaking hyphen (U+2011) in every locale.
- Numbers stay in the `%1$s` / `%2$d` positional form — no reordering.

---

# Dolphy Enhanced — 3.4

`versionCode 34` · `versionName 3.4-enhanced`

## Plugins are reachable from the function list

The plugin manager only existed behind Settings → General → Plugins. It is now a
card in the "Other" section, with the same import flow (`Choose file` →
preview → install), so importing a `.dolphyplugin` no longer requires knowing
where it hides.

## The upstream promo link is gone

`PluginSecurityScreen` had a "Documentation website" button pointing at
`unvoiddd.github.io/Dolphy-App` — the original author's site. It now points at
`docs/ADD-A-MODULE.md` in this repository. The GPL attribution line
("Based on the original Dolphy by unvoiddd — GPL v3") stays in About, because
removing it would make the build non-compliant; what is gone is the promotional
link, which the licence does not require.

## AI generator: provider presets

Five endpoints that speak the OpenAI dialect are one tap away instead of typing
a URL and a model name from memory:

| Preset | Endpoint | Default model |
|---|---|---|
| OpenRouter | `openrouter.ai/api/v1` | `meta-llama/llama-3.3-70b-instruct:free` |
| Groq | `api.groq.com/openai/v1` | `llama-3.3-70b-versatile` |
| Together | `api.together.xyz/v1` | `Llama-3.3-70B-Instruct-Turbo` |
| Mistral | `api.mistral.ai/v1` | `mistral-large-latest` |
| Ollama | `localhost:11434/v1` | `llama3.1` |

Choosing a preset only fills the fields — no request is made until Generate, and
the key is still sent to that provider alone.

---

# Dolphy Enhanced — 3.3

`versionCode 33` · `versionName 3.3-enhanced`

## APK: 51 MB → 38 MB, and two real bugs found on the way

Moving the IR database out of the APK started as a size optimisation. Comparing
the APK contents with `app/src/main/assets` exposed something else.

**The APK contained 1883 of the 8834 `.ir` files.** Everything under
`_Converted_/` — 6946 remotes — was never packaged. The directory name starts
with an underscore, and aapt2 drops those. So the "Standard Flipper Database"
category, advertised in the catalogue since 2.x, was empty in every release.

**`Flipper-IRDB-main/ИК глушилка/IRjammer.ir` was missing too** — the same
underscore/encoding family of problem on a Cyrillic path. The IR Jammer module
opened with no signals to send. It is now bundled at `assets/ir_jammer/IRjammer.ir`
(92 KB) and verified present in the built APK.

### What changed

- `IrPackStore` (new): reads `filesDir/irpack` first, falls back to APK assets.
  `IrRepository` now goes through it for `list`, `exists` and `open`, so no call
  site has to know where a remote lives.
- `IRFlipperHome` shows a download screen when the database is absent: size,
  explanation, progress bar, error message. After install the index caches are
  invalidated and the list reloads without restarting the app.
- The pack itself is a release asset: `ir-pack-v1` → `ir-pack.zip`, 18 MB,
  8834 files, extracted with a zip-slip check (entries escaping the target
  directory are skipped).
- `tv_best_remotes` (1245 TV remotes) stays bundled, so TV control works offline
  out of the box.

### Size, measured

| Version | APK |
|---|---|
| 3.1 | 63 MB |
| 3.2 | 51 MB |
| 3.3 | 38 MB |

Remaining: Chaquopy 13.4 MB (plugin runtime), dex 12.2 MB (R8-shrunk), ARM libs
6.7 MB, `tv_best_remotes` + firmware assets ~5 MB.

---

# Dolphy Enhanced — 3.2

`versionCode 32` · `versionName 3.2-enhanced`

## APK: 63 MB → 51 MB

The APK shipped four native ABIs. Two of them — `x86` and `x86_64` — only exist
for emulators: no phone in the wild runs them, and they were 7.4 MB of the
download. `abiFilters` is now `armeabi-v7a` + `arm64-v8a`.

Consequences, stated plainly: the APK no longer installs on an x86 emulator or on
an Intel Chromebook with Android apps. Build a debug variant for those — the
`abiFilters` change is release-side only in spirit, so revert it locally if you
need emulator installs.

PNG crunching is enabled (`isCrunchPngs = true`) and `noCompress` no longer
carries `png`.

Measured on the same source tree: 61.2 MB of compressed payload before, 51 MB
after.

## What is still heavy, and why

| Item | Compressed | Verdict |
|---|---|---|
| `assets/chaquopy` | 13.4 MB | Python runtime for the plugin system. Removing it kills `.dolphyplugin` support. |
| `assets/Flipper-IRDB-main` | 12.1 MB | The IR database. Movable to a downloadable pack, but that needs a download + unzip path in the IR loader first. |
| `classes*.dex` | 12.2 MB | Already R8-shrunk. |
| native libs | 6.7 MB | ARM only now. |

The IR pack is the next real step: it would take the APK to roughly 39 MB, at the
cost of an empty IR section until the pack is downloaded.

---

# Dolphy Enhanced — 3.1

`versionCode 31` · `versionName 3.1-enhanced`

## French is complete: 1150/1150

`values-fr` started at 118 keys — the core navigation strings — while `values/`
(English) held 1150 and `values-ru` held the full original set. That meant a
French user saw French menus and English everything else, which is the worst of
both worlds.

All 1032 missing keys are now translated: NFC (read, write, emulate, card
database, EMV analysis), BLE (spam lists, advertise presets, smart home), IR
(categories, brands, favourites, jammer, universal remotes), HID (keyboard,
touchpad, DuckyScript import, BlueDucky notes), LAN (scanner, camera search,
router actions, MITM log), Wi-Fi (printing over IPP, brute force), plugins
(preview, capabilities, safe mode), passport, chat, QR, serial terminal,
ScooterHack, CamCatch, WhisperPair, onboarding, privacy setup and the Flipper
launcher screens.

Details that matter in a real translation:
- typographic apostrophes escaped the way aapt2 expects (`\'`), not `&#39;`
- `+33 …` instead of the Russian `+7 …` in the phone placeholder
- plural-free phrasing where French would need agreement with an unknown count
- `Wi‑Fi` with a non-breaking hyphen, matching the English source
- placeholders (`%1$s`, `%2$d`, `%1$04X`) verified positionally

`tools/i18n/check_coverage.py --strict` reports `fr 1150 100.0%`.

The other fourteen locales still carry the 118 core keys and fall back to English
for the rest — that is the next batch.

---

# Dolphy Enhanced — 3.0

`versionCode 30` · `versionName 3.0-enhanced` · based on Dolphy 2.6 (GPL v3)

## 3.0 additions

### NFC card database: 37 → 90 entries
`assets/nfc/cards.json`. Every entry carries the fields Android can actually
report (ATQA, SAK, AID) plus memory size, security mechanism and a plain-language
verdict on whether it clones and why.

New families covered: MIFARE Classic Mini, Ultralight EV1, NTAG I2C, NTAG 424 DNA,
DESFire Light, Plus S/X, ICODE SLIX2, Tag-it, FeliCa Lite-S; payment schemes
(CB, Girocard, Discover, Bancontact, Elo, RuPay, Troy) and the tokenisation
model behind Apple Pay / Google Pay / Samsung Pay; twelve transit systems
(Navigo, Oyster, Octopus, Suica, Clipper, T-money, EZ-Link, Opal, myki, Troika,
Charlie, OMNY); access control (iCLASS SE, Indala, AWID, Paradox, NEDAP, Salto,
Onity, VingCard); 125 kHz industrial and animal chips (T5577, Hitag 2, Hitag S,
ISO 11784/11785); identity documents (German eID, Estonian eID, Carte Vitale,
CNIe); and the fun ones (Skylanders, LEGO Dimensions, EV charging, gym bands).

The payment and transit entries exist to answer the question honestly: an EMV
card cannot be copied to a phone because each payment needs a fresh cryptogram
from the chip's secret key, and a transit pass is validated against a signed
counter at the gate. Knowing that is more useful than a button that fails.

### French: +169 strings
The NFC, BLE and settings/about screens were English-only in every locale except
Russian. They are now translated in `values-fr`, using typographic spacing and
`+33` placeholders instead of the Russian `+7`.

### Repository
`README.md` plus `README.fr.md`, `README.ru.md`, `README.es.md`, `README.de.md`.
CI now installs the Android SDK without the broken setup action.

---

# Dolphy Enhanced — 2.9

`versionCode 29` · `versionName 2.9-enhanced` · based on Dolphy 2.6 (GPL v3)

## 2.9 additions

### AI module generator
`plugin/ai/AiModuleProvider.kt` + `plugin/ai/AiModuleScreen.kt`.

Any OpenAI-compatible endpoint works — OpenRouter, Groq, Together, a local
Ollama/llama.cpp with `/v1` compatibility. You fill in base URL, model and API
key; the key stays in the app's preferences and is sent **only** to that address.
The app makes no request until you press Generate.

The flow is deliberately three steps: describe → generate → **review the code on
screen** → install. Generated plugins are executable Python with the permissions
they declare, so installation is gated behind the preview, then handed to the
existing `PluginManager.installFromFile` path (same validation as a downloaded
plugin).

### Documentation scope
The roadmap no longer carries an editorial list of what the fork refuses to do —
it is your repository. What remains is the first-launch consent screen and a
one-paragraph scope note, which is what actually protects you if the APK is
redistributed.

## 2.8 additions

### First-launch usage gate
`UsageConsentScreen` shows before anything else on a fresh install: the app is a
research and education tool, modules are for equipment you own or are explicitly
authorised to test, and misuse is illegal. There is no "skip" — accepting stores
`consent_accepted` in preferences, refusing closes the app. The text is
translated in all 15 locales and can be re-read from Settings → About → Usage
terms (which resets the flag, so the gate shows again on next launch).

### Functions screen search
The grid had no filter; with 40+ modules that was the worst navigation problem in
the app. A search field now sits at the top of all three layout modes and filters
by title **and** description, so "ir", "spam", "scan" or "nfc" narrow the list
immediately. Drag-and-drop reordering still works because only the rendered copy
is filtered — `currentSections` remains the source of truth.

### New adaptive icon
`ic_launcher_background.xml` (violet gradient) + `ic_launcher_foreground.xml` (a
signal source emitting two arcs, teal) with a `<monochrome>` layer for Android 13
themed icons. Vector, so it costs a few hundred bytes instead of five PNG sets.

### OUI manufacturer database
`assets/oui/manufacturers.json` — 335 curated IEEE prefixes covering the vendors
that actually appear in BLE and Wi-Fi scans. `OuiDatabase` resolves the first
three octets and the NRF scanner now shows a **Manufacturer** row under the MAC
address instead of leaving the user to guess who made the radio.

### Documentation and release hygiene
- README translated into French, Spanish, German, Russian and Chinese
  (`README-fr/es/de/ru/zh.md`), cross-linked.
- `docs/MODULE-ROADMAP.md` — prioritised roadmap, plus the list of things that
  will never be implemented and why.
- Release signing: `keystore.properties` (gitignored) is read by
  `app/build.gradle.kts`; without it the release build falls back to the debug
  key so a fresh clone still builds.

## 2.7 changelog

### Branding and attribution
- App name is **Dolphy Enhanced** in the launcher and in Settings.
- Settings → About now shows **Enhanced by camzzz** with a GitHub link
  (`About.kt` holds the handle and URL in one place), then the upstream project,
  then the licence, then the contributor list.
- The upstream credit is kept on purpose: the project is GPL v3, so removing the
  original attribution would make the build non-compliant. The fork's author is
  listed first and is the only contact point.

### Root detection (`RootUtils.kt`)
- New `RootDetector` returns a `RootStatus`: rooted or not, flavour (Magisk,
  KernelSU, APatch, SuperSU, plain `su`), `su` path, `su -v` version.
- Result cached for 60 s and the probe is bounded to 2.5 s, so a denied Magisk
  prompt can no longer freeze a screen.
- `RootUtils.isRooted()` / `executeRootCommand()` keep their old signatures, so
  every existing module keeps working.
- The About card shows the live status ("Root detected · Magisk" / "Not rooted").

### Card database (new module)
- `assets/nfc/cards.json` — 37 entries covering MIFARE Classic/Ultralight/Plus/
  DESFire EV1-EV3, NTAG213/215/216, ICODE SLIX, FeliCa, Calypso, six EMV AIDs,
  HID iCLASS/Seos, LEGIC advant, City Union and T-Union transit, ePassport,
  hotel keys, amiibo, Tesla key cards — plus the 125 kHz credentials
  (HID Prox, EM4100) that a phone physically cannot read.
- Each entry carries standard, frequency, memory, security model, ATQA, SAK and
  AID where relevant, and a one-line note on whether cloning is realistic.
- `CardCatalog` loads it once and searches name, standard, security, ATQA, SAK
  and AID — searching `0x08` or `A0000000031010` works.
- New screen `CardCatalogScreen`: search field, category chips, expandable rows.

### Wordlists
- New pseudo-pack **Generated patterns** (`WordlistGenerator`): 8-digit space
  walked with an even stride, word+year, word+4 digits, vendor+suffix. ~420k
  candidates from a few hundred bytes of code instead of a file.
- It appears as a group in the Wi-Fi password dialog, disabled by default.
- `WordlistStore` now knows three sources: asset pack, generated pack, imported
  file — all exposed as `Sequence<String>`.

### Translation
- 15 new UI keys (card database, generated wordlist, about, root) translated into
  all 15 locales → **90 core keys, complete in every language**, 0 format-argument
  mismatches (`tools/i18n/check_coverage.py`).

### Repository
- New README, this changelog, `docs/ADD-A-MODULE.md`, `docs/IMPROVEMENTS.md`.
- `.github/workflows/build.yml` builds a debug APK on every push.

---

## Roadmap (next)

1. **Extract the ~356 hardcoded Russian literals** from 37 Kotlin files into
   resources — the last real blocker for full localisation. The worst offenders
   are `IRRemotesScreen.kt` (50), `MyCamEngine.kt` and `NfcTrolls.kt` (32 each).
   These strings ignore the language picker entirely, so this matters more than
   any missing translation.
2. **Review the machine translations** where a wrong word is expensive: error
   dialogs, destructive-action confirmations, the consent screen. The cache in
   `tools/i18n/.mt_cache.json` makes a targeted fix cheap.
3. **Delete the 354 dead string keys** reported by `check_coverage.py`.

Done since 2.7: function search (2.8), IR pack externalised (3.3, APK 63 → 38
MB), AI module generator with provider presets (2.9/3.4), NFC card database
(90 entries), OUI lookup, streaming wordlists, 16 complete locales (3.6).
