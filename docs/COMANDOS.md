# Comandos `/jegvr`

Son comandos de cliente: funcionan en un mundo propio y en cualquier servidor, y no hace falta ser operador.
`/jegvr help` muestra un resumen en el chat.

## Dónde se guardan las poses

Ninguna pose está escrita en el código. Hay dos archivos:

| Archivo | Quién lo usa | Qué guarda |
|---|---|---|
| `src/main/resources/assets/jeg_vr_compat/gun_poses.json` (dentro del `.jar`) | todos | las poses por defecto del addon: la general y las de cada arma calibrada |
| `config/jeg_vr_compat/guns.json` | cada jugador | sus propios ajustes, encima de los del addon |

- **Al jugar desde el proyecto** (`runClient`): los comandos cambian el archivo interno del proyecto y `/jegvr save`
  lo guarda ahí. Va dentro del próximo `.jar`, así que lo calibrado así lo tienen todos. `guns.json` no se usa.
- **Con el `.jar` instalado**: el archivo interno no se puede cambiar. Los comandos cambian `guns.json`, que se crea
  vacío la primera vez. Un arma con ajuste propio usa el del jugador en lugar del del addon; las demás siguen las
  poses del addon, también cuando se actualiza a una versión nueva.
- **Editar a mano**: los dos archivos se pueden editar con el juego abierto; se recargan solos en un segundo.

## Calibrar cómo se ve un arma en la mano

Los cambios se aplican al arma que tenés en la **mano principal** y se ven al instante. Si no tenés un arma en la
mano, cambian la pose general, que se aplica a todas las armas. Cada arma guarda solo su diferencia con esa pose
general.

### Ejes

Los ejes son los del control, no los del mundo:

| Eje | Positivo | Negativo |
|---|---|---|
| X | hacia la derecha | hacia la izquierda |
| Y | hacia arriba | hacia abajo |
| Z | hacia atrás (hacia vos) | hacia adelante (hacia donde apunta el control) |

- **Distancias:** en bloques, que en VR equivalen a metros. `0.01` = 1 cm.
- **Ángulos:** en grados. `pitch` sube o baja la punta, `yaw` la gira a la izquierda o la derecha, `roll` rota el
  arma sobre el cañón.
- **Mano izquierda:** el arma se espeja sola, así que se calibra con la derecha y vale para las dos manos.

### Comandos

| Comando | Qué hace |
|---|---|
| `/jegvr move <dx> <dy> <dz>` | Corre el arma esa cantidad desde donde está. En los ejes que no querés tocar poné `0`. |
| `/jegvr rot <dpitch> <dyaw> <droll>` | La gira esos grados desde donde está. En los que no querés tocar poné `0`. |
| `/jegvr scale <factor>` | Tamaño final, el mismo número que muestra el chat (más grande = arma más grande). |
| `/jegvr pose <pitch> <yaw> <roll> [tx ty tz]` | Pone valores absolutos, los mismos que muestra el chat. Sirve para copiar una pose. |
| `/jegvr show` | Muestra los valores actuales del arma en la mano. |
| `/jegvr save` | Guarda (ver "Dónde se guardan las poses"). Sin guardar, se pierde al cerrar el juego. |
| `/jegvr reset` | Con el `.jar`: borra tu ajuste de esa arma y vuelve a la pose del addon. Desde el proyecto: le quita su ajuste propio en el archivo interno (sin arma: vuelve la pose general a lo último guardado). |

**Ejemplos:**
- `/jegvr move 0 0.02 0`: 2 cm más arriba.
- `/jegvr move 0 0 -0.03`: 3 cm más adelante.
- `/jegvr rot 0 0 15`: rota 15° sobre el cañón.

Cada comando responde con los valores finales, por ejemplo
`jeg:holy_shotgun: pitch=0.0 yaw=0.0 roll=15.0 | t=(0.000, 0.020, 0.000) | scale=0.60`.
Con el `.jar`, esa línea sirve para compartir una pose y que se agregue a las poses del addon.

## Agarre a dos manos de cada arma

La mano fantasma y las partículas del modo precisión muestran dónde va la mano libre. Ese lugar sale solo del modelo del
arma (el brazo de apoyo que JEG le dibuja), y las armas sin ese brazo usan un lugar fijo delante del gatillo. Si en
alguna arma queda mal, se puede tomar de tu propia mano:

| Comando | Qué hace |
|---|---|
| `/jegvr grip capture [segundos]` | Cuenta 5 segundos (o los que digas), con una vibración por segundo en la mano libre. Al terminar guarda dónde está tu control libre respecto del control del arma, como agarre de esa arma. Se guarda solo, en el mismo archivo que las poses (desde el proyecto, en el del `.jar`). |
| `/jegvr grip reset` | El arma en la mano vuelve al agarre automático. |
| `/jegvr grip ghost` | Muestra la mano fantasma ahora (sirve para ver cómo quedó un agarre). |

En el entorno de desarrollo también hay una tecla, "Capturar agarre a dos manos (VR, depuración)", que captura al
instante; se asigna a un botón del control en los controles de Vivecraft / SteamVR. En el `.jar` publicado no aparece.

El agarre se guarda en la entrada del arma como `"grip": {"x", "y", "z", "qx", "qy", "qz", "qw"}`: la posición en metros
y la rotación del control libre, en los ejes del control del arma, para un arma en la mano derecha (en la izquierda se
usa espejado).

## Otros

| Comando | Qué hace |
|---|---|
| `/jegvr zones` | Muestra u oculta las cajas de ayuda de la recarga con la mano y del guardamanos. |
| `/jegvr grabzone hip\|front\|off` | Dónde se agarra el cargador: en la cadera (de pie), delante de la panza (sentado) o en ningún lado (recarga con la mano apagada). |

Estas dos opciones también están en el botón Config del mod, en la lista de mods.
