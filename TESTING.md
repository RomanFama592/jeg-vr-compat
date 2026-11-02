# Checklist de pruebas

Completar una sección por fase de `PLAN.md`. Las pruebas en VR las hace el usuario manualmente.

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
- Guías visibles por defecto; `/jegvr zones` las oculta o las vuelve a mostrar.
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
- [x] (auto) Compila; aplican los 17 mixins; se crea `config/jeg_vr_compat-client.toml` con `grabZone = "HIP"` y `showGuides = true`.
- [ ] Sentado con controles, `/jegvr grabzone front`: la caja aparece delante de la panza del lado de la mano libre; agarrar y meter el cargador funciona igual que de pie.
- [ ] `/jegvr grabzone off`: no hay cajas y el grip siempre cambia de slot; la tecla de recarga y el botón secundario siguen recargando.
- [ ] `/jegvr grabzone hip`: vuelve a la cadera.
- [ ] `/jegvr zones` oculta las cajas; al cerrar y abrir el juego siguen ocultas (se guarda).
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
