# Checklist de pruebas

Completar una sección por fase de `PLAN.md`. Las pruebas en VR las hace el usuario manualmente.

## Pendientes con servidor (el usuario las deja para más adelante)
Correr antes de las últimas fases del plan (F9.1/F9.4, Fase V, Fase M). Se arman con `MULTIJUGADOR.md`.
- [ ] Matriz de servidores F8.3 / S.6: solo JEG | JEG + Vivecraft | JEG + Vivecraft + addon, con cliente VR y flat.
- [ ] Servidor dedicado solo con JEG: balas desde el controlador (F6.3), cliente VR entra sin errores (Fase S), gatillo de la mano libre teletransporta (S.5/S.5b), recarga con la mano y modo precisión funcionan (Fase 7, S.7), cartuchos con la recarga de JEG cortada (F7.8).
- [ ] `runServer -PnoVivecraft`: el addon funciona sin Vivecraft en el servidor (Fase S).
- [ ] Servidor con addon + cliente flat sin addon: entra y juega normal (Fase S).
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
- [ ] Un jugador. Arma con cargador **solo en la secundaria**, principal vacía, sin llenar y con munición: la caja (con `/jegvr zones`) aparece en la cadera del lado de la **mano principal**. Meter la principal (vibra), grip de la principal: agarra; tocar el pozo del arma de la secundaria: recarga esa arma (animación en la secundaria). El hotbar no cambia de slot.
- [ ] Escopeta solo en la secundaria: cada cartucho entra al instante, como del otro lado.
- [ ] Rifle solo en la secundaria: la caja del guardamanos aparece delante de la secundaria; grip de la principal → modo precisión (violeta). El rifle apunta por la línea secundaria → principal y los disparos van por ahí.
- [ ] Sale del modo igual que del otro lado (otro grip, alejar la mano, recargar, cambiar de slot).
- [ ] Del lado normal (arma en la principal) todo sigue igual.
- [ ] Con un arma en cada mano: no hay cajas, se recarga con el botón.
