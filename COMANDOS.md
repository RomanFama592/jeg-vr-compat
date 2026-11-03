# Comandos `/jegvr`

Son comandos de cliente: funcionan en un mundo propio y en cualquier servidor, y no hace falta ser operador.
`/jegvr help` muestra un resumen en el chat.

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
| `/jegvr scale <factor>` | Tamaño final. `0.6` es el de todas las armas; `0.9` es un 50 % más grande. |
| `/jegvr pose <pitch> <yaw> <roll> [tx ty tz]` | Pone valores absolutos, los mismos que muestra el chat. Sirve para copiar una pose. |
| `/jegvr show` | Muestra los valores actuales del arma en la mano. |
| `/jegvr save` | Guarda en `config/jeg_vr_compat/guns.json`. Sin guardar, se pierde al cerrar el juego. |
| `/jegvr reset` | Vuelve el arma en la mano a la pose de fábrica. |

**Ejemplos:**
- `/jegvr move 0 0.02 0`: 2 cm más arriba.
- `/jegvr move 0 0 -0.03`: 3 cm más adelante.
- `/jegvr rot 0 0 15`: rota 15° sobre el cañón.

Cada comando responde con los valores finales, por ejemplo
`jeg:holy_shotgun: pitch=0.0 yaw=0.0 roll=15.0 | t=(0.000, 0.020, 0.000) | scale=0.60`.
Esa línea es la que hay que pasar para dejar la pose como valor por defecto del addon.

## Otros

| Comando | Qué hace |
|---|---|
| `/jegvr zones` | Muestra u oculta las cajas de ayuda de la recarga con la mano y del guardamanos. |
| `/jegvr grabzone hip\|front\|off` | Dónde se agarra el cargador: en la cadera (de pie), delante de la panza (sentado) o en ningún lado (recarga con la mano apagada). |

Estas dos opciones también están en el botón Config del mod, en la lista de mods.
