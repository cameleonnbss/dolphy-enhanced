# Dolphy Enhanced — Deutsch

Verbesserter Fork von [Dolphy](https://github.com/unvoiddd/Dolphy-App), einem
Android-Multitool für Funkprotokoll-Forschung, NFC/RFID und Hardware-Emulation.
Die meisten Module laufen ohne Root.

**Erweitert von [camzzz](https://github.com/camzzz)** · Originalprojekt von
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ Nur auf Geräten verwenden, die dir gehören oder die du ausdrücklich testen
> darfst. Stören, blockieren oder abfangen von Fremdgeräten ist in den meisten
> Ländern illegal und **nicht Teil** dieser Version. Beim ersten Start erscheint
> ein Bestätigungsbildschirm.

## Was dieser Fork hinzufügt

- **16 Sprachen, alle vollständig**: `values/` enthält Englisch, eine
  teilweise übersetzte Sprache fällt also auf Englisch zurück, nicht auf
  Russisch. Alle sechzehn Locales tragen die 1155 Schlüssel. Auswahl mit Suche,
  nativen Namen, RTL-Unterstützung (Arabisch) und Modus „wie das System“.
- **Wortlisten, die das Telefon nicht mehr lahmlegen**: mitgelieferte Pakete
  zeilenweise gelesen, Importe einmal normalisiert und als Datei gespeichert
  (kein JSON-Block mehr in den Einstellungen) und **generierte Muster**:
  Hunderttausende Kandidaten aus Masken, ohne Platz im APK.
- **Kartendatenbank**: 37 dokumentierte NFC/RFID-Familien (MIFARE, DESFire,
  NTAG, FeliCa, Calypso, EMV-AIDs, HID, LEGIC, Fahrausweise, ePassport …),
  durchsuchbar nach Name, ATQA, SAK oder AID.
- **Root-Erkennung**: gerootet oder nicht, welcher Manager (Magisk / KernelSU /
  APatch / SuperSU), Pfad und Version von `su`, mit Cache und Zeitgrenze.
- **Bestätigungsbildschirm** beim ersten Start: Bildungszwecke und nur
  autorisierte Ziele, über Einstellungen → Über erneut lesbar.
- **Suche** im Funktionsbildschirm, neues adaptives Symbol, Name
  „Dolphy Enhanced“.

## Bauen

```bash
gradlew.bat assembleDebug     # Windows
./gradlew assembleDebug       # Linux / macOS
```

Benötigt JDK 17+ und das Android SDK 36. Die APK liegt in
`app/build/outputs/apk/debug/`.

## Dokumentation

- [`docs/ENHANCED.md`](docs/ENHANCED.md) — Änderungsprotokoll und Roadmap.
- [`docs/MODULE-ROADMAP.md`](docs/MODULE-ROADMAP.md) — was geplant ist und was
  bewusst außerhalb des Rahmens bleibt.
- [`docs/ADD-A-MODULE.md`](docs/ADD-A-MODULE.md) — die App erweitern.

## Lizenz

GNU GPL v3, siehe [`LICENSE`](LICENSE). Der Hinweis auf das Originalprojekt
bleibt: das ist Lizenzpflicht, keine Höflichkeit.

[← Zurück zum englischen README](README.md)
