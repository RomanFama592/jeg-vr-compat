# Cómo sacar una versión

[← Volver al índice](README.md)

Cada push a `master` compila y corre las pruebas en GitHub Actions ([`build.yml`](../.github/workflows/build.yml)); el
jar queda en la pestaña **Actions**, dentro de la corrida, para probarlo. Una etiqueta `vX.Y.Z` publica la versión en
CurseForge y crea el release de GitHub con el jar ([`release.yml`](../.github/workflows/release.yml)).

## Preparación (una sola vez)

1. **Token de CurseForge:** en CurseForge, *My Account → API Tokens* (en la página de autores), crear un token.
2. **Guardarlo en GitHub:** en el repo, *Settings → Secrets and variables → Actions*:
   - pestaña **Secrets**, *New repository secret*: nombre `CURSEFORGE_TOKEN`, valor el token;
   - pestaña **Variables**, *New repository variable*: nombre `CURSEFORGE_PROJECT_ID`, valor el *Project ID* que
     muestra la página del proyecto en CurseForge (columna de la derecha, "About Project").

El token no va nunca en un archivo del repo.

## Cada versión

1. Mientras se trabaja, anotar los cambios en `CHANGELOG.md` bajo `## Unreleased`.
2. Al publicar:
   - subir `mod_version` en `gradle.properties` (p. ej. `0.1.1`);
   - en `CHANGELOG.md`, cambiar `## Unreleased` por `## 0.1.1 (fecha)` y dejar un `## Unreleased` vacío arriba;
   - commit: `Release 0.1.1`.
3. Crear la etiqueta y subirla:

   ```bash
   git tag v0.1.1
   git push origin master v0.1.1
   ```

4. Mirar la corrida en **Actions**. Si sale bien, la versión aparece en CurseForge (puede tardar unos minutos en pasar la
   revisión) y en **Releases** de GitHub.

El workflow frena antes de publicar si la etiqueta no coincide con `mod_version`, si el `CHANGELOG.md` no tiene la
sección de esa versión o si todavía dice `(unreleased)`, o si fallan la compilación o las pruebas.

**Tipo de versión:** `0.x` sale como *alpha*; si la versión dice `beta` (p. ej. `0.5.0-beta`), como *beta*; de `1.0.0`
en adelante, como *release*.

**Dependencias en CurseForge:** Just Enough Guns y Vivecraft van como requeridas. Para cambiarlo, la lista
`dependencies` de `release.yml`.

## Modrinth, más adelante

`mc-publish` sube a las dos. Hay que sumar en `release.yml` `modrinth-id: ${{ vars.MODRINTH_PROJECT_ID }}` y
`modrinth-token: ${{ secrets.MODRINTH_TOKEN }}`, con el token de Modrinth (*Settings → PATs*, permiso para crear
versiones) y el ID del proyecto guardados igual que los de CurseForge.
