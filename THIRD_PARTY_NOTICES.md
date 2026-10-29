# Avisos de terceros

El código original de SpeakCraft Survival se licencia bajo MIT. Esa licencia no cambia la de Minecraft, sus traducciones, las dependencias o los modelos descargados.

| Componente | Uso y procedencia |
|---|---|
| Minecraft / traducciones | Nombres de objetos/bloques de los assets oficiales 26.3. Derechos de Mojang/Microsoft; origen y hashes en `verification/language-sources.json`. No se distribuyen los binarios del juego. |
| Fabric Loader / API / Loom | Herramientas y APIs de [Fabric](https://github.com/FabricMC). Conservan sus propias licencias. Fabric API se instala por separado. |
| Vosk 0.3.45 | Motor offline Apache-2.0 de [Alpha Cephei](https://github.com/alphacep/vosk-api). JAR anidado con bibliotecas nativas; conserva sus avisos originales. |
| JNA 5.18.1 | [Java Native Access](https://github.com/java-native-access/jna), licencia dual LGPL-2.1-or-later/Apache-2.0. JAR anidado con avisos originales. |
| Modelos Vosk | Descarga separada, no incluida en JAR/ZIP. Los modelos pequeños es-0.42, de-0.15 y ja-0.22 se publican bajo Apache-2.0 en la [lista oficial](https://alphacephei.com/vosk/models). |
| Lecturas japonesas | Datos derivados mediante [SudachiPy](https://github.com/WorksApplications/sudachi.rs/tree/develop/python) 0.6.10 y [SudachiDict-core](https://github.com/WorksApplications/SudachiDict) 20250515. El diccionario completo no se distribuye; se incluyen resultados, modificaciones y scripts. |

Los avisos de SudachiDict y sus fuentes (incluidos UniDic y NEologd) se conservan en [licenses/SudachiDict-LEGAL.txt](licenses/SudachiDict-LEGAL.txt), junto a [Apache-2.0](licenses/Apache-2.0.txt). Las modificaciones consisten en seleccionar nombres de Minecraft, corregir lecturas, separar palabras y convertir kana a caracteres latinos. Esos avisos también se incluyen en el JAR.

Las DLL de soporte presentes en el paquete Vosk proceden de su distribución oficial; no se modificaron. La distribución binaria de Gradle no se publica en este repositorio: el Wrapper la descarga con su checksum fijado.

**No es un producto oficial de Minecraft. No está aprobado ni asociado con Mojang o Microsoft.** Las marcas pertenecen a sus titulares. No se incluye una licencia de acceso al juego.
