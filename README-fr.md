# Dolphy Enhanced — Français

Fourche améliorée de [Dolphy](https://github.com/unvoiddd/Dolphy-App) — une
boîte à outils Android multi-protocoles pour la recherche sur les ondes, le
NFC/RFID et l'émulation matérielle. La plupart des modules fonctionnent sans
root.

**Amélioré par [camzzz](https://github.com/camzzz)** · projet original par
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ À n'utiliser que sur des équipements qui vous appartiennent ou que vous
> êtes explicitement autorisé à tester. Brouiller, perturber ou intercepter des
> appareils tiers est illégal dans la plupart des pays et **ne fait pas partie**
> de cette version. Un écran d'acceptation s'affiche au premier lancement.

## Ce que cette fourche ajoute

- **15 langues** correctement câblées : `values/` contient l'anglais, donc une
  langue partiellement traduite retombe sur l'anglais et non sur le russe.
  Sélecteur avec recherche, noms natifs, support RTL (arabe), mode
  « comme le système ». *(English · Русский · Français · Español · Deutsch ·
  Italiano · Português · Türkçe · Українська · Polski · العربية · हिन्दी ·
  Bahasa Indonesia · 中文 · 日本語 · 한국어)*
- **Wordlists qui ne font plus planter le téléphone** : packs fournis lus ligne
  par ligne, imports normalisés une fois puis stockés en fichier (plus de blob
  JSON dans les SharedPreferences), et **motifs générés** — des centaines de
  milliers de candidats produits par masques, sans aucun poids dans l'APK.
- **Base de cartes** : 37 familles NFC/RFID documentées (MIFARE, DESFire, NTAG,
  FeliCa, Calypso, AID EMV, HID, LEGIC, cartes de transport, ePassport…),
  cherchables par nom, ATQA, SAK ou AID.
- **Détection root** : racine ou non, gestionnaire (Magisk / KernelSU / APatch /
  SuperSU), chemin et version de `su`, avec cache et timeout.
- **Écran d'acceptation** au premier lancement : usage éducatif et cibles
  autorisées uniquement, relisable depuis Réglages → À propos.
- **Recherche** dans l'écran Fonctions, nouvelle icône adaptative, nom
  « Dolphy Enhanced ».

## Compiler

```bash
gradlew.bat assembleDebug     # Windows
./gradlew assembleDebug       # Linux / macOS
```

Nécessite JDK 17+ et le SDK Android 36. L'APK sort dans
`app/build/outputs/apk/debug/`.

## Documentation

- [`docs/ENHANCED.md`](docs/ENHANCED.md) — journal des modifications et feuille
  de route.
- [`docs/MODULE-ROADMAP.md`](docs/MODULE-ROADMAP.md) — ce qui est prévu, et ce
  qui est volontairement hors périmètre.
- [`docs/ADD-A-MODULE.md`](docs/ADD-A-MODULE.md) — étendre l'application.

## Licence

GNU GPL v3, voir [`LICENSE`](LICENSE). La mention du projet d'origine est
conservée : c'est une obligation de licence, pas une courtoisie.

[← Retour au README anglais](README.md)
