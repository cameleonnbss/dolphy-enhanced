# Dolphy Enhanced

[FR](README.fr.md) · [EN](README.md) · [RU](README.ru.md) · [ES](README.es.md) · **DE**

Erweiterter Fork von [Dolphy](https://github.com/unvoiddd/Dolphy-App) — ein
Android-Werkzeugkasten für Funkprotokoll-Analyse, NFC/RFID und Hardware-Emulation.
Für die meisten Module ist kein Root nötig.

**Entwickelt von [camzzz](https://github.com/cameleonnbss)** · Originalprojekt von
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ Nur auf eigenen Geräten und Netzen oder mit ausdrücklicher Erlaubnis
> verwenden. Die App verlangt beim ersten Start die Zustimmung.

---

## Was dieser Fork hinzufügt

- **16 Sprachen** mit Englisch als Rückfall, Sprachauswahl mit Eigenbezeichnungen,
  RTL-Unterstützung für Arabisch.
- **NFC-Kartendatenbank**: 90 technische Einträge (ATQA, SAK, AID, Speicher,
  Sicherheit, Klonbarkeit) — MIFARE, DESFire, FeliCa, Calypso, EMV, Zutrittsausweise,
  Fahrkarten, Ausweisdokumente.
- **IEEE-OUI-Datenbank**: 335 Präfixe, Hersteller im BLE-Scanner sichtbar.
- **KI-Modulgenerator**: jeder OpenAI-kompatible Endpoint. Ablauf: beschreiben →
  erzeugen → **Code prüfen** → installieren.
- **Streaming-Wortlisten**: Importe laufen nicht mehr über SharedPreferences.
- **Root-Erkennung** inklusive Manager und Version, gecacht.
- **Material 3 Expressive**: Akzentfarbe, UI-Skalierung, animierter Hintergrund.

## Installation

Signierte APK aus den [Releases](https://github.com/cameleonnbss/dolphy-enhanced/releases).

## Build

```bash
# Windows
gradlew.bat assembleRelease

# Linux / macOS (kein Wrapper-Skript im Repo — Gradle 8.11+ nötig)
gradle assembleRelease
```

Die Release-Signatur kommt aus einer nicht versionierten `keystore.properties`.

## Lizenz

GNU GPL v3. Die Nennung des Originalprojekts bleibt — das verlangt die Lizenz.
