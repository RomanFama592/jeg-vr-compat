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
