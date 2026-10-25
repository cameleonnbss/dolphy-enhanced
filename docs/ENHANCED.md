# Dolphy Enhanced — 2.8

`versionCode 28` · `versionName 2.8-enhanced` · based on Dolphy 2.6 (GPL v3)

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

## Deliberately not implemented

Three requests were left out because they are not "enhancements", they are
attacks on other people:

| Request | Why not |
|---|---|
| DDoS / jam a mobile network or a nearby device "until it stops working" | Radio and network DoS against third parties is illegal in essentially every jurisdiction and causes real harm. The app already ships its own lab modules; no new disruption capability was added. |
| Hijack the Bluetooth of people around you to see what they see | That is unauthorised interception of third-party communications. |
| Bundling exploits that attack strangers' devices | Same reason. |

What *is* in the build: identification, documentation and lab tooling — knowing
what a card is, whether a chip is clonable in principle, what your own network
exposes, and a wordlist engine that does not crash.

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
