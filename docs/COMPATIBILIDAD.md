# Compatibilidad con mods de optimización y shaders

Estado al 2026-11-07, con JEG 0.13.2 y Vivecraft 1.20.1-1.2.5. "Mixins OK" significa que todos los mixins aplicaron juntos
sin errores, no que se vea bien en VR. Las versiones de JEG y Vivecraft que acepta el addon están en
[Versiones de JEG y Vivecraft](#versiones-de-jeg-y-vivecraft), al final.

Las pruebas con visor se hicieron con unas **Meta Quest 3 a través de SteamVR**. Otros visores y runtimes no están
probados.

## Problemas conocidos

| Problema | Causa | Qué hacer |
|---|---|---|
| **Acedium 0.2.7-beta + Oculus: el juego crashea al apagar y volver a prender los shaders en un mundo** (`IllegalStateException` en `SodiumResultCompatibility.packageSectionGeometry`) | Acedium comprueba que el tamaño de la malla de cada chunk coincida con el formato de vértices que espera, y falla. El addon no aparece en la traza. Lo más probable es que el cambio de formato al recargar los shaders sea lo que dispara esa comprobación | No cambiar los shaders dentro de un mundo con Acedium instalado: elegirlos en el menú principal, o sacar Acedium. Sin Acedium no crashea (probado por el usuario). Sin confirmar si pasa también sin el addon |

## Qué se comprobó

| Combinación | Cómo | Resultado |
|---|---|---|
| Modpack real del usuario, **con el `.jar` publicable y con visor**: Forge 47.4.4, JEG 0.13.2, Vivecraft 1.2.5, Embeddium 0.3.31, Oculus 1.8.0, ImmediatelyFast 1.5.1, EntityCulling 1.8.1, Create 6.0.6, GeckoLib 4.7.4, Framework 0.7.15, Acedium 0.2.7, Complementary Reimagined r5.2.2 + Euphoria Patches 1.3.2 | Prueba del usuario en juego | Anduvo muy bien, salvo el crash de arriba. Las cajas guía desfasadas entre ojos se arreglaron y el usuario lo confirmó, igual que el láser del arco. Falta el protocolo completo de abajo |
| Cliente de desarrollo **con visor**: Embeddium 0.3.31, Oculus 1.8.0, BSL 10.1.8 y Complementary Reimagined r5.9.3 | Protocolo de abajo, prueba del usuario (2026-11-09) | **Nada visual raro** con los dos packs. Log sin errores del addon; solo avisos de los packs (Complementary r5.9.3 nombra biomas de versiones más nuevas) y dos errores sueltos de Vivecraft al recargar los shaders |
| Embeddium 0.3.31 + Oculus 1.8.0 + Vivecraft + JEG + addon | `runClientAudit`, en plano y en entorno de desarrollo: aplica todos los mixins de todos los mods y cierra el juego | **Mixins OK**: 43 aplicaciones del addon, audit completo, sin errores de inyección |
| Nombres que usan los mixins `remap = false` | `./gradlew checkMixins`, contra los jars originales de JEG, Vivecraft, GeckoLib y Framework | **0 problemas** tras corregir `renderByItem` |
| Rangos de versión de `mods.toml` | El juego arrancó con los rangos nuevos | Aceptan JEG 0.13.2 y Vivecraft 1.20.1-1.2.5 |

## Cómo se relacionan estos mods con el addon

Revisado desmontando los jars con `javap`.

| Pieza | Qué hace | Efecto en el addon |
|---|---|---|
| Vivecraft | Reconoce `oculus`, `iris`, `embeddium`, `rubidium` y `sodium`, y trae mixins propios para Iris, Sodium, OptiFine y EMF | La integración base con shaders la resuelve Vivecraft |
| Vivecraft + Oculus | En VR cancela las pasadas de manos de Iris (`HandRenderer.renderSolid` y `renderTranslucent`) y dibuja con `VRArmHelper` | Nuestros mixins de manos siguen corriendo con shaders |
| Embeddium `ItemRendererMixin` | Dibuja ítems horneados por su vía rápida si el buffer es compatible; si no, avisa y usa la vía normal | Las armas clásicas espejadas van por la vía normal. Puede salir un aviso `logBadConsumer` en el log; no es un error |
| Oculus `FullyBufferedMultiBufferSource` | `endBatch(RenderType)` no hace nada: el dibujo se acumula hasta el siguiente `endBatch()` | Si no hay otro vaciado, lo acumulado se dibuja en la pasada del otro ojo y se ve desfasado. **El addon siempre usa `endBatch()`** para lo que dibuja en el mundo |
| Oculus `MixinItemRenderer` | Solo marca qué ítem se dibuja, al inicio y al final de `render` | No choca con nuestro `@WrapOperation` |
| Vivecraft y Oculus en `ItemInHandRenderer` e `ItemInHandLayer` | Tocan otros puntos de los mismos métodos | Aplican juntos. Los nuestros cosméticos llevan `require = 0` para no crashear si otro mod los pisa |

## Lo que el addon dibuja y cómo se comporta con shaders

| Elemento | Técnica | Con shaders | Falta comprobar en VR |
|---|---|---|---|
| Armas y manos | Las dibuja JEG o Vivecraft | Pasan por el pack | Que se vean en las dos manos |
| Arma espejada (mano izquierda) | Buffer que invierte el orden de cada cara, sin shader propio | Sin riesgo previsto | Caras e iluminación correctas |
| Paneles de munición | `RenderType.text` con luz máxima y `glDepthRange` | El rango se restaura siempre | Que queden por encima del arma y no generen halos |
| Láser del arco | `debugQuads`; **con shaders, `RenderType.lightning`** | Los `debugQuads` usan el programa básico del pack y podrían oscurecerse de noche | Que se vea de noche; con shaders el aspecto puede ser más brillante |
| Cajas guía | `debugFilledBox` y `lines` | Solo depuración, apagadas por defecto | — |
| Brillos de recarga (F10.1) | Cuadrados que miran al visor, `RenderType.entityTranslucentEmissive` con texturas propias, luz máxima y sin escribir profundidad | Pasan por el programa de entidades del pack | Que se vean igual con BSL y Complementary, de día y de noche |
| Caja de munición (F10.2) | El modelo del bloque `jeg:ammo_box` con `Sheets.cutoutBlockSheet()` y la luz del lugar | Pasa por el programa de entidades del pack | Que se vea con su textura y su luz, de día y de noche |
| Mano fantasma del agarre (F10.4) | El modelo de la mano de Vivecraft, dibujado otra vez en `entityTranslucent` con la piel del jugador, más claro y transparente | Pasa por el programa de entidades del pack | Que se vea transparente con BSL y Complementary |
| Partículas del agarre (F10.4) | Partículas de polvo vanilla (`DustParticleOptions`) | Las dibuja el pack como cualquier partícula | Que se vean suaves con y sin shaders |
| Resto de la Fase 10 (sin hacer) | Munición en la mano | Pendiente | — |

El addon no trae shaders ni `RenderType` propios, y no importa clases de Sodium, Iris ni OptiFine. Para saber si hay
shaders usa `VR.shadersActive()`, que le pregunta a Vivecraft. Eso evita depender de esos mods.

## Otros mods habituales

Estos **no se probaron** porque sus jars no estaban disponibles. La columna "Riesgo" es lo que se espera por lo que tocan.

| Mods | Riesgo | Por qué |
|---|---|---|
| FerriteCore, Smooth Boot, Krypton, Radium/Canary, Starlight, C2ME | Muy bajo | No tocan el dibujo de manos ni la lógica de JEG que interceptamos |
| Sodium Extra / Embeddium Extra | Bajo | Por encima del dibujo de armas |
| Dynamic FPS | Bajo | Vivecraft trae su propio mixin para este mod |
| Entity Model Features / Texture Features | Bajo | Vivecraft trae su propio mixin para EMF; revisar el brazo en tercera persona |
| Distant Horizons | Medio | Vivecraft y Oculus tienen integración, pero depende de sus versiones; no toca al addon directamente |
| Accelerated Rendering y similares | Medio-alto | Reemplazan el camino de vértices, justo donde está el espejo |
| OptiFine | Alto | **Se anuncia como no probado** (decisión del usuario, 2026-11-08). Vivecraft lo admite, pero Oculus es otra vía y GeckoLib con OptiFine suele dar problemas |

## Protocolo de prueba (lo hace el usuario, con visor)

Kit en `compat-test/` (ignorado por git, bajado de Modrinth con el hash verificado): `mods/` con Embeddium 0.3.31 y
Oculus 1.8.0 (las versiones de `-Pcompat`), y `shaderpacks/` con BSL 10.1.8 y Complementary Reimagined r5.9.3.

1. Base: Embeddium, sin shaders. Mirar arma en cada mano, mano izquierda espejada, paneles, recarga con la mano.
2. Embeddium + Oculus, con un pack liviano (BSL) y uno pesado (Complementary Reimagined).
3. En cada pack, de día y de noche: armas animadas y clásicas, destello, retroceso, láser del arco, paneles, hotbar en el
   antebrazo, tercera persona con la cámara de Vivecraft.
4. Comparar los FPS con y sin el addon, con un arma en cada mano.
5. Revisar el log: ni errores repetidos ni avisos por cada cuadro.

Cada resultado se anota en la tabla "Tested with" de la página del mod. Lo que no se probó no se anuncia.

## Comandos

```bash
./gradlew runClientAudit
```

```bash
./gradlew runClientAudit -Pcompat
```

`-Pcompat` agrega Embeddium y Oculus desde Modrinth Maven.

Para revisar los nombres en producción (necesita Python):

```bash
./gradlew checkMixins
```

## Versiones de JEG y Vivecraft

Desde la Fase M (2026-11-11) un mismo jar sirve para dos líneas de cada mod. `mods.toml` acepta **JEG `[0.13.2,0.15)`** y
**Vivecraft `[1.20.1-1.2.5,1.20.1-1.4)`**; con otra versión, Forge muestra su pantalla de error en lugar de crashear en
medio de una partida.

| | Vivecraft 1.2.5 | Vivecraft 1.3.x |
|---|---|---|
| **JEG 0.13.2** | Jugado en VR (es la combinación de las versiones 0.1 y 0.2 del addon) | Automático; falta jugarlo en VR |
| **JEG 0.14.x** | Automático; falta jugarlo en VR | Automático; falta jugarlo en VR |

"Automático" es todo esto, sin visor:

| Qué | Cómo | Resultado (2026-11-11) |
|---|---|---|
| El código compila contra las cuatro combinaciones | `./gradlew checkMatrix` (JEG 0.13.2 y 0.14.5, Vivecraft 1.2.5 y 1.3.15) | Compila |
| Los nombres que usan los mixins existen en los jars que tienen los jugadores | `./gradlew checkMixins`, en las cuatro combinaciones | 0 problemas |
| Lo mismo contra las versiones intermedias | `checkMatrix` con `-Pvivecraft_version_latest` (1.3.0, 1.3.2 a 1.3.9 y 1.3.13) y `-Pjeg_version_latest` (0.14.0 a 0.14.4) | Las 15 compilan, con 0 problemas |
| Cada mixin se aplica en el juego | `./gradlew runClientAudit` con cada combinación (`-PlatestJeg`, `-PlatestVivecraft`, `-Platest`), y `tools/check_audit.py`: ningún enganche queda sin su punto de inyección | Las cuatro: sin errores y sin enganches sueltos. También con los extremos de cada línea (Vivecraft 1.3.0, JEG 0.14.0) y con Embeddium + Oculus sobre las versiones nuevas |
| Se elige el código de la línea correcta | La línea `Detected JEG ... / Vivecraft ... (vc12 o vc13)` del log | Correcta en las cuatro |

Lo que cambia entre las líneas, para quien lea un reporte:

- **Vivecraft 1.3** reorganizó la interacción: la hotbar que se toca con la mano es un módulo
  (`InteractiveHotbarModule`), los trackers procesan en `activeProcess`, y trae una API oficial (`org.vivecraft.api`). El
  addon usa esa API para saber si hay VR, quién juega en VR, la vibración, el modo sentado, la mano hábil, la escala de
  mundo y qué vista se dibuja. Las poses de los controles salen de los mismos internos en las dos líneas, para que un
  arma quede exactamente igual en una que en otra.
- **JEG 0.14** agrega la motosierra y el martillo neumático (minan en lugar de disparar), el Libro de Salmos, el lanzador
  bajo el cañón, el disparo desde una montura y las cajas de suministros, y calienta las armas por disparo en lugar de
  por tiempo. El addon sigue esas reglas en la mano secundaria; las mecánicas nuevas funcionan como en JEG, sin gestos de
  VR propios todavía.
- Al cambiar de Vivecraft 1.2 a 1.3 con la misma carpeta de configuración, Vivecraft anota un error al leer una opción
  vieja (`WeaponCollision`). Es de Vivecraft y no afecta al addon.

