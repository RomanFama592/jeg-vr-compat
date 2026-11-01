# JEG x Vivecraft — Addon de compatibilidad VR

Addon de Forge 1.20.1 (`jeg_vr_compat`) que hace compatible **Just Enough Guns 0.13.2**
(https://github.com/MigaMiDev/Just-Enough-Guns) con **Vivecraft** sin modificar ninguno de los dos.
El plan de trabajo detallado está en `PLAN.md`. Leelo antes de empezar y marcá las tareas a medida que se completen.

## Stack
- Minecraft 1.20.1, Forge 47.2.x, ForgeGradle 6, Java 17, mappings oficiales (Mojang).
- JEG 0.13.2 (paquete raíz `ttv.migami.jeg`), usa GeckoLib `4.4.4` para armas animadas.
- Vivecraft **1.20.1-1.2.5-forge** (decisión del usuario). Solo trae `org.vivecraft.api_beta` (`VivecraftAPI.isVRPlayer`, `VivecraftClientAPI.isVrActive`); el resto se toma de internals (`VRPlayer.get().vrdata_world_render`: `c0` mano principal, `c1` libre, `hmd`; `MCVR.get().triggerHapticPulse`; `ClientDataHolderVR.getInstance().vrSettings.reverseHands`). `VivecraftBridge` los llama directo (Vivecraft es `compileOnly` + `runtimeOnly`), sin reflexión; `VR` captura `Throwable` y se desactiva si algo falla.
- JEG requiere además **MrCrayfish Framework >= 0.7.0** (obligatorio) y GeckoLib >= 4.4.4. El jar de JEG es `libs/JustEnoughGuns-0.13.2-1.20.1.jar` (se referencia como `fg.deobf('local:JustEnoughGuns:0.13.2-1.20.1')`; `files()` no se deobfusca).
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
3. **Vivecraft opcional.** Ninguna clase fuera de `compat/vivecraft/VivecraftBridge.java` puede importar
   `org.vivecraft.*`. `VR.java` comprueba `ModList.isLoaded("vivecraft")` antes de tocar el bridge.
4. **Mixins condicionales.** `JegVrMixinPlugin.shouldApplyMixin` devuelve false si falta `jeg` o `vivecraft`.
5. Mixins sobre clases de JEG: `@Mixin(value = X.class, remap = false)`; los `@At(INVOKE)` que apuntan a
   métodos vanilla llevan `remap = true`.
6. No esconder bones de GeckoLib para quitar brazos: los hijos (cargador en mano, balas) deben seguir visibles.
   Se suprime solo el dibujo de `ModelPart.render` del modelo del jugador.
7. Nada de rotar cámara/jugador en VR (mareo). El recoil va al modelo del arma, haptics y desviación de puntería.

## Estructura esperada
```
src/main/java/com/jegvr/
  JegVrCompat.java                 # @Mod("jeg_vr_compat")
  compat/VR.java                   # fachada segura
  compat/vivecraft/VivecraftBridge.java
  mixin/JegVrMixinPlugin.java
  mixin/render/GunRenderingHandlerVRMixin.java
  mixin/render/AnimatedGunRendererVRMixin.java
  mixin/render/AnimatedGunArmsVRMixin.java
  mixin/render/ClassicGunArmsVRMixin.java
  mixin/recoil/GunRecoilVRMixin.java
  mixin/net/C2SMessageShootVRMixin.java
  mixin/model/PlayerModelVRMixin.java
  client/VrGunPose.java            # corrección de orientación por arma
  client/VrGunConfig.java          # carga config/jeg_vr_compat/guns.json
  client/VrRecoil.java
  client/VrGrabReload.java
  client/VrDebugCommands.java      # /jegvr pose ..., /jegvr zones
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
`javap -c -p -classpath libs/jeg-0.13.2.jar <clase>` antes de escribir un `@At` o un descriptor.

## Comandos
- `./gradlew build` — compila (debe pasar sin warnings de mixin).
- `./gradlew runClient` — cliente de prueba (flat).
- Dos jugadores: `runServer` + `runClient` (VR) + `runClient2` (`run-client2/`, usuario `Flat`, VR apagado); ver `MULTIJUGADOR.md`.
- Logs de mixin: añadir `-Dmixin.debug.export=true -Dmixin.debug.verbose=true` en el run config si un inject no aplica.
- El usuario prueba VR manualmente; tras cada fase dejá un checklist de prueba en `TESTING.md`.

## Convenciones
- Código y comentarios en inglés; documentación y mensajes al usuario en español.
- Commits pequeños, uno por tarea de `PLAN.md`, con el ID de la tarea (ej. `F2.3: skip flat transforms in VR`).
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

