# Contribuir

Para errores, abrí un issue con Minecraft, Fabric Loader/API, versión del mod, idioma, pasos de reproducción y resultado esperado. Quitá nombres de usuario, rutas personales, direcciones de servidores y credenciales de cualquier log. No hace falta publicar tu voz para reportar un problema.

## Preparar el proyecto

Cloná el repositorio e instalá JDK 25. Ejecutá `gradlew.bat build` en Windows o `./gradlew build` en otros sistemas. Solo Windows x64 está verificado para voz. El proyecto usa Loom sin remapeo; no agregues mappings de versiones antiguas.

`src/main` controla retención, validación y persistencia en el servidor. `src/client` maneja pantalla y micrófono. `src/test` y `src/gametest` contienen las pruebas; no agregues simulaciones de voz al modo normal.

## Corregir lecturas japonesas

1. Buscá el nombre oficial en `src/main/resources/data/speakcraft/names/ja.json`.
2. Para una lectura contextual, agregá el nombre completo y su kana a `names` en `scripts/readings/overrides.json`. `terms` reemplaza solamente tokens exactos de Sudachi; no es una sustitución de subcadenas.
3. Documentá la razón o fuente en el PR. Preferí corregir nombres completos si una lectura cambia según el contexto.
4. Regenerá y probá:

```powershell
python -m venv .tools/readings-venv
.\.tools\readings-venv\Scripts\python.exe -m pip install -r scripts/readings/requirements.txt
.\.tools\readings-venv\Scripts\python.exe scripts/readings/generate.py
python -m unittest discover -s scripts/readings -p "test_*.py"
.\.tools\readings-venv\Scripts\python.exe scripts/readings/generate.py --check
.\gradlew.bat build
```

Versioná las correcciones, `ja.json`, `coverage.json` y un caso de regresión significativo. Las lecturas son ayuda visual; no agregues romaji como respuesta aceptada en japonés. No hace falta Python ni Sudachi para jugar o compilar el JAR con los datos ya generados.

## Añadir otro idioma

Agregar una carpeta de modelo no basta: la versión actual admite explícitamente `es`, `de`, `ja`.

1. Incorporá un archivo de nombres oficiales `data/speakcraft/names/<código>.json` y registrá origen/versión en `verification/language-sources.json`.
2. Agregá el código al cargador `ItemNames`, validaciones `Ledger`/`ClientSettings` y selector `SettingsScreen`. Usá `rg '"es"|"de"|"ja"' src` para revisar todos los puntos.
3. Elegí un modelo offline Vosk y documentá URL oficial, licencia, tamaño, estructura y disponibilidad en `Install-VoskModels.ps1` e instrucciones. No descargues silenciosamente ni incluyas modelos sin sus licencias.
4. Revisá `AnswerMatcher`: no elimines distinciones significativas del idioma. Justificá cualquier alias; no uses parecidos fonéticos inventados.
5. Agregá pruebas y validá captura/transcripción con hablantes. Diferenciá implementación de verificación real.

Traducir los menús es independiente: agregá o modificá `assets/speakcraft/lang/<locale>.json`.

## Distribución

Actualizá `mod_version`, README, instalación y changelog; ejecutá build, pruebas y auditoría. `scripts/Package-Release.ps1` prepara JAR/ZIP/SHA256 en `dist/<versión>`. Subilos como assets de una Release etiquetada `v<versión>` después de verificar el commit.

Nunca subas `.tools`, cachés de Gradle, modelos de voz, mundos, grabaciones, credenciales ni logs personales. El Wrapper y los avisos de terceros sí son parte de las fuentes.
