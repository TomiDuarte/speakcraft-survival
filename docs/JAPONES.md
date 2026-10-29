# Leer japonés sin conocer los kanjis

Activá **F8 → Lectura / Reading hints**. Debajo del nombre japonés aparece **Romaji**, una representación en letras latinas. Las lecturas largas ocupan varias líneas. **Cómo leer japonés / Read Japanese → Español** abre una guía dentro del juego, aunque los menús estén en inglés.

| Nombre | Lectura | Significado |
|---|---|---|
| オークの原木 | ooku no genboku | Tronco de roble |
| オークの板材 | ooku no itazai | Tablas de roble |
| 作業台 | sagyoudai | Mesa de trabajo |
| 木のツルハシ | ki no tsuruhashi | Pico de madera |
| 鉄インゴット | tetsu ingotto | Lingote de hierro |
| 土 | tsuchi | Tierra |
| 石 | ishi | Piedra |
| 松明 | taimatsu | Antorcha |

## Orientación para hispanohablantes

- Las vocales **a, e, i, o, u** son relativamente cercanas a las españolas. No leas las palabras como inglés.
- **oo**, **ou** y **uu** señalan vocales largas en esta guía: sostené el sonido, sin agregar una pausa. `ooku` empieza con una o larga. Hay excepciones según la palabra; esto es una orientación inicial.
- **sh** se parece al sonido de pedir silencio. **ch** como en «chico». **ts** combina t y s, como en `tsuchi`.
- **h** es aspirada; no es muda como en español. **j** no es la jota española: se aproxima a una y suave, sin el sonido «sh» del español rioplatense.
- **r** es breve, parecida a la de «caro», no la rr de «carro». **g** mantiene sonido de g, incluso ante e/i.
- Una consonante doble marca una pequeña retención antes de soltarla: `ingotto`. El apóstrofo separa una n silábica de la vocal siguiente.
- Los espacios facilitan leer; no hace falta hacer pausas entre cada fragmento.

Decí el **nombre japonés**, no su significado español. El romaji no se acepta como texto alternativo: Vosk debe transcribir japonés. La tolerancia no mide calidad fonética. Para escuchar pronunciación de hablantes, podés usar los recursos gratuitos [Irodori de The Japan Foundation](https://www.irodori.jpf.go.jp/en/starter/pdf.html).

## De dónde salen las lecturas

`src/main/resources/data/speakcraft/readings/ja.json` contiene 2.777 entradas precomputadas sobre las traducciones oficiales de Minecraft 26.3. Incluye nombres de bloques, objetos y variantes; no son 2.777 objetos distintos. Se generaron con **SudachiPy 0.6.10 / SudachiDict-core 20250515**, conversión de kana a romaji y ajustes en `scripts/readings/overrides.json`.

No se ejecuta un diccionario lingüístico en cada tick ni se agrega una descarga al abrir el juego. El JAR incluye solo las lecturas. Si cambia el nombre oficial, una lectura antigua deja de mostrarse para evitar asociaciones equivocadas.

Es una ayuda orientativa, no una revisión profesional de todo el catálogo. Los nombres compuestos o inventados pueden requerir correcciones. Se omiten kanjis fuera del diccionario y cadenas con parámetros; [coverage.json](../scripts/readings/coverage.json) enumera las omisiones. No se transforma un kanji desconocido usando su pronunciación china.

Reportá errores con el Registry ID, nombre japonés, lectura mostrada y lectura propuesta con una fuente o explicación. [Cómo contribuir](../CONTRIBUTING.md).
