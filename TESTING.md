# Checklist de pruebas

Completar una secciÃ³n por fase de `PLAN.md`. Las pruebas en VR las hace el usuario manualmente.

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

## Fase 3 (calibración)
- [ ] VR: con el arma en mano, `/jegvr pose <pitch> <yaw> <roll> [tx ty tz]` cambia la pose en vivo (rotación en grados, traslación en bloques). Probar valores de 90 en cada eje hasta que el cañón apunte hacia adelante.
- [ ] `/jegvr save` escribe `config/jeg_vr_compat/guns.json`; editarlo a mano y guardar lo recarga sin reiniciar.
- [x] CalibraciÃ³n inicial (revÃ³lver animado, derecha): pitch 45, ty 1 â†’ defaults en cÃ³digo. Falta validar otras armas, clÃ¡sicas y mano izquierda.
- [ ] Mano izquierda (zurdo en Vivecraft): la pose se espeja bien.
- [ ] Flat: nada cambia.

## Fase 3 (pose automática, sin calibrar)
- [ ] VR: revólver (animada) y una clásica: el cañón apunta hacia donde apunta el controlador, sin ejecutar ningún comando.
- [ ] VR: 3 armas más de distinto tipo (rifle, escopeta, pistola) y mano izquierda.
- [ ] Si alguna queda girada: enviar log (línea `jeg_vr_compat/diag`) y qué arma es; no hace falta calibrar.
