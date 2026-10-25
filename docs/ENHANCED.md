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

1. **Extract the 361 hardcoded Russian literals** from 37 Kotlin files into
   resources — this is the last real blocker for full localisation.
2. **Search in the Functions screen** — the grid has no filter today; with 40+
   modules that hurts.
3. **Move the 94 MB IR database to a downloadable pack** — it is 90% of the APK.
4. **Delete the 354 dead string keys** reported by `check_coverage.py`.
5. **AI module generator**: an OpenAI-compatible client (configurable base URL +
   key stored locally) that emits a `.dolphyplugin` scaffold from a prompt. Not
   shipped yet: it needs a provider endpoint and a review step before generated
   code can be installed, and shipping it half-done would be worse than not
   shipping it.
