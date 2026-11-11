# Créditos

[← Volver al índice](README.md)

## Poses de armas

Cómo queda cada arma en la mano se calibra a ojo, con el visor puesto, y hay muchas armas. Si calibraste una y te
quedó bien, mandala: las poses aceptadas pasan a ser las del addon para todos, y quien las mandó figura acá y en el
[changelog](../CHANGELOG.md) de la versión en la que entran.

**Cómo mandar una pose**

1. Con el arma en la mano principal, ajustala con `/jegvr move`, `/jegvr rot` y `/jegvr scale`. `/jegvr axes` muestra
   los ejes y los giros sobre el arma. Todo está explicado en [COMANDOS.md](COMANDOS.md).
2. Guardala con `/jegvr save`.
3. Abrí un issue con la plantilla
   [Gun pose](https://github.com/RomanFama592/jeg-vr-compat/issues/new?template=gun_pose.yml) y pegá los valores que
   muestra `/jegvr show`, o la entrada del arma en `config/jeg_vr_compat/guns.json`. Una captura ayuda mucho.
4. Decí con qué nombre querés figurar, o dejalo vacío para no figurar.

También sirve un pull request que edite `src/main/resources/assets/jeg_vr_compat/gun_poses.json`.

**Qué se acepta**

- Poses de las armas de Just Enough Guns y de packs de armas que usen JEG, por su id de ítem.
- Con la mano derecha: en la izquierda el arma se espeja sola.
- Se prueban en VR antes de entrar. Si dos personas mandan la misma arma, queda la que mejor se ve con la mano en la
  empuñadura, y figuran las dos si la final sale de ambas.
- Lo que se manda se publica con la licencia del proyecto (GPL-3.0-or-later).

**Quiénes aportaron**

| Arma | Quién | Versión |
|---|---|---|
| *Todavía nadie: la primera puede ser tuya.* | | |

## Mods en los que se apoya

- [Just Enough Guns](https://modrinth.com/mod/just-enough-guns), de MigaMi: las armas, sus modelos y animaciones.
- [Vivecraft](https://modrinth.com/mod/vivecraft): Minecraft en VR.
- [MixinExtras](https://github.com/LlamaLad7/MixinExtras), de LlamaLad7 (va dentro del jar).

Este addon no está afiliado a los autores de esos mods.
