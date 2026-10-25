# Dolphy Enhanced — Español

Bifurcación mejorada de [Dolphy](https://github.com/unvoiddd/Dolphy-App), una
multiherramienta Android para investigación de protocolos inalámbricos,
NFC/RFID y emulación de hardware. La mayoría de los módulos funcionan sin root.

**Mejorado por [camzzz](https://github.com/camzzz)** · proyecto original de
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ Úsala solo en equipos de tu propiedad o que estés explícitamente
> autorizado a probar. Interferir, bloquear o interceptar dispositivos de
> terceros es ilegal en la mayoría de los países y **no forma parte** de esta
> versión. Hay una pantalla de aceptación en el primer arranque.

## Lo que añade esta bifurcación

- **16 idiomas, todos completos**: `values/` contiene el inglés, así que un
  idioma parcialmente traducido cae al inglés y no al ruso. Las dieciséis
  locales llevan las 1155 claves. Selector con búsqueda, nombres nativos,
  soporte RTL (árabe) y modo «como el sistema».
- **Listas de palabras que ya no bloquean el teléfono**: packs incluidos leídos
  línea por línea, importaciones normalizadas una vez y guardadas en archivo (ya
  no hay un blob JSON en las preferencias) y **patrones generados**: cientos de
  miles de candidatos producidos por máscaras, sin ocupar nada en el APK.
- **Base de datos de tarjetas**: 37 familias NFC/RFID documentadas (MIFARE,
  DESFire, NTAG, FeliCa, Calypso, AID EMV, HID, LEGIC, tarjetas de transporte,
  ePassport…), buscables por nombre, ATQA, SAK o AID.
- **Detección de root**: si está rooteado, qué gestor (Magisk / KernelSU /
  APatch / SuperSU), ruta y versión de `su`, con caché y tiempo de espera.
- **Pantalla de aceptación** en el primer arranque: uso educativo y solo en
  objetivos autorizados, releíble desde Ajustes → Acerca de.
- **Búsqueda** en la pantalla de Funciones, nuevo icono adaptativo, nombre
  « Dolphy Enhanced ».

## Compilar

```bash
gradlew.bat assembleDebug     # Windows
./gradlew assembleDebug       # Linux / macOS
```

Requiere JDK 17+ y el SDK de Android 36. El APK queda en
`app/build/outputs/apk/debug/`.

## Documentación

- [`docs/ENHANCED.md`](docs/ENHANCED.md) — registro de cambios y hoja de ruta.
- [`docs/MODULE-ROADMAP.md`](docs/MODULE-ROADMAP.md) — qué está previsto y qué
  queda deliberadamente fuera.
- [`docs/ADD-A-MODULE.md`](docs/ADD-A-MODULE.md) — ampliar la aplicación.

## Licencia

GNU GPL v3, ver [`LICENSE`](LICENSE). La mención al proyecto original se
mantiene: es una obligación de licencia, no una cortesía.

[← Volver al README en inglés](README.md)
