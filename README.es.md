# Dolphy Enhanced

[FR](README.fr.md) · [EN](README.md) · [RU](README.ru.md) · **ES** · [DE](README.de.md)

Fork mejorado de [Dolphy](https://github.com/unvoiddd/Dolphy-App) — una caja de
herramientas Android para investigación de protocolos inalámbricos, NFC/RFID y
emulación de hardware. Sin root para la mayoría de los módulos.

**Desarrollado por [camzzz](https://github.com/cameleonnbss)** · proyecto original de
[unvoiddd](https://github.com/unvoiddd) · GNU GPL v3.

> ⚠️ Úsalo solo en dispositivos y redes propios o con autorización explícita.
> La app pide aceptar estas condiciones en el primer inicio.

---

## Qué añade este fork

- **16 idiomas** con respaldo en inglés, selector con nombres nativos y soporte RTL.
- **Base de tarjetas NFC**: 90 fichas técnicas (ATQA, SAK, AID, memoria, seguridad,
  veredicto de clonación) — MIFARE, DESFire, FeliCa, Calypso, EMV, tarjetas de
  acceso, transporte y documentos de identidad.
- **Base OUI IEEE**: 335 prefijos, el fabricante aparece en el escáner BLE.
- **Generador de módulos con IA**: cualquier endpoint compatible con OpenAI.
  Flujo: describir → generar → **revisar el código** → instalar.
- **Motor de wordlists en streaming**: las importaciones no pasan por SharedPreferences.
- **Detección de root** con gestor y versión, en caché.
- **Tema Material 3 Expressive**: color de acento, escala de interfaz, fondo animado.

## Instalación

Descarga el APK firmado desde las [releases](https://github.com/cameleonnbss/dolphy-enhanced/releases).

## Compilación

```bash
# Windows
gradlew.bat assembleRelease

# Linux / macOS (el repo no incluye el script wrapper — usa Gradle 8.11+)
gradle assembleRelease
```

La firma de release usa un `keystore.properties` no versionado.

## Licencia

GNU GPL v3. El crédito al proyecto original se mantiene: la licencia lo exige.
