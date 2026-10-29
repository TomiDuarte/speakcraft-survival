# Reproducir verificaciones

`gradlew.bat build` ejecuta JUnit y GameTests. `python verification/audit_distribution.py` comprueba el JAR generado y escribe `distribution.json` local con su SHA256, dependencias, DLL y exclusión de clases de pruebas.

`VoskFileSmoke.java` reconoce el archivo PCM que se le indique (16 kHz/16 bit/mono); su caso de prueba espera «diamante». `VoskModelLoadSmoke.java` carga el modelo y decodifica silencio sin abrir el micrófono. Ambas se ejecutan con Java 25, Vosk/JNA (y Gson para la primera) en el classpath. Ninguna clase se incluye en el JAR del mod.

`language-sources.json` conserva procedencia y hashes de los nombres oficiales. Los logs, capturas locales, archivos de audio sintético y fuentes descargadas para investigar APIs no se versionan. El resumen público está en `VERIFICACION.md`; los reportes automatizados se adjuntan a cada ejecución de GitHub Actions.
