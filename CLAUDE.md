# JEG x Vivecraft — Addon de compatibilidad VR

Addon de Forge 1.20.1 (`jeg_vr_compat`) que hace compatible **Just Enough Guns** (0.13.2 y 0.14.x,
https://github.com/MigaMiDev/Just-Enough-Guns) con **Vivecraft** (1.2.5 y 1.3.x) sin modificar ninguno de los dos.
Un mismo jar sirve para las cuatro combinaciones (Fase M).
El plan de trabajo detallado está en `notas/PLAN.md` (privado: fuera de git, junto con `notas/AUDITORIA.md` y `notas/PUBLICACION.md`). Leelo antes de empezar y marcá las tareas a medida que se completen.

## Stack
- Minecraft 1.20.1, Forge 47.2.x, ForgeGradle 6, Java 17, mappings oficiales (Mojang).
- JEG **0.13.2** y **0.14.5** (paquete raíz `ttv.migami.jeg`), usa GeckoLib `4.4.4` para armas animadas. `main` se compila
  contra 0.13.2; lo que solo existe en 0.14 se reconoce por nombre (`compat/jeg/JegItems`) o por versión (`compat/Versions`).
- Vivecraft **1.20.1-1.2.5-forge** y **1.20.1-1.3.15-forge**. 1.2 solo trae `org.vivecraft.api_beta` (`VivecraftAPI.isVRPlayer`, `VivecraftClientAPI.isVrActive`); 1.3 trae la API oficial `org.vivecraft.api` (`VRAPI`, `VRClientAPI`, `VRRenderingAPI`; `RenderPass` pasó a `org.vivecraft.api.client.data`). El resto se toma de internals que no cambiaron entre las dos (`VRPlayer.get().vrdata_world_render`: `c0` mano principal, `c1` libre, `hmd`; `MCVR.get().triggerHapticPulse`; `ClientDataHolderVR.getInstance().vrSettings.reverseHands`). Los bridges los llaman directo (Vivecraft es `compileOnly` + `runtimeOnly`), sin reflexión; `VR` captura `Throwable` y se desactiva si algo falla.
- Tres source sets en un jar: `main` (compila contra las cuatro combinaciones), `src/vc12` (contra Vivecraft 1.2.5) y `src/vc13` (contra 1.3.15). `compat/Versions` detecta qué hay instalado sin cargar clases, y de ahí salen el bridge y los mixins.
- ForgeGradle solo desofusca **una** versión de cada mod por build (dos se pisan en su configuración interna de originales). La otra versión se usa tal como se publica (`jegDependency`/`vivecraftDependency` en `build.gradle`): sus nombres propios y los nombres de clase del juego son los mismos, que es lo que el compilador necesita.
- JEG requiere además **MrCrayfish Framework >= 0.7.0** (obligatorio) y GeckoLib >= 4.4.4. Vienen de repositorios públicos, sin archivos locales: JEG de Modrinth Maven (`maven.modrinth:just-enough-guns`, el mismo jar que publica Modrinth) y Framework de Curse Maven (`curse.maven:framework-549225:<id de archivo>`, porque no está en Modrinth). Se compila y se prueba con Framework 0.7.12. Las versiones están en `gradle.properties`.
- Toolchain local (no instalado en el sistema): JDK 17 en `.tools/jdk-17*`, Gradle wrapper 8.1.1. Usar `JAVA_HOME=.tools/jdk-17.0.20.1+1`.
- Mixin (SpongePowered) + MixinExtras 0.3.5 (`@WrapWithCondition`, `@WrapOperation`).
- Todas las dependencias de mods son `compileOnly` + `fg.deobf(...)`. Nunca `implementation` salvo MixinExtras (jarJar).

## Reglas de arquitectura (no negociables)
1. **Cliente primero, servidor opcional** (Fase S, aprobada por el usuario). Todo debe funcionar en un servidor
   que solo tiene JEG, reutilizando `C2SMessageReload`, `C2SMessageShoot`, etc. Las mejoras de servidor van por el
   canal propio `jeg_vr_compat:main` (acepta ausencia en ambos lados) y el cliente solo lo usa si
   `VrAim.serverHasAddon()` (`VrNetwork.isPresent(connection)`). Código de servidor en `com.jegvr.server` y mixins en `mixin.server` (lista
   `mixins` del json, solo requieren `jeg`; nunca importan clases de cliente ni de Vivecraft).
2. **Flat intacto.** Toda intervención está detrás de `VR.active()` (jugador local) o `VR.isVRPlayer(p)`
   (jugadores remotos). Con VR apagado o Vivecraft ausente, el comportamiento debe ser idéntico al de JEG.
3. **Vivecraft opcional.** Fuera de los mixins (que `JegVrMixinPlugin` condiciona), ninguna clase salvo las de
   `compat/vivecraft/` puede importar `org.vivecraft.*`. Lo que se le pide a un mod de VR está en
   la interfaz `compat/VrBackend`, y el resto del addon usa `VR`, que comprueba `ModList.isLoaded("vivecraft")` antes de
   crear el backend. `VR` nombra el bridge (`Versions.vivecraft().bridgeClass()`) y lo crea por reflexión: nunca lo
   referencia, para que no se cargue el de la otra línea. Una línea nueva de Vivecraft es otro bridge y otro source set.
4. **Mixins condicionales.** `JegVrMixinPlugin.shouldApplyMixin` devuelve false si falta `jeg` o `vivecraft`, y para los
   mixins de `mixin.vc12` / `mixin.vc13` cuando el Vivecraft instalado es de la otra línea.
5. Mixins sobre clases de JEG: `@Mixin(value = X.class, remap = false)`; los `@At(INVOKE)` que apuntan a
   métodos vanilla llevan `remap = true`.
6. No esconder bones de GeckoLib para quitar brazos: los hijos (cargador en mano, balas) deben seguir visibles.
   Se suprime solo el dibujo de `ModelPart.render` del modelo del jugador.
7. Nada de rotar cámara/jugador en VR (mareo). El recoil va al modelo del arma, haptics y desviación de puntería.
8. **Nombres de mixin en producción.** En un mixin `remap = false`, un método de JEG que sobrescribe uno vanilla se nombra
   con los dos nombres (`{"renderByItem", "m_108829_"}`). Después de tocar mixins, correr `./gradlew checkMixins`.
9. **Otros mods y shaders.** Lo que solo cambia cómo se ve lleva `require = 0`. Nada de shaders ni `RenderType` propios sin
   revisar `docs/COMPATIBILIDAD.md`. Para saber si hay shaders, `VR.shadersActive()`; nunca importar Sodium, Iris ni OptiFine. Lo que se dibuja en el mundo se vacía con `buffers.endBatch()`, nunca con `endBatch(RenderType)`: con Oculus esa no hace nada y el dibujo sale en la pasada del otro ojo.
10. **Contrato entre versiones del addon.** Red: un ID de paquete solo se agrega y un paquete no cambia de formato; lo nuevo
    sube `VrNetwork.PROTOCOL` y se envía según `serverProtocol`. Configuración: el TOML solo suma opciones, con el
    comportamiento actual por defecto; `guns.json` lleva `schema`. No se renombran teclas `key.jeg_vr_compat.*` ni
    subcomandos de `/jegvr`. Datos propios en ítems: bajo un sub-compuesto `jeg_vr_compat`. Mejora nueva: con su
    interruptor. Textos para el jugador: en `en_us` y `es_es`. Claves y constantes de JEG: en `compat/jeg/JegKeys`.
11. **Dos versiones de cada mod.** `main` tiene que compilar contra las cuatro combinaciones: no puede nombrar una clase,
    un campo ni un método que solo exista en una versión. Lo exclusivo de una línea de Vivecraft va en `src/vc12` o
    `src/vc13` (un bridge, o un mixin en `mixin.vc12` / `mixin.vc13`, sin `@At` a métodos vanilla: esos source sets no
    tienen refmap). Un método que cambió de nombre se nombra con los dos (`{"doProcess", "activeProcess"}`), y un handler
    de `@Inject` puede no tomar los argumentos del método si su tipo cambió. Una clase que solo existe en JEG 0.14 se
    reconoce por nombre en `compat/jeg/JegItems`; un método que solo existe en JEG 0.14 se intercepta desde
    `mixin.jeg014` (en `main`, lista `mixins`: el plugin lo aplica solo con JEG 0.14 y `checkMixins` lo revisa solo contra
    0.14) y se llama por reflexión. Después de tocar algo de esto, `./gradlew checkMatrix` y
    `runClientAudit` con cada combinación.

## Estructura esperada
En la raíz solo van `README.md` (inglés), `README.es.md` (español, mismo contenido), `CHANGELOG.md`, `LICENSE`, `.github/` (plantillas de issues), este
archivo y lo de Gradle. Los demás documentos públicos van en `docs/`, con su índice en `docs/README.md`; las notas privadas, en `notas/`, que git ignora.
```
src/main/java/com/jegvr/
  JegVrCompat.java                 # @Mod("jeg_vr_compat")
  compat/VR.java                   # fachada segura
  compat/VrBackend.java            # lo que se le pide a un mod de VR
  compat/Versions.java             # qué JEG y qué Vivecraft hay instalados (sin cargar clases); elige bridge y mixins
  compat/vivecraft/VivecraftBridge.java  # la parte de VrBackend común a Vivecraft 1.2 y 1.3 (abstracta)
  compat/jeg/JegKeys.java          # claves NBT y constantes privadas de JEG
  compat/jeg/JegItems.java         # tipos de ítem que solo tiene JEG 0.14, por nombre
  mixin/JegVrMixinPlugin.java
  mixin/render/GunRenderingHandlerVRMixin.java
  mixin/render/AnimatedGunRendererVRMixin.java
  mixin/render/AnimatedGunArmsVRMixin.java
  mixin/render/ClassicGunArmsVRMixin.java
  mixin/recoil/GunRecoilVRMixin.java
  mixin/net/C2SMessageShootVRMixin.java
  mixin/model/PlayerModelVRMixin.java
  client/VrGunPose.java            # corrección de orientación por arma
  client/VrGunConfig.java          # poses: assets/jeg_vr_compat/gun_poses.json (jar) + config/jeg_vr_compat/guns.json (usuario)
  client/VrGunPoseFile.java        # formato de esos archivos y suma de capas (sin estado del juego: tiene pruebas)
  client/VrRecoil.java
  client/VrGrabReload.java
  client/VrZones.java              # geometría de las cajas (cadera, pozo, guardamanos), sin estado del juego: tiene pruebas
  client/VrThrow.java              # granadas, molotov y bomba de agua: gatillo o gesto (F9.26); VrThrowMotion, el gesto
  server/VrServerThrow.java        # la granada sale de la mano (mixin.server.ThrowableItemVRMixin)
  client/VrDebugCommands.java      # /jegvr help|pose|move|rot|scale|save|reset, axes, zones, grabzone (ver docs/COMANDOS.md)
  client/VrAxesGuide.java          # /jegvr axes: ejes y círculos de giro sobre el arma; VrAxesGeometry, sus líneas (tiene pruebas)
  config/VrClientConfig.java       # config/jeg_vr_compat-client.toml (ForgeConfigSpec CLIENT); getters = lo que rige
  config/VrServerConfig.java       # <mundo>/serverconfig/jeg_vr_compat-server.toml (SERVER): reglas S.8
  config/VrRules.java              # reglas del servidor y cómo se combinan con la opción del jugador (tiene pruebas)
  server/VrServerRules.java        # C2SVrHello -> S2CVrRules; quién juega en VR; reenvío al editar el archivo
  client/VrRulesClient.java        # reglas recibidas; se borran al salir
src/vc12/java/com/jegvr/         # solo Vivecraft 1.2: compat/vivecraft/Vivecraft12Bridge, mixin/vc12/HotbarTouchVRMixin
src/vc13/java/com/jegvr/         # solo Vivecraft 1.3: compat/vivecraft/Vivecraft13Bridge, mixin/vc13/HotbarTouchVRMixin
src/main/resources/
  META-INF/mods.toml
  jeg_vr_compat.mixins.json
  data/vivecraft/tags/items/rotated_tools.json   # experimento de la fase 3
```

## Clases de JEG que interceptamos (verificadas en el código fuente)
| Clase | Qué hace en flat |
|---|---|
| `client.handler.GunRenderingHandler` | `onRenderOverlay(RenderHandEvent)`: `translate(0.56*off,-0.52,-0.72)`, `mulPose(equip)`, `apply{Aiming,Sway,Sprinting,Recoil,Reload,Shield,Melee,Bobbing}Transforms` (privados void), `renderReloadArm`, `renderFirstPersonArms`, `renderWeapon` (público) |
| `client.render.gun.animated.AnimatedGunRenderer` | `renderByItem` (ADS/bobbing en línea ~L386-495), copias privadas de `apply*Transforms`, `renderRecursively` dibuja brazos en bones `left_arm/right_arm/fake_left_arm/fake_right_arm` vía `ModelPart.render(...)` (~L726-743) |
| `client.handler.GunRecoilHandler` | `onRenderTick`: rota cámara y jugador (`setXRot/setYRot`) |
| `client.handler.ReloadHandler` | `setReloading(boolean)` público → envía `C2SMessageReload` |
| `network.message.C2SMessageShoot` | ctor `(Player)` guarda `rotationYaw/rotationPitch`; el servidor los aplica al jugador antes de disparar (`ServerPlayHandler` ~L161) |
| `mixin.client.PlayerModelMixin`, `HumanoidModelMixin`, `client.handler.PlayerModelHandler` | poses de brazos en tercera persona |

Los números de línea son de `master` a fecha 2026-10; **verificar siempre contra el jar 0.13.2** con
`javap -c -p -classpath <jar de JEG en la caché de Gradle> <clase>` antes de escribir un `@At` o un descriptor.

## Comandos
- `./gradlew build` — compila (debe pasar sin warnings de mixin). Publicar `build/libs/jeg_vr_compat-<versión>.jar`, nunca el `-slim`.
- Versiones de una corrida: por defecto las más viejas (JEG 0.13.2 + Vivecraft 1.2.5, con las que se compila el jar). `-PlatestJeg`, `-PlatestVivecraft` o `-Platest` (las dos) usan las más nuevas, en `runClient`, `runClientAudit` y `runServer`.
- `./gradlew runClientAudit [-Pcompat]` — aplica todos los mixins de todos los mods, loguea lo que falla y cierra (carpeta `run-audit/`, VR apagado). `-Pcompat` suma Embeddium y Oculus. Después, `python tools/check_audit.py <javap>` lista los handlers `require = 0` que no encontraron dónde inyectar (el log no lo dice). El log trae el perfil: `Detected JEG 0.14.5 [mining, mounts] / Vivecraft 1.20.1-1.3.15 (vc13)`.
- `./gradlew checkMixins` — revisa los nombres de los mixins `remap = false` contra los jars que tienen los jugadores, en las cuatro combinaciones (necesita Python).
- `./gradlew checkMatrix` — compila `main` contra las cuatro combinaciones, `vc12` y `vc13` contra su Vivecraft, y corre `checkMixins`. Para probar otra versión puntual: `-Pjeg_version_latest=0.14.3` o `-Pvivecraft_version_latest=1.20.1-1.3.13-forge`.
- `./gradlew test` — pruebas de la matemática pura (`src/test`). La lógica nueva que se pueda separar del juego va en una clase sin
  estado de Vivecraft ni de Forge (como `VrZones`, `VrGunPoseFile`) y suma su prueba.
- `tools/jegdiff.sh <jar viejo> <jar nuevo>` — antes de soportar otra versión de JEG: qué clases de JEG que usa el addon
  faltan o cambiaron, y qué textos de `JegKeys` ya no están. Comparar jars del mismo tipo (los dos de Modrinth).
- Publicar: subir `mod_version`, pasar `## Unreleased` del `CHANGELOG.md` a `## X.Y.Z (fecha)`, commit y etiqueta `vX.Y.Z`;
  `.github/workflows/release.yml` sube a CurseForge y crea el release de GitHub (ver `docs/PUBLICAR.md`).
- `./gradlew runClient` — cliente de prueba (flat).
- `./gradlew runClient -Pzgc` — lo mismo con `-XX:+UseZGC` (perfil de IntelliJ `runClient (ZGC)`). Los `vmOptions` de un perfil Gradle de IntelliJ van a Gradle, no al juego.
- Dos jugadores: `runServer` + `runClient` (VR) + `runClient2` (`run-client2/`, usuario `Flat`, VR apagado); ver `docs/MULTIJUGADOR.md`.
- Logs de mixin: añadir `-Dmixin.debug.export=true -Dmixin.debug.verbose=true` en el run config si un inject no aplica.
- El usuario prueba VR manualmente; tras cada fase dejá un checklist de prueba en `docs/TESTING.md`.

## Convenciones
- Código y comentarios en inglés; documentación y mensajes al usuario en español.
- Commits pequeños, uno por tarea de `notas/PLAN.md`, con el ID de la tarea (ej. `F2.3: skip flat transforms in VR`).
- Si una firma de JEG o de la API de Vivecraft no coincide con lo descrito aquí, no inventes: inspeccioná el jar,
  corregí el código y actualizá este archivo.

## Firmas verificadas en JEG 0.13.2 (ver `reference/signatures.txt`)
- `IHeldAnimation` está en `ttv.migami.jeg.client.render` (no hay `C2SMessagePlayerRotation`; sí `C2SMessageBurst`, `C2SMessageShooting`).
- `renderFirstPersonArms(Player, HumanoidArm, ItemStack, PoseStack, MultiBufferSource, int, float)`.
- `GunRenderingHandler.renderWeapon(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, MultiBufferSource, int, float)` devuelve `boolean`.
- `AnimatedGunRenderer` solo tiene `applyRecoil/Bobbing/Aiming/Sway/Sprinting`; `GunRenderingHandler` suma `Reload/Shield/Melee`.
- `C2SMessageShoot`: campos privados `rotationYaw/rotationPitch`, ctors `()`, `(Player)`, `(float,float)`.
- `ShootingHandler.fire` construye `C2SMessageShoot(Player)` entre `GunFireEvent.Pre` y `Post`; `C2SMessageBurst()` no lleva rotación (`handleBurst` usa la del jugador en servidor).
- Mira: Vivecraft `VREffectsHelper.shouldRenderCrosshair()` (privado estático); JEG `CrosshairHandler.onRenderOverlay(RenderGuiOverlayEvent.Pre)`.
- `ServerPlayHandler.handleShoot(C2SMessageShoot, ServerPlayer)` lee el arma con `getItemInHand(MAIN_HAND)` una sola vez; la cadencia va en `ShootTracker` (por jugador y por `Item`, ctor público); `ReloadTracker.inventoryFeed(Player, Gun)` usa `getMainHandItem`. `FireMode` es clase (`SEMI_AUTO`, `AUTOMATIC`, `BURST`, `PULSE`, `RELEASE_FIRE`).
- `GripType` y `ReloadType` son clases (no enums). `GunFireEvent` está en `ttv.migami.jeg.event` (`Pre`/`Post`).


## Pose de armas en VR (automática)
- Vivecraft dibuja la mano con `VRArmHelper.renderVRHand_Main/Offhand` -> `ItemInHandRenderer.renderArmWithItem` (su mixin `vrRenderArmWithItem` lo sustituye): **no se dispara `RenderHandEvent`** en VR (verificado), así que `GunRenderingHandler.onRenderOverlay` no corre en VR; las armas pasan por `renderByItem`/`renderWeapon`.
- Marco de la mano: `RenderHelper.setupRenderingAtController` = translate(posición controlador - cámara) · matriz del controlador · scale(worldScale). Luego `VivecraftItemRendering.applyFirstPersonItemTransforms` añade una rotación por tipo; para `GunItem` la cancelamos en `VivecraftItemRenderingVRMixin`.

## Poses de armas (no van en el código)
- Toda pose/rotación/escala de armas va en `src/main/resources/assets/jeg_vr_compat/gun_poses.json` (capa interna, va en el jar), nunca como constante en Java. `config/jeg_vr_compat/guns.json` es la capa del usuario (una entrada del usuario reemplaza la interna de esa arma).
- En desarrollo (`FMLLoader.isProduction()` falso y el archivo fuente existe al lado de `run/`), `/jegvr ...` y `/jegvr save` editan el archivo fuente y se ignora `guns.json`. Comandos documentados en `docs/COMANDOS.md`.
