# Adding a module to Dolphy

Dolphy has three extension paths. Pick the lightest one that fits.

| Path | When | Cost | Where |
|------|------|------|-------|
| **Plugin** | Your module only needs Python/JS + the device bridge | No rebuild of Dolphy | `.dolphyplugin` file |
| **Native screen** | You need Compose UI, native APIs, or tight integration | Rebuild the APK | `app/src/main/java/...` |
| **Asset pack** | You only add data (remotes, IR codes, wordlists) | Rebuild (or drop-in folder) | `app/src/main/assets/` |

---

## 1. Plugin (fastest, no APK rebuild)

A plugin is a single `.dolphyplugin` text file containing readable Python, a
manifest, and the permissions it needs. Users open it with Dolphy and review the
source before installing.

See `plugin-examples/` for four working examples:

- `wifi_scanner_md3e.dolphyplugin` — Wi-Fi scanning, permission group `wifi`
- `ble_scanner_dex_md3e.dolphyplugin` — BLE scanning + embedded DEX, groups
  `bluetooth`, `dex`
- `radio_lab.dolphyplugin`, `integration_hooks.dolphyplugin` — smaller samples

Rules of thumb:

- Declare the smallest permission group you can (`wifi`, `bluetooth`, `dex`, …).
- Keep the UI in the plugin's Material 3 Expressive components so it matches the app.
- Never embed network calls to third-party servers: users are told to read the source.

---

## 2. Native screen (Compose)

One command scaffolds the screen, the strings, and prints the two snippets you
still paste by hand:

```bash
python tools/new-module.py \
  --id spectrum_analyzer \
  --title "Spectrum Analyzer" \
  --desc "Live 2.4 GHz band activity" \
  --section NETWORK \
  --package network
```

It creates `app/src/main/java/com/droid/dolphy/network/SpectrumAnalyzerScreen.kt`
and adds `spectrum_analyzer_title` / `spectrum_analyzer_desc` to the English
strings. Then:

1. **Register the card** in `FunctionCatalog.kt`, inside the right section list:

   ```kotlin
   FunctionDestination(
       Icons.Default.Extension,
       stringResource(R.string.spectrum_analyzer_title),
       stringResource(R.string.spectrum_analyzer_desc),
       "other/spectrum_analyzer",
       OtherSections.NETWORK,
   )
   ```

2. **Register the route** in `MainActivity.kt` next to the other `composable("other/...")` entries:

   ```kotlin
   composable("other/spectrum_analyzer") { SpectrumAnalyzerScreen(screenNavController) }
   ```

3. **Translate**: add the two keys to `tools/i18n/core_translations.py`, then

   ```bash
   python tools/i18n/add_locale.py
   python tools/i18n/check_coverage.py
   ```

### Conventions the existing screens follow

- `SectionTopBar(title = …, onBack = { navController.popBackStack() })` at the top.
- Accent colour comes from the theme; never hardcode a colour for text.
- Long-running work goes through `rememberCoroutineScope()` + `Dispatchers.IO`,
  with UI updates wrapped in `withContext(Dispatchers.Main)`.
- Every user-visible string is a resource. **No literal Russian or English text
  in Kotlin** — that was the single biggest source of untranslatable screens
  (361 literals before the cleanup).

---

## 3. Asset pack (data only)

- IR remotes: `assets/Flipper-IRDB-main/`, `assets/tv_best_remotes/`
- BadUSB scripts: `assets/bad_usb_scripts/`
- Wordlists: `assets/wordlists/` + an entry in `assets/wordlists/manifest.json`
- Router profiles: `assets/routers/*.json`

### Adding a wordlist pack

1. Put the file in `app/src/main/assets/wordlists/`.
2. Declare it in `manifest.json`:

   ```json
   { "id": "my_pack", "file": "my_pack.txt", "titleKey": "wordlist_my_pack_title", "minLength": 8 }
   ```

3. Add `wordlist_my_pack_title` to the strings + `core_translations.py`.

`WordlistStore` reads it lazily line by line — a 100 MB list costs the same RAM
as a 1 KB one. Keep one entry per line, no duplicates, no trailing spaces.
