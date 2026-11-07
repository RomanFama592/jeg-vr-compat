#!/usr/bin/env python3
"""Checks the names the addon's remap=false mixins use against the jars players have.

Development runs on deobfuscated jars, where a method that overrides a vanilla one has its Mojang name. In the game
that method has its SRG name (m_108829_), and a remap=false mixin is not translated, so it only fails outside of
development. This script reads the mixin sources and looks every name up in the original jars:

  python tools/check_mixins.py <javap> <classpath of the original jars, separated by ;>

Exits with 1 if a name is missing. Run it after any change to a mixin and against every new JEG or Vivecraft version.
"""
import pathlib
import re
import subprocess
import sys

SOURCES = pathlib.Path(__file__).resolve().parent.parent / 'src/main/java/com/jegvr/mixin'


def main(javap, classpath):
    cache = {}

    def dump(cls):
        if cls not in cache:
            run = subprocess.run([javap, '-p', '-l', '-classpath', classpath, cls], capture_output=True, text=True)
            cache[cls] = run.stdout if run.returncode == 0 else None
        return cache[cls]

    def has(text, name):
        return name == '<init>' or re.search(r'[\s.]' + re.escape(name) + r'[(;\s]', text) is not None

    problems = 0
    for path in sorted(SOURCES.rglob('*.java')):
        text = path.read_text(encoding='utf-8')
        header = re.search(r'@Mixin\((.*?)\)\s*\n?public', text, re.S)
        if not header or 'remap = false' not in header.group(1):
            continue
        imports = {i.rsplit('.', 1)[-1]: i for i in re.findall(r'^import ([\w.]+);', text, re.M)}
        constants = dict(re.findall(r'String (\w+)\s*=\s*\n?\s*"([^"]+)"', text))
        groups = []
        for selector in re.findall(r'method\s*=\s*(\{[^}]*\}|"[^"]*"|\w+)', text):
            names = re.findall(r'"([^"]+)"', selector)
            names += [constants[c] for c in re.findall(r'\b([A-Z_]{2,})\b', selector) if c in constants]
            groups.append([n.split('(')[0] for n in names])
        for name in re.findall(r'@(?:Accessor|Invoker)\("([^"]+)"\)', text) \
                + re.findall(r'@Shadow\s+private [\w<>.]+ (\w+);', text) \
                + re.findall(r'@Local\(name = "(\w+)"\)', text):
            groups.append([name])
        for target in re.findall(r'(\w+)\.class', header.group(1)):
            full = imports.get(target, target)
            members = dump(full)
            if members is None:
                print(f'{path.name}: target class {full} not found')
                problems += 1
                continue
            for group in groups:
                # An array is fine when one of its names exists: {development name, game name}.
                if group and not any(has(members, name) for name in group):
                    print(f'{path.name} -> {full}: none of {group} exists in the game')
                    problems += 1
    print(f'{len(cache)} target classes checked, {problems} problems')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1], sys.argv[2]))
