# Verificación de la versión 0.2.0

Fecha: **29 de octubre de 2026**. Compilación y pruebas locales con Windows x64, Java 25, Minecraft 26.3, Fabric Loader 0.19.5 y Fabric API 0.165.0+26.3. El usuario reportó funcionamiento de voz satisfactorio del prototipo 0.1.0 usando Loader 0.20.1; no se presenta como una medición de precisión ni una certificación de todas las configuraciones.

## Resultados ejecutados

| Verificación | Resultado |
|---|---|
| `gradlew.bat build` | BUILD SUCCESSFUL |
| JUnit | 29 aprobadas, sin fallos ni omisiones |
| GameTests con Minecraft y Mixins reales | 15 aprobadas: 14 propias y 1 base de Fabric |
| Pruebas Python del conversor kana/romaji | 6 aprobadas |
| Regeneración del catálogo con `--check` | Coincide con los datos incluidos |
| Auditoría del JAR | Vosk/JNA y DLL incluidas; pruebas excluidas; diccionarios y avisos presentes |
| `runClient` | Inicio correcto; F8, guía y botón Español verificados visualmente; cierre normal |

JUnit cubre máquina de estados/retenciones (14), configuración (2), comparación de texto (5), nombres y lecturas (5), y servicio de voz/errores técnicos (3). Las pruebas nuevas validan lecturas de objetos de supervivencia, cambios de nombre, no aceptar romaji como respuesta japonesa y coherencia de todo el catálogo incluido.

Los GameTests ejercitan recogida real, crafteo normal/derecho/Shift/Q, inventario 2×2 y mesa 3×3, cursor existente, rechazo seguro del intercambio numérico, inventario lleno, entrega única, modo creativo, codec de stacks y guardado/carga vanilla del jugador con idioma, desbloqueos y objetos retenidos. Las pruebas del núcleo cubren reintentos, vencimiento, cola, reinicios, tokens antiguos y suspensión sin penalización.

La configuración y guía en español caben en el área de juego de **854×480** utilizada para la inspección. [Captura](docs/images/japanese-guide.png). No se recorrieron manualmente todos los tamaños de GUI ni todos los estados del HUD.

## Voz

En la preparación del prototipo se cargaron las DLL Vosk/JNA Windows y se reconoció «diamante» con el modelo español real desde un WAV sintético, sin gramática que fuerce la respuesta. También se instaló el modelo japonés oficial y se verificó su carga y decodificación de silencio. El usuario probó el mod y reportó que funciona; no se guardaron grabaciones de esa prueba.

No se ejecutó una evaluación sistemática con hablantes ni reconocimiento real en alemán. La variación entre micrófonos, voces, ruido y vocabulario sigue necesitando pruebas. Las 2.777 lecturas son ayudas de diccionario con correcciones; no fueron revisadas individualmente por un especialista japonés.

## Reproducibilidad

```powershell
.\gradlew.bat build
python -m unittest discover -s scripts/readings -p "test_*.py"
python verification/audit_distribution.py
.\scripts\Package-Release.ps1
```

El build produce reportes en `build/reports/tests/test`. GitHub Actions está configurado para repetir build, pruebas y empaquetado y adjuntar reportes. El paquete publicado incluye `SHA256SUMS.txt`. [Herramientas de verificación](verification/README.md).

Los logs y capturas de investigación locales no se suben automáticamente porque pueden contener rutas personales. El mensaje Vosk sobre `missing-native-model` pertenece a una prueba deliberada que pasa. Java 25 informa acceso nativo de JNA y Gradle advierte deprecaciones futuras; no impiden el build fijado. Un bloqueo local de la carpeta anterior de resultados se resolvió apartándola y generando resultados nuevos antes de publicar.

## Límites pendientes

No se verificaron servidores públicos, otros sistemas operativos, cada combinación de dispositivo o fallos eléctricos durante guardados. Cofres, hornos, intercambios y otros adaptadores quedan fuera del alcance actual. No se garantiza resistencia a clientes modificados. Las futuras versiones de Minecraft requieren nuevas pruebas. Ver [README](README.md) para compatibilidad y avisos.
