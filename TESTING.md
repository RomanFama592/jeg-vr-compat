# Checklist de pruebas

Completar una sección por fase de `PLAN.md`. Las pruebas en VR las hace el usuario manualmente.

## Pendientes con servidor (el usuario las deja para más adelante)
**Decisión (2026-11-08):** la alpha sale sin estas pruebas; la página avisa que el servidor no se probó en un servidor
dedicado. Quedan para cuando alguien reporte o el usuario decida correrlas.
Correr antes de las últimas fases del plan (F9.1/F9.4, Fase V, Fase M). Se arman con `MULTIJUGADOR.md`.
- [ ] Matriz de servidores F8.3 / S.6: solo JEG | JEG + Vivecraft | JEG + Vivecraft + addon, con cliente VR y flat.
- [ ] Servidor dedicado solo con JEG: balas desde el controlador (F6.3), cliente VR entra sin errores (Fase S), gatillo de la mano libre teletransporta (S.5/S.5b), recarga con la mano y modo precisión funcionan (Fase 7, S.7), cartuchos con la recarga de JEG cortada (F7.8).
- [ ] `runServer -PnoVivecraft`: el addon funciona sin Vivecraft en el servidor (Fase S).
- [ ] Servidor con addon + cliente flat sin addon: entra y juega normal (Fase S).
- [ ] (sin servidor, pero también pendiente) `.jar` (`-all`) en un Minecraft normal: poses del addon, `guns.json` del usuario y `/jegvr reset` (ver sección "Poses internas").
- [ ] Dos jugadores en supervivencia: el disparo de la mano libre le baja vida al otro (F8.5).
- [ ] Dos jugadores: el otro escucha el sonido de cada cartucho metido a mano (F7.8).
- [ ] Servidor solo con JEG (F9.22): golpear moviendo el arma de la mano principal le pega al mob, también a un costado de donde mirás. Con el addon en el servidor, también con la secundaria.
- [ ] Granadas (F9.26) en un servidor solo-JEG: salen hacia donde apunta la mano (desde la cabeza). Con el addon en el
  servidor: salen desde la mano, y con el gesto la fuerza cambia. Un cliente con addon viejo (protocolo 6) en un servidor
  nuevo y al revés: lanzan sin errores.
- [ ] Reglas del servidor (S.8) en un servidor dedicado: `world/serverconfig/jeg_vr_compat-server.toml` con `DENY` y
  `TRIGGER`; un cliente VR con el addon las recibe, uno con el addon viejo (protocolo 7) entra y juega con sus opciones,
  y uno sin el addon entra normal.
- [ ] Servidor dedicado con este `.jar` nuevo (la versión del canal subió a 4: cliente y servidor tienen que tener la misma versión del addon): disparo y medidores de la mano secundaria; el otro jugador escucha la preparación del lanzacohetes en esa mano.

## Auditoría, revisión 4 (rama `refactor/auditoria-publicacion`)
Verificado automáticamente: build, pruebas de `VrGunMirror`, `tools/check_mixins.py` y `runClientAudit` con Embeddium y
Oculus en plano.
- [x] **Jar real**: probado por el usuario con visor en su modpack, con Complementary Reimagined (ver `COMPATIBILIDAD.md`).
- [x] Con shaders, cajas guía (`/jegvr zones`): se ven en el mismo lugar con cada ojo (usuario, 2026-11-07).
- [x] Con shaders, láser del arco compuesto: se ve bien (usuario, 2026-11-07).
- [x] Crash de Acedium: sin Acedium, apagar y prender shaders en un mundo no crashea (usuario, 2026-11-07).
- [ ] (opcional) Con Acedium y sin el addon: confirmar que crashea igual.
- [ ] Versión equivocada: con Vivecraft 1.3 o JEG 0.14, Forge muestra el error de versión en lugar de crashear.
- [x] VR: arma en la mano izquierda (espejada): caras correctas, sin agujeros ni bajones de FPS (usuario, 2026-11-08).
- [ ] VR: dos armas animadas distintas: recargar una y disparar otra no anima a la otra, tampoco en la hotbar (G2).
- [ ] `guns.json`: romperlo a propósito con el juego abierto: un solo error en el log; al arreglarlo, se aplica solo.
- [ ] `/jegvr help`: sale en el idioma del juego (inglés y español).
- [ ] Protocolo de shaders de `COMPATIBILIDAD.md`.
- [ ] Servidor dedicado con este jar: arranca sin clases de cliente y el cliente VR detecta el addon (arma en la secundaria).

## S.8 — Reglas del servidor
Se prueban en un jugador: el archivo es `run/saves/<mundo>/serverconfig/jeg_vr_compat-server.toml` (o
`saves/<mundo>/serverconfig/` en un Minecraft normal). Se crea al abrir el mundo; editalo con el mundo abierto y se
aplica solo (el log dice `VR server rules reloaded`).
- [ ] Sin tocar el archivo (todo en `PLAYER`): las tres opciones se cambian en la pantalla como antes y no aparece ningún
  aviso en el chat.
- [ ] `reloadWithBothGuns = "DENY"` con tu opción encendida: con un arma en cada mano no recarga (doble vibración) ni
  con la tecla ni agarrando el cargador. En la pantalla de opciones la fila está gris, en "No", y el tooltip dice "Lo
  define el servidor".
- [ ] `reloadWithBothGuns = "ALLOW"` con tu opción apagada: recarga con un arma en cada mano.
- [ ] `throwMode = "TRIGGER"` con tu opción en "Gesto": las granadas se lanzan con el gatillo, hacia donde apunta la mano.
  `throwMode = "VIVECRAFT"`: como arco de Vivecraft.
- [ ] `bowLaser = "DENY"`: el arco compuesto no muestra el láser aunque tu opción esté encendida.
- [ ] Con algo distinto de `PLAYER` y el VR prendido, al entrar al mundo sale una vez el aviso "Este servidor define
  algunas opciones de VR por vos".
- [ ] Al salir del mundo y entrar a otro sin cambios, vuelven tus opciones.
- [ ] En plano (VR apagado), con `DENY`: un arma en cada mano recarga como en JEG.

JEG giraba el arma en tercera persona según hacia dónde mira la cabeza (el visor) y la pose de apuntado; ahora, en un
jugador VR, el arma sigue solo al brazo, que sigue al control.
- [ ] Con la cámara de Vivecraft (tercera persona o cámara de mixed reality): un rifle y una escopeta en la mano apuntan
  hacia donde apunta el control, aunque mires para otro lado (arriba, abajo, a un costado).
- [ ] Lo mismo con la minigun y el lanzacohetes.
- [ ] Una pistola (una mano): sigue bien como antes.
- [ ] Con un arma en cada mano: las dos siguen a su control.
- [ ] Otro jugador en plano (`runClient2`): sus armas en tercera persona se ven como siempre, inclinándose con su cabeza.

## F9.26 — Granadas en VR
Opción: Config del mod → "Granadas" (Gatillo por defecto, Gesto de lanzar, Como en Vivecraft).
Prueba del usuario (2026-11-08): funcionan bien como primera versión; el gesto se mejora en G.6.
- [ ] Al elegir una granada en la hotbar se ve en la mano principal, de pie sobre la mano y de un tamaño razonable
  (no en la secundaria ni enorme). Igual la de humo, la aturdidora, la molotov y la bomba de agua.
- [ ] Gatillo: apretar saca el seguro (sonido y vibración); soltar la lanza hacia donde apunta la mano, también apuntando
  a un costado o arriba, sin mirar hacia ahí.
- [ ] Soltar enseguida: igual se lanza, apenas después (JEG pide medio segundo).
- [ ] Mantener la granada común: vibra una vez por segundo y más rápido al final; si no la soltás, explota en la mano
  como en JEG.
- [ ] Mientras la sostenés, el gatillo no golpea, no rompe bloques ni teletransporta.
- [ ] En la mano secundaria (con la tecla de cambiar de mano), con un arma en la principal: cada gatillo hace lo suyo.
- [ ] Gesto: hacer el movimiento de lanzar y soltar en el medio: va hacia donde iba la mano, y más lejos cuanto más
  rápido. Soltar quieta: cae cerca, hacia donde apunta la mano. Caminando, el paso no suma a la fuerza.
- [ ] Mientras la sostenés con el seguro quitado, ¿la granada tiembla o se mueve sola en la mano? (Minecraft usa la
  animación de tensar un arco para estos ítems; si se ve, se corrige).
- [ ] "Como en Vivecraft": vuelve a lo de antes (arco en la secundaria).
- [ ] En plano (sin VR): las granadas funcionan como siempre.

## F9.25 — Sin recarga con un arma en cada mano
Opción: botón Config del mod en la lista de mods → "Recargar con un arma en cada mano" (por defecto apagada).
Probado por el usuario en VR (2026-11-08): funciona.
- [x] Apagada, un arma en cada mano: la tecla de recarga, el botón de apuntar (secundario) y el gatillo con el arma vacía
  no recargan ninguna de las dos; la mano de esa arma vibra dos veces cortito, sin repetirse sin parar al mantener el
  gatillo.
- [x] Apagada: con el arma de la secundaria apuntando hacia arriba, tampoco recarga; descargar sí funciona.
- [x] Apagada: soltar o guardar un arma y recargar la otra funciona normal (tecla, botón y con la mano).
- [x] Apagada: una recarga que ya había empezado antes de agarrar la segunda arma termina bien.
- [x] Encendida: todo como antes, con dos armas y con una.
- [x] En plano (sin VR), con la opción apagada: recarga como siempre.

## Fase 1
- [ ] Flat, sin Vivecraft: arranca, se puede disparar y recargar.
- [ ] Flat, con Vivecraft en modo NONVR: arranca sin errores de mixin.

- [x] (auto) runClient con Vivecraft 1.2.5 (NONVR) y con -PnoVivecraft: el addon carga, sin NoClassDefFoundError.
- [ ] Con visor: `VR active` pasa a true al entrar en VR (verificar con un log al abrir un mundo).

## Fase 2
- [ ] VR: sostener un arma clásica (p. ej. pistola) y una animada (GeckoLib): sigue al controlador sin sway, bobbing ni offset de pantalla.
- [ ] VR: mantener apuntar (ADS) no mueve ni centra el arma.
- [ ] VR: pasar `latest.log` con las líneas `jeg_vr_compat/diag` (decide si F2.4 requiere mover la intervención a `GunItemStackRenderer`).
- [ ] Flat (sin visor): apuntar, correr, recargar y disparar se ven igual que JEG solo.

## Fase 3 (calibración manual, opcional)
- [ ] VR: con el arma en mano, `/jegvr pose <pitch> <yaw> <roll> [tx ty tz]` cambia la pose en vivo (rotación en grados, traslación en bloques).
- [ ] `/jegvr save` escribe `config/jeg_vr_compat/guns.json`; editarlo a mano y guardar lo recarga sin reiniciar.
- [ ] Flat: nada cambia.

## Fase 3 (pose automática, sin calibrar)
- [x] VR: revólver (izquierda) y rifle (derecha) apuntan hacia adelante sin comandos. Quedaban corridos a la izquierda en ambas manos.
- [x] VR: tras quitar el offset de pantalla `firstperson_*` del modelo, el arma queda centrada en la mano. Faltaba bajarla y adelantarla un poco.
- [x] VR: con el offset de agarre en código (adelante 0.06, abajo 0.08), el mango calza en la mano en revólver y rifle.
- [ ] VR: 3 armas más de distinto tipo (escopeta, pistola, clásica no animada).
- [ ] Si alguna queda girada o corrida: enviar log (línea `jeg_vr_compat/diag`) y qué arma es; no hace falta calibrar.

## Fase 4 (brazos estáticos)
- [x] (auto) Los mixins de brazos y offset aplican al arrancar, sin errores de inyección.
- [x] VR: con un arma animada solo se ven las manos de Vivecraft (no los brazos con manga de JEG).
- [ ] VR: en una recarga animada el cargador sigue visible (hijo del bone de brazo).
- [ ] Flat: los brazos de JEG se siguen viendo igual que antes.

## Fase 5 (recoil)
- [x] (auto) `GunRecoilVRMixin` aplica al arrancar, sin errores de inyección.
- [ ] VR: al disparar, la cámara NO se mueve.
- [ ] VR: cada disparo patea el arma hacia arriba y atrás, y vuelve sola en unos ticks; ráfagas no la dejan girada más de ~25°.
- [ ] VR: vibra el controlador del arma; con un rifle/escopeta (dos manos) también vibra un poco la otra mano; con pistola/revólver no.
- [ ] VR: el patadón se nota proporcional (pistola suave, escopeta fuerte). Si es mucho o poco, decir cuál.
- [ ] Flat: el recoil de cámara de JEG sigue igual que antes.

## Fase 6 (disparo desde el controlador, recoil de gameplay, mira)
- [x] (auto) Los 8 mixins aplican al arrancar (incluidos `C2SMessageShootVRMixin` y `VREffectsHelperVRMixin`), sin errores.
- [x] VR: mirar hacia un lado y apuntar el arma hacia otro: la bala va adonde apunta el arma, no la cabeza. El primer disparo salía con ángulo y atravesaba bloques cercanos (se corrigió con convergencia, F6.5).
- [ ] VR (convergencia): el primer disparo pega justo donde apunta el cañón, de cerca y de lejos.
- [ ] VR (convergencia): con el arma apoyada en un bloque o metida en él, la bala impacta ese bloque y no lo atraviesa.
- [ ] VR: el primer disparo va exacto; en fuego continuo las balas siguientes suben y se abren a los lados, más con escopeta/rifle que con pistola. Al dejar de disparar ~1 s, vuelve a ser preciso.
- [ ] VR: lo visual del arma no cambió respecto a la Fase 5.
- [ ] VR: con un arma en la mano principal no se ve la mira/cursor (ni en el mundo ni en el HUD); al cambiar a otro ítem vuelve.
- [ ] VR: arma de ráfaga (burst): ¿los disparos 2..n van al controlador o a la cabeza? (F6.2)
- [ ] Servidor dedicado solo con JEG: las balas siguen al controlador (F6.3).
- [ ] Flat: puntería, recoil y mira igual que JEG solo.
- [x] VR: probado por el usuario. Bien en general; en algunas armas el recoil de puntería casi no se nota (aceptable por ahora).
- Si el recoil de puntería es mucho o poco: `AIM_CLIMB_PER_DEGREE`, `AIM_DRIFT_PER_DEGREE`, `AIM_DECAY` en `VrRecoil`.

## Fase S (componente de servidor opcional)
- [x] (auto) Compila; al arrancar el cliente aplican los 9 mixins, incluido `server.ProjectileEntityVRMixin`.
- [ ] VR, un jugador (el servidor integrado tiene el addon): la bala sale desde la punta del arma (se ve en la estela), no desde la cabeza.
- [ ] VR, un jugador: disparar por encima de una pared baja con el arma levantada sobre ella: la bala pasa (antes chocaba con la pared).
- [ ] VR, un jugador: con el arma metida en un bloque, la bala impacta ese bloque y no lo atraviesa.
- [ ] VR, un jugador: arma de ráfaga: todos los disparos siguen al controlador.
- [ ] Servidor dedicado con JEG + addon, sin Vivecraft (`./gradlew runServer -PnoVivecraft`; la primera vez hay que aceptar el EULA en `run-server/eula.txt`): un cliente VR entra y la bala sale del arma.
- [ ] Servidor dedicado solo con JEG (sin addon): un cliente VR entra sin errores y dispara como en la Fase 6 (convergencia).
- [ ] Servidor con addon + cliente flat sin addon: entra y juega normal.
- [ ] Flat con addon en ambos lados: disparo igual que JEG solo.
- [x] Probado por el usuario: todo bien.

## S.5 (disparo con la mano libre, base de dual wield)
- [x] (auto) Compila; al arrancar aplican los 10 mixins, incluido `server.ServerPlayHandlerVRMixin`.
- Preparación: en Controles → "JEG VR" aparece "Disparar arma de la mano libre (VR)". Asignarla a una tecla y después al gatillo de la mano libre en los bindings de Vivecraft/SteamVR (las teclas de mods deberían aparecer en la lista de acciones).
- [ ] VR, un jugador: arma en cada mano. El gatillo de la mano libre dispara el arma de esa mano, la bala sale de su cañón y va adonde apunta ese controlador. Vibra ese controlador.
- [ ] La munición que baja es la del arma de la mano libre; la de la mano principal no cambia.
- [ ] La misma arma en las dos manos: cada mano dispara a su propia cadencia y se pueden disparar a la vez.
- [ ] Arma automática en la mano libre: dispara mientras mantenés el gatillo; semi: un disparo por pulsación.
- [ ] Sin munición en la mano libre: no dispara (no se recarga todavía con la mano libre; ver F9.6).
- [ ] Servidor solo con JEG: la tecla no hace nada y no hay errores.
- [ ] Flat: nada cambia (la tecla solo actúa en VR).

## S.5b (dos armas: destello, animación, gatillo y recarga)
- [x] (auto) Compila; al arrancar aplican los 16 mixins (nuevos: `input.*`, `render.GunAnimationsVRMixin`, `server.ReloadTrackerVRMixin`).
- [ ] VR, arma en cada mano: al disparar con la derecha solo destella el arma derecha; al disparar con la izquierda destella y se anima (corredera, etc.) la izquierda.
- [ ] Misma arma animada en las dos manos: cada una se anima solo cuando dispara ella.
- [ ] Con un arma en la mano libre el gatillo de esa mano dispara y NO teletransporta (ni aparece el arco). Sin arma en esa mano, el gatillo vuelve a teletransportar.
- [ ] Recarga con el cañón de la mano libre hacia arriba (la principal no): recarga la de la mano libre, con animación y sonido; la munición de la principal no cambia.
- [ ] Recarga con las dos hacia arriba, o ninguna: recarga la principal (como antes).
- [ ] Solo arma en la mano libre (mano principal vacía u otro ítem): la tecla de recarga la recarga.
- [ ] Cambiar de slot del hotbar durante la recarga de la mano libre la cancela (como JEG con la principal).
- [ ] Servidor solo con JEG: el gatillo de la mano libre sigue teletransportando y la recarga es la de JEG.
- [ ] Flat: nada cambia.

## S.5c (ajustes de dos armas)
- [x] (auto) Compila; al arrancar aplican los 17 mixins (nuevo: `input.AimingHandlerVRMixin`).
- [ ] Arma a dos manos en la principal y la mano libre vacía: vibran los dos mandos. Con un arma u otro ítem en la mano libre: solo vibra el de la principal.
- [ ] Arco (vanilla) en la principal y arma en la mano libre: el gatillo de la mano libre tensa el arco / teletransporta como siempre, y no dispara el arma.
- [ ] Al disparar la mini gun con la principal, un arco de JEG en la mano libre no hace la animación de carga (ahora ese arco dispara con su gatillo: ver "Reglas de disparo en la mano secundaria").
- [ ] Botón de apuntar (zoom) con un arma: no hace zoom y recarga; con un arma en cada mano recarga la que tenga el cañón hacia arriba (la principal si las dos o ninguna).
- [ ] Flat: apuntar con clic derecho sigue haciendo zoom.

## Fase 7 (recarga con la mano)
- [x] (auto) Compila; al arrancar aplican los 17 mixins.
- Guías (cajas) apagadas por defecto: `/jegvr zones` o `showGuides = true` en `config/jeg_vr_compat-client.toml` las prende.
- [ ] VR, arma en la principal sin llenar, mano libre vacía, con munición: aparece una caja **azul** en la cadera del lado de la mano libre. Al meter la mano se pone **verde** y vibra suave.
- [ ] Sin munición para esa arma: la caja de la cadera es **roja** y el grip no agarra. Arma llena: no hay caja.
- [ ] Mano en la caja verde + grip (se puede soltar): vibra y suena; una caja **amarilla** sigue la mano y aparece una caja **blanca** debajo del arma.
- [ ] Tocar la caja blanca con la amarilla: se pone verde, recarga (animación y sonido de JEG) y vibran las dos manos.
- [ ] Volver a apretar el grip sostiendo el cargador: lo suelta. También se cancela al cambiar de slot o a los 10 s.
- [ ] Apretar el grip fuera de la caja de la cadera: cambia de slot del hotbar como siempre.
- [ ] Escopeta: cada agarre mete un solo cartucho; la tecla de recarga sigue cargando todos.
- [ ] La tecla de recarga y el botón de apuntar siguen recargando.
- [ ] Si la caja de la cadera o la del arma quedan incómodas: decir hacia dónde moverlas o si son chicas.
- [ ] Servidor solo con JEG: funciona igual (usa la recarga normal de JEG).
- [ ] Flat: nada cambia, no se ven cajas.
- [ ] **Supervivencia:** pistola sin llenar y balas de pistola en el inventario: la caja de la cadera es azul (no roja) y deja agarrar.
- [ ] **Animación de recarga con dos armas:** la principal vacía y sin su munición, la secundaria con otra munición: al disparar con la principal la secundaria no se anima. Recargar la principal anima solo la principal; recargar la secundaria (botón con la secundaria apuntando arriba) anima solo la secundaria.
- [ ] Secundaria sin balas: volver a apretar su gatillo la recarga (como JEG con la principal).

## F7.4 (modo sentado y config)
- [x] (auto) Compila; aplican los 17 mixins; se crea `config/jeg_vr_compat-client.toml` con `grabZone = "HIP"` y `showGuides = false` (cajas apagadas por defecto).
- [ ] Sentado con controles, `/jegvr grabzone front`: la caja aparece delante de la panza del lado de la mano libre; agarrar y meter el cargador funciona igual que de pie.
- [ ] `/jegvr grabzone off`: no hay cajas y el grip siempre cambia de slot; la tecla de recarga y el botón secundario siguen recargando.
- [ ] `/jegvr grabzone hip`: vuelve a la cadera.
- [ ] Config nueva: sin cajas. `/jegvr zones` las muestra; al cerrar y abrir el juego siguen visibles (se guarda). Otra vez `/jegvr zones` las oculta.
- [ ] Modo sentado de Vivecraft (manos sin tracking): no hay cajas; recargar con la tecla funciona.

## S.7 (modo precisión con las dos manos)
- [x] (auto) Compila; aplican los 17 mixins.
- [ ] Rifle (dos manos) en la principal, mano libre vacía: acercar la mano libre al guardamanos: aparece una caja azul unos 30 cm adelante de la mano del arma; con la mano adentro se pone verde.
- [ ] Grip en la caja verde: vibran las dos manos, suena, la caja pasa a violeta. El hotbar no cambia de slot.
- [ ] Disparar ráfagas largas con y sin modo precisión contra una pared: con precisión los impactos quedan más juntos y el arma patea menos.
- [ ] Sale del modo (caja vuelve a azul o desaparece): otro clic del grip; alejar la mano libre; recargar (botón o cargador); cambiar de slot.
- [ ] Pistola (una mano): no hay caja de guardamanos.
- [ ] Con un arma en cada mano: no hay caja.
- [ ] El cargador de la cadera sigue funcionando igual.
- [ ] `/jegvr zones` oculta también esta caja.
- [ ] Servidor solo con JEG: el modo funciona igual (usa el mensaje de apuntar de JEG).
- [ ] Flat: el botón de apuntar sigue haciendo zoom como siempre.

## S.7b/S.7c (apuntar con las dos manos)
- [x] (auto) Compila; aplican los 17 mixins.
- [ ] En modo precisión (caja violeta), mover la mano libre a izquierda, derecha, arriba y abajo: el arma gira alrededor de la mano principal y apunta hacia la mano libre; la caja violeta sigue a la mano libre.
- [ ] Disparar en modo precisión: las balas salen por donde apunta el cañón dibujado (probar apuntando solo moviendo la mano libre).
- [ ] La mano libre suelta el arma (sale del modo) al acercarla mucho a la principal, al alejarla mucho o al llevarla muy al costado.
- [ ] Fuera del modo precisión el arma sigue solo a la mano principal, como antes.
- [ ] Cámara de Vivecraft (tercera persona / cámara de mano): no se ven las cajas; en los ojos sí.

## F8.1/F8.2 (brazos del jugador VR vistos desde afuera)
- [x] (auto) Compila; aplican 22 mezclas (las 17 de antes + 5 de las poses de JEG).
- [ ] VR, cámara de tercera persona o de mano de Vivecraft, con pistola, rifle y minigun: los brazos siguen a los controles (como sin arma) y el arma está en la mano, no flotando ni pegada al pecho.
- [ ] Con dos armas: los dos brazos siguen a sus controles.
- [ ] Servidor con Vivecraft (`runServer`) + `runClient` (VR) + `runClient2` (flat): el jugador flat ve los brazos del jugador VR siguiendo los controles, con el arma en la mano.
- [ ] El jugador flat, visto desde el VR, tiene la pose de armas normal de JEG (brazos al frente apuntando).
- [ ] Flat solo: la tercera persona (F5) se ve como en JEG.

## Pantalla de opciones (botón Config de Forge)
- [x] (auto) Compila; el juego arranca con las 22 mezclas.
- [ ] Mods → JEG x Vivecraft Compat → Config: abre "JEG x Vivecraft: opciones" con dos botones y Listo.
- [ ] "Agarrar cargador" cambia entre cadera / delante de la panza / apagado; el texto al pasar el mouse explica cada una.
- [ ] "Cajas de ayuda" sí/no; al volver al juego (VR) se ven o no las cajas.
- [ ] Lo elegido queda en `config/jeg_vr_compat-client.toml` y se mantiene al reiniciar; `/jegvr zones` y `/jegvr grabzone` cambian lo mismo.

## F8.5 (arreglos tras la prueba con dos jugadores)
- [x] (auto) Compila; aplican las 22 mezclas.
- [ ] Cámara de Vivecraft y jugador flat: el rifle (y la minigun) se ven en la mano del jugador VR, no solo la pistola.
- [ ] Supervivencia, dos armas distintas: disparar al otro jugador con cada mano. Los dos disparos le bajan vida (corazones de su barra y sonido de golpe).

## F8.6 (arma de la mano secundaria vista desde afuera)
- [x] (auto) Compila; aplican 23 mezclas (nueva: `ItemInHandLayerVRMixin`).
- [x] VR con un arma en cada mano: el jugador flat (con addon + Vivecraft) ve las dos armas, cada una en su mano. Probar pistola + rifle.
- [x] Cámara de Vivecraft del jugador VR: se ven las dos armas.
- [x] Jugador flat con un arma en la principal y otra en la secundaria: como en JEG (la secundaria no se dibuja).

## F8.7 (armas sónicas con dos armas)
- [x] (auto) Compila; aplican 24 mezclas (nueva: `GunEventBusVRMixin`).
- [x] Hypersonic Cannon (o Supersonic Shotgun) en la principal y pistola en la secundaria: la pistola dispara balas normales, sin onda sónica. El cañón sigue lanzando su onda.
- [x] Al revés (sónica en la secundaria, pistola en la principal): la secundaria lanza la onda sónica y la principal dispara balas.
- [x] Con un arma que se desgasta en la secundaria: pierde durabilidad la secundaria, no la principal.
- [x] Flat y una sola arma: igual que antes.

## F7.8 (cartuchos uno a uno, rápido)
- [x] (auto) Compila; aplican 24 mezclas; canal en versión 3.
- [x] Un jugador (LAN/servidor pendientes), escopeta sin llenar y cartuchos en el inventario: agarrar en la cadera y tocar el pozo: entra un cartucho al instante (sin animación de recarga de JEG), suena la carga, el arma da una sacudida corta y vibran las dos manos.
- [x] Repetir rápido: cada agarre suma uno; si vas muy rápido algunos no entran (límite contra spam).
- [x] Escopeta vacía: el primer cartucho suena también como el cierre del arma.
- [x] Se gasta un cartucho del inventario por cada uno que entra; en creativo no se gasta.
- [ ] El otro jugador escucha el sonido de carga.
- [x] La tecla de recarga y el botón secundario siguen cargando todo con la animación de JEG.
- [ ] Servidor sin el addon: queda como antes (más lento, con la recarga de JEG cortada tras un cartucho).

## F9.12 (ambidiestro: arma en la secundaria, principal libre)
- [x] (auto) Compila; aplican 24 mezclas.
- [x] Un jugador. Arma con cargador **solo en la secundaria**, principal vacía, sin llenar y con munición: la caja (con `/jegvr zones`) aparece en la cadera del lado de la **mano principal**. Meter la principal (vibra), grip de la principal: agarra; tocar el pozo del arma de la secundaria: recarga esa arma (animación en la secundaria). El hotbar no cambia de slot.
- [x] Escopeta solo en la secundaria: cada cartucho entra al instante, como del otro lado.
- [x] Rifle solo en la secundaria: la caja del guardamanos aparece delante de la secundaria; grip de la principal → modo precisión (violeta). El rifle apunta por la línea secundaria → principal y los disparos van por ahí.
- [x] Sale del modo igual que del otro lado (otro grip, alejar la mano, recargar, cambiar de slot).
- [x] Del lado normal (arma en la principal) todo sigue igual.
- [x] Con un arma en cada mano: no hay cajas, se recarga con el botón.

## F9.14 (hotbar en el antebrazo)
- [x] (auto) Compila sin avisos; aplican 25 mezclas (nueva: `GuiHandlerVRMixin`); la config tiene `visuals.hudPlacement = "ELBOW"` (la opción vieja sí/no se migra sola).
- [x] Usuario: "hacia el codo" está bien.
- [x] Vivecraft con la HUD en la mano o en la muñeca (opciones de HUD de Vivecraft). Sin arma en la secundaria: la hotbar está donde siempre.
- [x] Con un arma en la secundaria (sola o con otra en la principal): la hotbar se corre hacia el codo, sin quedar tapada ni metida en el arma.
- [x] Tocar la hotbar con la otra mano sigue eligiendo el slot que tocás (en su nueva posición).
- [x] Probar pistola y rifle; y zurdo (manos invertidas en Vivecraft).
- [x] Config → "Hotbar con arma en la secundaria" = "Sobre el antebrazo": la hotbar queda a lo largo del lado interior del antebrazo (zona del marco rojo de la foto), se lee girando la muñeca, y tocarla con la otra mano elige el slot tocado.
- [x] "Como en Vivecraft": vuelve a la posición de Vivecraft.
- [x] HUD fija en la cabeza: nada cambia.

## F9.16 (gatillos con la mano dominante invertida)
- [x] (auto) Compila; aplican 27 mezclas (nuevas: `MCOpenVRAccessor`, `ShootingHandlerVRMixin`).
- [x] Primera prueba del usuario: seguía cruzado y mantener el gatillo contrario disparaba en ráfaga. Corregido (ver PLAN F9.16).
- [x] Mantener apretado el gatillo con una pistola o escopeta: dispara una sola vez por pulsación (no ráfaga), en las dos manos y con manos normales e invertidas.
- [x] Manos invertidas: las vibraciones salen en la mano correcta (la del arma que dispara).
- [x] Manos normales: cada arma dispara con el gatillo de su mano, como antes.
- [x] Manos invertidas en Vivecraft (sin cambiar nada en SteamVR): pistola en la derecha (secundaria) → dispara con el gatillo **derecho**; arma en la izquierda (principal) → gatillo **izquierdo**. Con un arma en cada mano, cada gatillo dispara la suya.
- [x] Manos invertidas: el gatillo de la mano del arma principal no teletransporta; sin arma, todo como Vivecraft.
- [x] Manos invertidas con el binding "reversed" de Vivecraft cargado en SteamVR: también cada gatillo dispara su arma.

## F9.17 (armas espejadas en la mano izquierda)
- [x] (auto) Compila sin avisos; aplican 28 mezclas (nueva: `render.ItemInHandRendererVRMixin`); la config tiene `visuals.mirrorLeftHand = true`.
- [x] Manos normales, pistola en la secundaria (izquierda): se ve espejada (cerrojo/eyector del lado izquierdo del arma), en la misma posición y ángulo que antes; las caras se ven por fuera (no "transparente" ni por dentro) y la iluminación es normal.
- [x] Disparar y recargar con esa arma: la animación (corredera, cargador) se mueve espejada y sigue pegada al arma; el destello sale de la boca del cañón y la bala sale por donde apuntás.
- [x] Probar un arma GeckoLib grande (rifle) y una clásica si hay alguna; también una con mira o accesorios: se ven completos.
- [x] Rifle en la izquierda + modo precisión (S.7b): sigue apuntando por la línea de las dos manos.
- [x] Manos invertidas en Vivecraft: el arma de la **principal** (ahora en la izquierda) se ve espejada y la de la secundaria (derecha) normal.
- [x] La mano derecha no cambia nada.
- [x] Config → "Espejar armas en la izquierda" = No: vuelve a verse como antes (sin espejar).
- [ ] Tercera persona / otros jugadores: hoy no se espeja (solo tu vista). Decime si querés que también.

## F9.11 (trompeta en la mano)
- [x] (auto) Compila sin avisos; aplican 28 mezclas.
- [x] Trompeta (`jeg:trumpet`) en la mano principal: apunta hacia adelante como un arma (campana adelante, boquilla hacia vos), pegada al control, no de costado ni separada.
- [x] En la secundaria (izquierda): igual, espejada.
- [ ] Agarre: el usuario lo vio unos cm por debajo de la mano; subido 4 cm (`ty 0.04`). Que las válvulas queden en la mano. Tamaño (foto del usuario: bien): si se ve muy chica o muy grande, o corrida, calibrala con `/jegvr pose` teniéndola en la principal (`/jegvr save`) y pasame los números para dejarlos por defecto.
- [x] Usarla (gatillo de usar): suena y lanza las notas como antes.
- [x] Flat: se ve como siempre.

## Comandos (`COMANDOS.md`)
- [ ] `/jegvr help` muestra el resumen en el chat.
- [ ] `/jegvr move 0 0.02 0` con un arma en la mano: sube 2 cm; `/jegvr show` muestra el valor.
- [ ] `/jegvr reset` con la trompeta en la mano: vuelve a la pose de fábrica de la trompeta (no a la de las armas).

## Poses internas (`gun_poses.json`) y del usuario (`guns.json`)
- [x] (auto) Compila; el `.jar` trae `assets/jeg_vr_compat/gun_poses.json`. En `runClient` el log dice que lee y edita el archivo de `src/main/resources` ("calibration commands edit the built-in file (development)").
- [x] Poses pasadas al archivo interno: trompeta y Holy Shotgun (calibradas por el usuario) y la general (que incluye el corrimiento del agarre que antes estaba en el código, `ty -0.08`, `tz -0.06`). El chat ahora muestra la `t` final (p. ej. trompeta `ty = 0.11` en vez de `0.19`): es la misma posición.
- [x] `runClient`: trompeta y Holy Shotgun se ven como las dejaste; las demás armas igual que antes.
- [ ] `runClient`: `/jegvr move 0 0.01 0` + `/jegvr save` cambia `src/main/resources/assets/jeg_vr_compat/gun_poses.json` (mirar el archivo en el proyecto).
- [ ] Con el `.jar` en un Minecraft normal (sin el proyecto): las poses del addon se ven igual; `/jegvr save` crea `config/jeg_vr_compat/guns.json` con solo tus cambios; `/jegvr reset` vuelve a la del addon. (Sin verificar todavía fuera del entorno de desarrollo.)

## F9.15 (mira láser del arco compuesto)
- [x] (auto) Compila; aplican 28 mezclas; config `visuals.bowLaser = true` y botón en la pantalla de config.
- [x] Usuario: el haz se ve bien; pidió 32 bloques (hecho: se desvanece en los últimos 5, del 27 al 32).
- [ ] VR, arco compuesto (`jeg:compound_bow`) en la principal: sin tensar no hay láser. Al tensar (mantener el gatillo) sale un haz rojo desde la punta del arco hacia donde apunta.
- [ ] El haz mide unos 32 bloques en el aire y se vuelve transparente de a poco hasta desaparecer en la punta.
- [ ] Apuntando a un bloque o a un mob a menos de 32 bloques: el haz se corta ahí y aparece un punto rojo; la flecha cae en ese punto.
- [ ] Apuntando hacia arriba o abajo: el haz sigue la curva de la flecha (con la velocidad del arco es casi recto en 16 bloques; la curva se nota poco).
- [x] Aspecto (pedido del usuario, aprobado): el haz se parece al del puntero láser de JEG: barra fina continua, casi blanca en la boca, después roja translúcida y transparente en la punta, tanto en el aire como al chocar con un bloque cercano; sin cortarse a lo lejos. En un bloque, el punto es el de JEG, pegado a la cara del bloque.
- [ ] Modo precisión (dos manos): el haz sigue la línea de las dos manos.
- [ ] Al soltar (disparo) o cambiar de arma, el láser desaparece. El arco primitivo no tiene láser.
- [ ] Config → "Láser del arco compuesto" = No: no aparece.
- [ ] Flat: no hay láser (decisión del usuario: solo VR).

## F9.5 (Finger Gun en la hotbar en VR)
- [x] (auto) Compila; `AnimatedGunRendererVRMixin` aplica (28 mezclas).
- [x] (usuario, primera mirada) VR, Finger Gun en la hotbar: se ve el ícono (la mano con el dedo, como en flat), en la mano y en el inventario.
- [x] VR, Finger Gun en la mano: sigue sin dibujarse el brazo de JEG (solo la mano de Vivecraft), igual que antes.
- [x] VR, Finger Gun tirado en el suelo o en un marco: se ve.
- [x] VR, otra arma en la mano: no aparecen brazos estáticos de JEG (sin cambios).
- [x] Flat: el Finger Gun se ve igual que siempre.

## F9.9 (nombre y munición junto a cada mano)
- [x] (auto) Compila; `GuiRendererVRMixin` aplica al arrancar (29 mezclas).
- [ ] VR, arma en la principal: junto a la mano, del lado del cuerpo, flota el nombre (con el color de JEG), la munición cargada en grande y la reserva en gris. Sigue a la mano y siempre mira hacia vos; no tapa la mira.
- [ ] El contador de JEG ya no aparece en la HUD de Vivecraft (el nombre y la munición no están repetidos).
- [ ] Un arma en cada mano: cada mano muestra lo suyo; al disparar baja solo el número de esa arma.
- [ ] Arma solo en la secundaria (con el addon en el servidor): el panel aparece en esa mano.
- [ ] Recargar: el arma que recarga muestra "Recargando" (sin reserva) y la otra sigue normal.
- [ ] Medidores (corrección pedida por el usuario): con el lanzacohetes en la principal, debajo del texto de esa mano sale el medidor de JEG; se llena al mantener el gatillo y ya no aparece en la HUD. Lo mismo con un arma de sobrecalentamiento (minigun, ametralladora ligera) y con los arcos (carga).
- [ ] Arma con medidor en la secundaria: sin medidor (esa mano dispara al instante, sin carga ni sobrecalentamiento).
- [ ] Por encima del arma (pedido del usuario): ninguna arma tapa el panel, ni siquiera acercándolo a la cara o con armas grandes (minigun, lanzacohetes). El texto se lee limpio, con su sombra detrás, y la barra llena se ve sobre la vacía. Los bloques tampoco lo tapan.
- [ ] Reserva: cambia al sumar o sacar munición del inventario; en creativo dice 9999.
- [ ] Manos invertidas: cada panel sigue del lado del cuerpo.
- [ ] Sin panel: Finger Gun, trompeta, sin arma, con un menú abierto o con F1.
- [ ] Modo sentado de Vivecraft: sin paneles; vuelve el contador de JEG.
- [ ] Config → "Munición junto a cada mano" = No: sin paneles; vuelve el contador de JEG.
- [ ] Flat: el contador de JEG igual que siempre.

## Reglas de disparo en la mano secundaria (F9.18)
Se prueba en un mundo de un jugador (ya tiene el addon en el servidor).
- [x] (auto) Compila; 29 mezclas al arrancar; el `@At` de `handlePreFireSound` verificado con `javap` (se aplica al cargar la clase, en la partida).
- [ ] Lanzacohetes en la secundaria: al mantener el gatillo se llena el medidor en el panel de esa mano, suena la preparación y recién ahí dispara; soltar antes cancela. Un disparo por apretada y su cadencia lenta.
- [ ] Minigun, ametralladora ligera o lanzallamas en la secundaria: el medidor de calor sube al disparar y baja al soltar. Al llenarse deja de disparar unos 4 s (80 ticks), salen humo y sonido, y no vuelve a disparar hasta enfriarse. Bajo el agua se enfría de golpe.
- [ ] Lanzallamas en la secundaria: al sobrecalentarse prende fuego al jugador, como en la principal.
- [ ] Soulhunter (ráfagas) en la secundaria: una apretada dispara la ráfaga completa aunque sueltes, con la pausa entre ráfagas.
- [ ] Typhoonee o cañón hipersónico (pulso) en la secundaria: medidor de preparación y un disparo por apretada.
- [ ] Arcos de JEG en la secundaria: mantener carga (medidor y animación de tensado), soltar dispara. Soltar enseguida no dispara. Poca carga hace menos daño. Con el compuesto sale el láser desde esa mano.
- [ ] Armas semiautomáticas y automáticas comunes en la secundaria: igual que antes.
- [ ] Un arma en cada mano: los medidores, el calor y la cadencia son independientes, incluso con la misma arma en las dos manos. Sobrecalentar una no frena a la otra.
- [ ] Un arco en cada mano: la carga de cada uno es la suya (el daño de uno no depende del otro).
- [ ] Sin munición en la secundaria: el gatillo recarga esa arma. Recarga con la mano y modo precisión con un arco solo en la secundaria (ahora se permite).
- [ ] Correcciones tras la primera prueba:
  - Retroceso: la minigun y el lanzacohetes en la secundaria empujan al personaje, en sentido contrario a donde apunta esa arma. Agachado, la mitad.
  - Cambiar de mano: sobrecalentá la minigun y cambiala de mano. Sigue caliente y bloqueada, y se enfría desde donde estaba. Lo mismo con la cadencia del lanzacohetes: disparar y cambiar de mano no deja volver a disparar antes de tiempo, para los dos lados.
  - Typhoonee en la secundaria: el rayo va hacia donde apunta esa mano, no la principal, y le pega a lo que apunta. También en la principal con modo precisión.
- [ ] Linterna acoplada a un arma en la secundaria (prendida con un golpe): ilumina hacia donde apunta esa arma, sigue a la mano y se apaga al golpear de nuevo. Corriendo se apaga, como en JEG.
- [ ] Linterna en un arma en cada mano: cada una ilumina hacia su lado.
- [ ] Puntero láser acoplado a un arma en la secundaria: el punto cae donde apunta esa arma.
- [ ] Mano principal y flat: sin cambios.

## Exclusión de la hotbar (F9.19)
Con la HUD de Vivecraft en la mano o la muñeca, y "Hotbar con arma en la secundaria" en "Hacia el codo" o "Sobre el antebrazo".
- [x] (auto) Compila; 31 mezclas; `config/jeg_vr_compat-client.toml` trae `hudExclusion = true` y `hudExcludedGuns = ["jeg:minigun"]`.
- [ ] Minigun en la secundaria: la hotbar queda en el dorso de la mano, donde la pone Vivecraft; tocarla con la otra mano cambia de slot.
- [ ] Otra arma en la secundaria: la hotbar va al codo o al antebrazo, como siempre.
- [ ] Config → "Hotbar en la mano con armas de la lista" = No: la minigun se comporta como las demás.
- [ ] Config → agregar otra arma al campo de texto (por ejemplo `jeg:minigun, jeg:flamethrower`) y salir con Listo: el lanzallamas también deja la hotbar en la mano, y la lista sigue igual al reabrir la pantalla. Lo mismo editando el `.toml`.
- [ ] La pantalla de config entra entera con todas las opciones (botón Listo visible), también con la escala de GUI más grande.

## F9.20 (animaciones de JEG eliminadas en VR: carrera, cuerpo a cuerpo, bayoneta y atasco)
- [x] (auto) Compila; `AnimatedGunItemVRMixin` y `AnimationProcessorVRMixin` aplican al arrancar (32 mezclas).
- [x] VR, correr con un arma: el arma no se mueve.
- [x] VR, cuerpo a cuerpo y bayoneta: el arma no se mueve; el golpe funciona.
- [x] VR, atasco (rifle de ráfaga o de combate), si se da: el arma no se mueve.
- [x] VR, disparar: el modelo anima (cerrojo, corredera) como antes, y salen casquillos y sonidos.
- [x] VR, cambiar de arma: se ve la animación de sacar, con su sonido.
- [x] VR, arcos: al cargar se tensa la cuerda en el modelo.
- [x] VR, recargar (revólver, escopeta, rifle con cargador) e inspeccionar (tecla Y): las animaciones se ven completas.
- [x] Partes escondidas: con el lanzacohetes, la ametralladora ligera y el rifle Blossom no aparece ninguna pieza de más al correr, y el arma no se corre de lugar.
- [x] Flat: todas las animaciones de JEG, como siempre.

## F9.22 (melee como la espada de Vivecraft)
Vivecraft → "Weapon collision" (colisión de armas): en AUTO está apagada en creativo, igual que con la espada. Probar en supervivencia o ponerla en ON.
- [x] (auto) Compila; `SwingTrackerVRMixin` y `ServerPlayHandlerVRMixin` aplican al arrancar (33 mezclas). `GunModifierHelperVRMixin` se aplica al cargar su clase.
- [ ] Arma en la principal: un movimiento fuerte contra un mob le pega, con vibración, sonido, partícula y empuje de JEG. Moverla despacio o tocarlo no le pega.
- [ ] Mob a un costado o atrás de donde mirás: si el arma lo alcanza, le pega igual.
- [ ] Sin bayoneta pega poco y con bayoneta más (daño de JEG).
- [ ] Enfriamiento: dos golpes seguidos, el segundo no pega hasta pasar ~¾ s (2 s corriendo). Disparar y golpear comparten esa espera.
- [ ] Bloques y puertas: al golpearlos con el arma pasa lo mismo que con una espada.
- [ ] Arma en la secundaria: lo mismo, con el daño de la bayoneta de esa arma.
- [ ] Arma en cada mano: cada golpe usa su arma.
- [ ] Minigun y arcos: sin melee de JEG (golpe común de Vivecraft, como antes).
- [ ] Apuntar y mover el arma rápido cerca de un mob: revisar que no pegue sin querer más de la cuenta.
- [ ] La tecla de melee de JEG sigue funcionando.
- [ ] Flat: el melee igual que siempre.

## Fase H (las manos siguen las animaciones)
Necesita ver las manos de Vivecraft ("Show player hands" encendido y sin el modo de brazos completos con el cuerpo a la vista). Sin esa mano no se desacopla nada.
- [x] (auto) Compila sin avisos de mixin; `VRArmRendererVRMixin` aplica al arrancar y `VRArmHelperVRMixin`, que solo carga en VR, aplica sin errores al forzar su carga (36 mezclas).
- [ ] Rifle de asalto en la principal, otra mano vacía, inspeccionar: la mano del arma se mueve con el arma y la otra va al arma y sigue la animación. Al terminar, las dos vuelven a los controles sin saltos.
- [ ] Recargar y sacar el arma: lo mismo. Mover el control durante la animación: el conjunto lo sigue con pesadez, y al terminar vuelve a seguirlo sin retraso.
- [ ] Disparar: la mano del arma sigue la animación, sin retraso y con el tiro saliendo a donde apunta. La otra mano sigue con su control y conserva lo que sostiene (H.2).
- [ ] Opción "Manos siguen las animaciones" apagada: inspeccionar, recargar y disparar sin que se muevan las manos ni haya pesadez, y el arma igual se anima (H.2).
- [ ] Holy Shotgun, Typhoonee y Atlantean Spear, con la tecla de recarga: el arma sube y se inclina durante la recarga, la mano del arma va con ella y la otra empuja bajo el cañón. Vuelven solas al terminar. Probar también con cada una en la secundaria (H.2).
- [ ] Otra mano con una antorcha o un bloque: se oculta mientras dura la animación y reaparece al terminar. La mano sigue la animación.
- [ ] Un arma en cada mano: al animarse una, la otra mano y su arma siguen con su control. Probar en las dos manos.
- [ ] Arma en la secundaria (espejada, F9.17): la mano del arma y la principal libre siguen la animación, en el lado correcto.
- [ ] Jugador zurdo ("Reverse hands"): lo mismo.
- [ ] Arma de una mano o minigun: la mano cuyo brazo no se ve en la animación se queda con el control y conserva su objeto.
- [ ] Modo precisión (dos manos): las manos siguen con los controles.
- [ ] Correr o hacer melee (animaciones eliminadas en F9.20): las manos no se mueven.
- [ ] Tamaño: la mano sobre el arma se ve proporcionada y agarrando en un lugar razonable.
- [ ] El panel de munición y la hotbar siguen en el control, no en la mano animada.
- [ ] Flat: sin cambios.

## F9.23 (descargar, inspeccionar y equipamiento en la secundaria)
- [x] (auto) Compila; las mezclas nuevas aplican sin errores, forzando la carga de las que no cargan solas (`AttachmentScreen`). Canal en versión 6: cliente y servidor con el mismo `.jar`.
- [ ] Arma solo en la secundaria: descargar devuelve su munición, inspeccionar la anima, y equipamiento abre su pantalla, con el nombre y el modelo de esa arma.
- [ ] Poner y sacar un accesorio en esa pantalla: al cerrar, el arma de la secundaria lo tiene o no. Se ve igual después de reentrar al mundo.
- [ ] Arma en cada mano, la secundaria hacia arriba y la principal no: las tres teclas actúan sobre la secundaria.
- [ ] Arma en cada mano, las dos hacia arriba o ninguna: actúan sobre la principal.
- [ ] Finger Gun en la secundaria: equipamiento no abre nada, como en la principal.
- [ ] Inspeccionar dos veces seguidas en la secundaria: reinicia la animación, como en la principal.
- [ ] Servidor sin el addon: las teclas siguen actuando sobre la principal.

## S.7d (caja del modo precisión)
Con `/jegvr zones` para ver la caja.
- [ ] Acercar la otra mano al arma: la caja se desliza por el cañón al nivel de la mano, en un solo eje, y se pone verde con la mano adentro.
- [ ] Con la mano al lado del control o detrás: la caja se queda a 5 cm del control, no más cerca.
- [ ] Agarrar en distintos puntos del cañón, también a unos 5 cm: entra al modo precisión y no se suelta enseguida.
- [ ] En violeta, igual que antes. Alejar la mano más de 70 cm, o girarla más de 50°: se suelta.

## F7.9 (Rocket Launcher cargado a mano)
- [ ] Agarrar el cohete de la cadera y tocar el arma: reproduce la recarga completa, igual que la tecla de recarga, y al terminar tiene el cohete. En las dos manos.
- [ ] Escopeta cargada a mano: sigue uno a uno, al instante y con sacudida.

## F9.24 (linterna, cruz y descarga en la secundaria)
- [ ] Linterna en la mano: se ve en el puño, apuntando adelante, en las dos manos. La luz sale hacia donde apunta. Si la pose no convence, `/jegvr` la ajusta como a un arma.
- [ ] Arma en la secundaria, mano principal vacía o con otro objeto: no se ve la cruz, ni en el mundo ni en el HUD. Sin armas en las manos, vuelve.
- [ ] Descargar en la secundaria: reproduce "sacar el arma", como en la principal, y devuelve la munición.
