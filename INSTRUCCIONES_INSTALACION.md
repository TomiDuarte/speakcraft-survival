# Instalación de SpeakCraft Survival 0.2.0

## 1. Preparar Minecraft

Necesitás **Minecraft Java Edition 26.3**, Java **25 x64**, Fabric Loader **0.19.5 o posterior compatible** y Fabric API **0.165.0+26.3**. Soporte inicial: Windows 10/11 x64. Otras plataformas no están verificadas. Bedrock, Forge/NeoForge y otras versiones del juego no son compatibles.

Instalá [Minecraft Launcher](https://www.minecraft.net/download), iniciá sesión con acceso a Java Edition y abrí **26.3** hasta el menú principal. Después cerrá el juego y el launcher.

Instalá [Fabric](https://fabricmc.net/use/installer/), pestaña **Client**, versión **26.3**, con **Create Profile** activado. La carpeta habitual del launcher es `%APPDATA%\.minecraft`.

En el launcher elegí **fabric-loader-26.3** en el selector inferior izquierdo, junto a Jugar. Si no aparece, cerrá y reabrí el launcher y revisá **Instalaciones → Modificadas / Modded**. Si Fabric no encuentra un directorio del lanzador, abrí Java Edition una vez antes de instalarlo; una carpeta vacía no sirve.

## 2. Instalar el mod

Descargá `SpeakCraftSurvival-0.2.0-windows.zip` o `speakcraft-survival-0.2.0.jar` desde [Releases](https://github.com/TomiDuarte/speakcraft-survival/releases). Extraé el ZIP antes de usarlo.

Con el juego cerrado, colocá en `mods` de la instancia:

```text
mods/
  speakcraft-survival-0.2.0.jar
  fabric-api-0.165.0+26.3.jar
```

[Descarga de Fabric API](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.165.0+26.3/fabric-api-0.165.0+26.3.jar). Fabric Loader y Fabric API son componentes distintos; se necesitan ambos.

No copies `-sources.jar`, el código fuente, modelos ni el ZIP completo a `mods`. Para actualizar, sacá el JAR anterior de SpeakCraft y conservá `config/speakcraft` y los mundos. No instales dos versiones juntas.

Vosk y JNA con las DLL Windows x64 vienen dentro del mod. No necesitás Python para jugar. Las DLL se extraen temporalmente; Windows debe permitir escribir en su carpeta temporal.

## 3. Instalar modelos de voz

Desde la carpeta extraída, abrí PowerShell:

```powershell
# Español
powershell -ExecutionPolicy Bypass -File .\scripts\Install-VoskModels.ps1 `
  -GameDirectory "$env:APPDATA\.minecraft" -Languages es

# Japonés
powershell -ExecutionPolicy Bypass -File .\scripts\Install-VoskModels.ps1 `
  -GameDirectory "$env:APPDATA\.minecraft" -Languages ja

# Alemán
powershell -ExecutionPolicy Bypass -File .\scripts\Install-VoskModels.ps1 `
  -GameDirectory "$env:APPDATA\.minecraft" -Languages de
```

Sin `-Languages`, instala los tres. Descarga explícitamente desde `alphacephei.com`, conserva ZIP en `.install-*`, registra SHA256 y no reemplaza modelos existentes. La huella local no es una firma del editor. El mod no descarga modelos en segundo plano.

Si tu perfil tiene un **Directorio del juego** personalizado, usá esa ruta en `-GameDirectory` y colocá allí los mods. Prism, Modrinth y otros launchers suelen usar carpetas por instancia. La carpeta del instalador Fabric y la de una instancia personalizada no necesariamente coinciden.

También podés extraer los [modelos oficiales](https://alphacephei.com/vosk/models) manualmente:

| Código | Modelo |
|---|---|
| `es` | `vosk-model-small-es-0.42` |
| `de` | `vosk-model-small-de-0.15` |
| `ja` | `vosk-model-small-ja-0.22` |

Estructura final, sin otra carpeta intermedia:

```text
<instancia>/config/speakcraft/models/
  es/am/final.mdl
  es/conf/model.conf
  es/graph/...
  ja/am/final.mdl
  ja/conf/model.conf
  ja/graph/...
  de/am/final.mdl
  de/conf/model.conf
  de/graph/...
```

Instalá solo los idiomas que uses. Seleccionar `ja` sin `models/ja` produce **Falta el modelo Vosk**; instalalo y repetí **Probar voz**. Normalmente no hace falta reiniciar tras instalar un modelo.

## 4. Micrófono y primer desafío

1. Habilitá en Windows el acceso al micrófono para aplicaciones de escritorio.
2. Abrí Minecraft con el perfil **Fabric 26.3**.
3. Presioná **F8**; algunos teclados requieren **Fn + F8**. Podés reasignarla en los controles.
4. Elegí `es`, `de` o `ja` y el micrófono. **Predeterminado del sistema** usa el dispositivo de Windows.
5. Pulsá **Probar voz / Test voice**. La prueba dura 15 segundos y se detiene al cerrar la pantalla.
6. Para japonés, activá **Lectura / Reading hints** y abrí **Cómo leer japonés / Read Japanese → Español**.
7. Pulsá **Guardar y reanudar / Save and resume**, entrá a supervivencia y recogé un objeto nuevo.

El dispositivo debe admitir PCM de 16 kHz, 16 bits, mono. Si falla, probá otro dispositivo. El temporizador del desafío comienza después de preparar modelo y micrófono. Los errores técnicos suspenden y conservan los objetos retenidos.

La ayuda latina orienta cómo leer japonés; no digas la traducción española. La tolerancia compara texto, no calidad fonética. [Guía](docs/JAPONES.md).

## Problemas frecuentes y progreso

- **F8 no hace nada**: verificá perfil Fabric, carpeta del JAR y asignación de tecla.
- **Falta el modelo**: instalá el del código elegido, sin carpetas anidadas adicionales.
- **Error de micrófono**: revisá permisos/dispositivo y repetí la prueba.
- **Objeto retenido**: corregí el error y usá Guardar y reanudar. Si acertaste pero no hay espacio, liberá un casillero.
- **Lectura no disponible**: esa entrada no tiene lectura utilizable; ver `CONTRIBUTING.md`.
- **Error al iniciar**: revisá versiones y JAR duplicados. Reportá el error relevante de `logs/latest.log`, quitando datos personales.

Progreso, idioma por mundo y objetos retenidos se guardan con el jugador. `client.json` guarda preferencias locales. `/speakcraft progress` y `/speakcraft reset` requieren permisos de Game Master; no hacen falta para jugar. Reset conserva pendientes y reinicia desbloqueos/contadores.

## Compilar

Con JDK 25 y `JAVA_HOME` configurado, desde el repositorio:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

La primera compilación necesita Internet. El reconocimiento es offline después de descargar dependencias y modelos. `runClient` usa la instancia `run`, separada del launcher; instalá allí los modelos para desarrollar.
