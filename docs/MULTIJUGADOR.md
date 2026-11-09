# Pruebas con dos jugadores (VR + flat)

Cada comando va en su propia terminal y pueden correr a la vez. La primera compilación tarda; las siguientes no.

| Instancia | Comando | Carpeta | Nombre |
|---|---|---|---|
| Servidor | `./gradlew runServer` | `run-server/` | — |
| Cliente VR | `./gradlew runClient` | `run/` | `Dev` |
| Cliente flat | `./gradlew runClient2` | `run-client2/` | `Flat` |

## Pasos
1. **Servidor:** `./gradlew runServer`.
   - La primera vez se cierra solo y pide aceptar el EULA de Minecraft: abrí `run-server/eula.txt`, leelo y cambiá `eula=false` a `eula=true` si estás de acuerdo. Volvé a lanzarlo.
   - `run-server/server.properties` se crea con `online-mode=false`, porque los clientes de desarrollo no tienen cuenta de Mojang.
2. **Cliente VR:** `./gradlew runClient`, con SteamVR abierto. Multijugador → Conexión directa → `localhost`.
3. **Cliente flat:** `./gradlew runClient2`. Arranca con el VR de Vivecraft apagado (`run-client2/config/vivecraft-client-config.json`, que se crea solo la primera vez). Conectate igual, a `localhost`.

## Variantes
- **Servidor sin Vivecraft:** `./gradlew runServer -PnoVivecraft`.
  - Prueba que el addon funcione en un servidor que no tiene Vivecraft.
  - Sin Vivecraft en el servidor, el cliente flat no ve el cuerpo VR del otro: lo ve como un jugador normal.
- **Cliente flat sin Vivecraft:** `./gradlew runClient2 -PnoVivecraft`.
  - Prueba el caso de un jugador que no instaló Vivecraft.
  - Tampoco ve las poses VR.
- **Servidor solo con JEG (sin el addon):** no hay comando. Hay que armar un servidor aparte con los jars de JEG, Framework y GeckoLib.

## Qué mirar (Fase 8)
- Desde el cliente flat: el arma del jugador VR está en su mano y apunta adonde apunta el controlador.
- Las balas del jugador VR salen del cañón, también vistas desde el otro cliente.
- Con dos armas: el destello y el sonido salen de la mano correcta.
- El cliente flat dispara, recarga y apunta como en JEG solo.
