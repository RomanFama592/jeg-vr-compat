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
