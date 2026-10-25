# Dolphy Enhanced

**FR** · [EN](README.md) · [RU](README.ru.md) · [ES](README.es.md) · [DE](README.de.md)

Fork amélioré de [Dolphy](https://github.com/unvoiddd/Dolphy-App) — une boîte à
outils Android pour la recherche sur les protocoles sans fil, le NFC/RFID et
l'émulation de matériel. Pas de root pour la plupart des modules.

**Développé par [camzzz](https://github.com/cameleonnbss)** · projet original par
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ À utiliser uniquement sur vos propres appareils et réseaux, ou avec une
> autorisation explicite. L'application demande d'accepter ces conditions au
> premier lancement.

---

## Ce que ce fork apporte

- **16 langues** avec repli sur l'anglais, sélecteur de langue avec noms natifs,
  prise en charge du RTL pour l'arabe.
- **Base de cartes NFC** : 90 fiches techniques (ATQA, SAK, AID, mémoire,
  sécurité, verdict de clonabilité) — MIFARE, DESFire, FeliCa, Calypso, EMV,
  badges d'accès, cartes de transport, documents d'identité.
- **Base OUI IEEE** : 335 préfixes, fabricant affiché dans le scanner BLE.
- **Générateur de modules par IA** : n'importe quel endpoint compatible OpenAI.
  Le flux est décrire → générer → **relire le code** → installer.
- **Moteur de wordlists en flux** : les imports ne passent jamais par
  SharedPreferences, plus de plantage sur les gros fichiers.
- **Détection de root** avec nom du gestionnaire et version, mise en cache.
- **Thème Material 3 Expressive** : couleur d'accent, échelle d'interface, fond
  animé, police Flipper.

## Installation

Téléchargez l'APK signé depuis les [releases](https://github.com/cameleonnbss/dolphy-enhanced/releases).

## Compilation

```bash
# Windows
gradlew.bat assembleRelease

# Linux / macOS (pas de script wrapper dans ce dépôt — Gradle 8.11+ requis)
gradle assembleRelease
```

La signature de release utilise un fichier `keystore.properties` non versionné.

## Licence

GNU GPL v3. Le crédit au projet d'origine est conservé : la licence l'exige.
