SpeakCraft Survival retiene cada objeto nuevo hasta que pronunciás su nombre. Primera versión pública del prototipo, con reconocimiento offline y ayuda de lectura japonesa ampliada.

### Descargas

- **SpeakCraftSurvival-0.2.0-windows.zip**: JAR, script para modelos y documentación. Extraer antes de instalar.
- **speakcraft-survival-0.2.0.jar**: solo el mod; colocar en `mods` junto a Fabric API.
- **SHA256SUMS.txt**: huellas de ambos archivos.

Los archivos automáticos **Source code** contienen fuentes, no el mod listo para jugar. Los modelos Vosk se instalan por separado.

### Versiones y requisitos

**Minecraft Java 26.3 · Java 25 x64 · Fabric Loader ≥0.19.5 compatible con 26.3 · Fabric API 0.165.0+26.3 · Windows 10/11 x64.** No es compatible con Bedrock, Forge/NeoForge ni snapshots 26.4. Otras plataformas y servidores públicos no están verificados.

### Novedades

- Lecturas en letras latinas para 2.777 claves japonesas, incluidos nombres y variantes.
- Ajuste de líneas para leer ayudas largas y guía accesible desde F8 con opción Español.
- Instalación documentada de español, alemán y japonés; cada idioma requiere su modelo.
- Documentación para agregar idiomas, corregir lecturas y reportar problemas.

### Instalar o actualizar

Con Minecraft cerrado, quitá el JAR anterior de SpeakCraft de `mods` y colocá el nuevo. Conservá mundos y `config/speakcraft`. No dejes ambas versiones instaladas. Abrí el perfil Fabric y usá F8 → Probar voz. [Instrucciones completas](https://github.com/TomiDuarte/speakcraft-survival/blob/main/INSTRUCCIONES_INSTALACION.md).

### Pruebas y límites

Build exitoso, 29 JUnit, 15 GameTests y 6 pruebas de romanización aprobados; configuración y guía verificadas en el cliente. [Informe](https://github.com/TomiDuarte/speakcraft-survival/blob/main/VERIFICACION.md).

Prototipo experimental: respaldá tus mundos. La mecánica elimina intencionalmente las adquisiciones retenidas cuyo desafío vence. La ayuda japonesa es orientativa y puede necesitar correcciones; la similitud textual no puntúa calidad fonética. Cofres, hornos e intercambios no están integrados. No es un antitrampas para servidores públicos.

No es un producto oficial de Minecraft ni está aprobado o asociado con Mojang/Microsoft. Código MIT; dependencias, modelos y traducciones conservan sus licencias y derechos.
