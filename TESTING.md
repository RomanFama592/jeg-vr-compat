# Checklist de pruebas

Completar una sección por fase de `PLAN.md`. Las pruebas en VR las hace el usuario manualmente.

## Pendientes con servidor (el usuario las deja para más adelante)
Correr antes de las últimas fases del plan (F9.1/F9.4, Fase V, Fase M). Se arman con `MULTIJUGADOR.md`.
- [ ] Matriz de servidores F8.3 / S.6: solo JEG | JEG + Vivecraft | JEG + Vivecraft + addon, con cliente VR y flat.
- [ ] Servidor dedicado solo con JEG: balas desde el controlador (F6.3), cliente VR entra sin errores (Fase S), gatillo de la mano libre teletransporta (S.5/S.5b), recarga con la mano y modo precisión funcionan (Fase 7, S.7), cartuchos con la recarga de JEG cortada (F7.8).
- [ ] `runServer -PnoVivecraft`: el addon funciona sin Vivecraft en el servidor (Fase S).
- [ ] Servidor con addon + cliente flat sin addon: entra y juega normal (Fase S).
- [ ] (sin servidor, pero también pendiente) `.jar` (`-all`) en un Minecraft normal: poses del addon, `guns.json` del usuario y `/jegvr reset` (ver sección "Poses internas").
- [ ] Dos jugadores en supervivencia: el disparo de la mano libre le baja vida al otro (F8.5).
- [ ] Dos jugadores: el otro escucha el sonido de cada cartucho metido a mano (F7.8).

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
- [ ] Arco de JEG en la mano libre: el gatillo de esa mano no lo dispara (vuelve a teletransportar). Al disparar la mini gun con la principal, el arco de la mano libre no hace la animación de carga.
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
- [ ] El haz mide unos 32 bloques: se ve entero hasta ~27 y se desvanece hasta desaparecer a los 32.
- [ ] Apuntando a un bloque o a un mob a menos de 32 bloques: el haz se corta ahí y aparece un punto rojo; la flecha cae en ese punto.
- [ ] Apuntando hacia arriba o abajo: el haz sigue la curva de la flecha (con la velocidad del arco es casi recto en 16 bloques; la curva se nota poco).
- [ ] Modo precisión (dos manos): el haz sigue la línea de las dos manos.
- [ ] Al soltar (disparo) o cambiar de arma, el láser desaparece. El arco primitivo no tiene láser.
- [ ] Config → "Láser del arco compuesto" = No: no aparece.
- [ ] Flat: no hay láser (decisión del usuario: solo VR).

## F9.5 (Finger Gun en la hotbar en VR)
- [x] (auto) Compila; `AnimatedGunRendererVRMixin` aplica (28 mezclas).
- [ ] VR, Finger Gun en la hotbar: se ve el ícono (la mano con el dedo, como en flat), en la mano y en el inventario.
- [ ] VR, Finger Gun en la mano: sigue sin dibujarse el brazo de JEG (solo la mano de Vivecraft), igual que antes.
- [ ] VR, Finger Gun tirado en el suelo o en un marco: se ve.
- [ ] VR, otra arma en la mano: no aparecen brazos estáticos de JEG (sin cambios).
- [ ] Flat: el Finger Gun se ve igual que siempre.
