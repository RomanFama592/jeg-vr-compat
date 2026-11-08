#!/usr/bin/env bash
# Compares two JEG jars on what this addon uses, before supporting a new JEG version (Fase M).
#
#   tools/jegdiff.sh <old JEG jar> <new JEG jar>
#
# - Every JEG class the addon's sources name (imports, mixin targets): MISSING if the new jar lacks it, CHANGED with
#   the differing members (javap -p) otherwise.
# - Every string in compat/jeg/JegKeys (NBT keys, bone and controller names...): GONE if the old jar has it and the
#   new one does not.
# Compare jars of the same kind: both as players have them (the Modrinth files), or both deobfuscated.
# A CHANGED class is not always a problem; it is the list of places to read. Exit code 1 when something is missing.
set -euo pipefail

if [ $# -ne 2 ]; then
    echo "usage: $0 <old JEG jar> <new JEG jar>" >&2
    exit 2
fi
old=$1
new=$2
root=$(cd "$(dirname "$0")/.." && pwd)

jdk_bin=""
for dir in "$root"/.tools/jdk-17*/bin; do
    [ -x "$dir/javap" ] || [ -x "$dir/javap.exe" ] && jdk_bin=$dir
done
javap=${jdk_bin:+$jdk_bin/}javap
jartool=${jdk_bin:+$jdk_bin/}jar

classes=$(grep -rhoE 'ttv\.migami\.jeg(\.[a-z0-9_]+)+\.[A-Z][A-Za-z0-9_]*' "$root/src/main/java" | sort -u)

missing=0
changed=0
for class in $classes; do
    before=$("$javap" -p -classpath "$old" "$class" 2>/dev/null || true)
    after=$("$javap" -p -classpath "$new" "$class" 2>/dev/null || true)
    if [ -z "$after" ]; then
        echo "MISSING  $class"
        missing=$((missing + 1))
    elif [ "$before" != "$after" ]; then
        echo "CHANGED  $class"
        diff <(echo "$before") <(echo "$after") | grep '^[<>]' | sed 's/^/    /' || true
        changed=$((changed + 1))
    fi
done

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir "$work/old" "$work/new"
(cd "$work/old" && "$jartool" xf "$old")
(cd "$work/new" && "$jartool" xf "$new")

gone=0
keys=$(grep -oE 'String [A-Z_]+ = "[^"]+"' "$root/src/main/java/com/jegvr/compat/jeg/JegKeys.java" | sed -E 's/.*"([^"]+)"/\1/')
for key in $keys; do
    if grep -rqaF -- "$key" "$work/old" && ! grep -rqaF -- "$key" "$work/new"; then
        echo "GONE     JegKeys \"$key\""
        gone=$((gone + 1))
    fi
done

count=$(echo "$classes" | wc -l | tr -d ' ')
echo "$count JEG classes compared: $missing missing, $changed changed; $gone JegKeys strings gone"
[ $missing -eq 0 ] && [ $gone -eq 0 ]
