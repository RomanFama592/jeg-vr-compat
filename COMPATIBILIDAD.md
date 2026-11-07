# Compatibilidad con mods de optimización y shaders

Estado al 2026-11-07, con JEG 0.13.2, Vivecraft 1.20.1-1.2.5 y Forge 47.2.0. Nada de esto se probó todavía con el visor
puesto. "Mixins OK" significa que todos los mixins aplicaron juntos sin errores, no que se vea bien en VR.

## Qué se comprobó

| Combinación | Cómo | Resultado |
|---|---|---|
| Embeddium 0.3.31 + Oculus 1.8.0 + Vivecraft + JEG + addon | `runClientAudit`, en plano y en entorno de desarrollo: aplica todos los mixins de todos los mods y cierra el juego | **Mixins OK**: 43 aplicaciones del addon, audit completo, sin errores de inyección |
| Nombres que usan los mixins `remap = false` | `tools/check_mixins.py` contra los jars originales de JEG, Vivecraft, GeckoLib y Framework | **0 problemas** tras corregir `renderByItem` |
| Rangos de versión de `mods.toml` | El juego arrancó con los rangos nuevos | Aceptan JEG 0.13.2 y Vivecraft 1.20.1-1.2.5 |

## Cómo se relacionan estos mods con el addon

Revisado desmontando los jars con `javap`.

| Pieza | Qué hace | Efecto en el addon |
|---|---|---|
| Vivecraft | Reconoce `oculus`, `iris`, `embeddium`, `rubidium` y `sodium`, y trae mixins propios para Iris, Sodium, OptiFine y EMF | La integración base con shaders la resuelve Vivecraft |
| Vivecraft + Oculus | En VR cancela las pasadas de manos de Iris (`HandRenderer.renderSolid` y `renderTranslucent`) y dibuja con `VRArmHelper` | Nuestros mixins de manos siguen corriendo con shaders |
| Embeddium `ItemRendererMixin` | Dibuja ítems horneados por su vía rápida si el buffer es compatible; si no, avisa y usa la vía normal | Las armas clásicas espejadas van por la vía normal. Puede salir un aviso `logBadConsumer` en el log; no es un error |
| Oculus `FullyBufferedMultiBufferSource` | `endBatch(RenderType)` no hace nada: el dibujo se acumula hasta el siguiente `endBatch()` | El láser y las guías se dibujan un poco más tarde. `VrGunHud` usa `endBatch()`, que sí vacía, así que su truco de profundidad sigue valiendo |
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
| Fase 10 (sin hacer) | Sprites y brillo sobre el arma | Ver la nota de F10.4 en `PLAN.md` | — |

El addon no trae shaders ni `RenderType` propios, y no importa clases de Sodium, Iris ni OptiFine. Para saber si hay
shaders usa `VR.shadersActive()`, que le pregunta a Vivecraft. Eso evita depender de esos mods.

## Otros mods habituales

Estos **no se probaron** porque sus jars no estaban disponibles. La columna "Riesgo" es lo que se espera por lo que tocan.

| Mods | Riesgo | Por qué |
|---|---|---|
| FerriteCore, Smooth Boot, Krypton, Radium/Canary, Starlight, C2ME | Muy bajo | No tocan el dibujo de manos ni la lógica de JEG que interceptamos |
| Entity Culling, Sodium Extra / Embeddium Extra | Bajo | Por encima del dibujo de armas; podrían afectar qué entidades se ven |
| Dynamic FPS | Bajo | Vivecraft trae su propio mixin para este mod |
| Entity Model Features / Texture Features | Bajo | Vivecraft trae su propio mixin para EMF; revisar el brazo en tercera persona |
| Distant Horizons | Medio | Vivecraft y Oculus tienen integración, pero depende de sus versiones; no toca al addon directamente |
| ImmediatelyFast | Medio | Cambia `BufferSource`, y usamos `endBatch` y un buffer envuelto |
| Accelerated Rendering y similares | Medio-alto | Reemplazan el camino de vértices, justo donde está el espejo |
| OptiFine | Alto | Vivecraft lo admite, pero Oculus es otra vía. GeckoLib con OptiFine suele dar problemas. Se recomienda no anunciarlo como soportado |

## Protocolo de prueba (lo hace el usuario, con visor)

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

`-Pcompat` agrega Embeddium y Oculus desde Modrinth Maven. La auditoría del 2026-11-07 usó en su lugar copias locales en
`run-audit/mods/`, tomadas de la caché de Gradle. Esas copias siguen ahí, así que para probar sin esos mods hay que
sacarlas de esa carpeta.

Para revisar los nombres en producción (ajustar las rutas a tus jars):

```bash
python tools/check_mixins.py <ruta a javap> "libs/JustEnoughGuns-0.13.2-1.20.1.jar;<vivecraft.jar>;<geckolib.jar>;libs/framework-forge-1.20.1-0.7.11.jar"
```
