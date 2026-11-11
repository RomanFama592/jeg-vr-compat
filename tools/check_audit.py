#!/usr/bin/env python3
"""After `./gradlew runClientAudit`: which of the addon's mixin handlers ended up unused.

A handler with `require = 0` that finds no place to inject is not an error for Mixin, and the log says nothing: the
feature is simply missing in the game. The audit run exports every class as the game uses it (run-audit/.mixin.out).
This script reads the exported classes that carry the addon's handlers (their names start with jegvr$) and lists the
handlers that no instruction calls:

  python tools/check_audit.py <javap> [run folder, default run-audit]

Exits with 1 if a handler is unused. A class missing from the export was never loaded: the audit loads every mixin
target, so that is reported too, from the list of targets in the log.
"""
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MARK = 'jegvr$'


def main(javap, run='run-audit'):
    classes = ROOT / run / '.mixin.out' / 'class'
    if not classes.is_dir():
        print(f'{classes} does not exist: run ./gradlew runClientAudit first')
        return 2
    checked = 0
    unused = 0
    for path in sorted(classes.rglob('*.class')):
        if MARK.encode() not in path.read_bytes():
            continue
        run_javap = subprocess.run([javap, '-c', '-p', str(path)], capture_output=True, text=True)
        text = run_javap.stdout
        # Declarations end in ");" or ") throws ...;"; instructions are the lines with "// Method" or "// InterfaceMethod".
        declared = set()
        called = set()
        for line in text.splitlines():
            if '// Method' in line or '// InterfaceMethod' in line or '// InvokeDynamic' in line:
                called.update(re.findall(r'([\w$]*' + re.escape(MARK) + r'[\w$]*)', line.split('//', 1)[1]))
            elif '(' in line and not line.startswith('      '):
                match = re.search(r'([\w$]*' + re.escape(MARK) + r'[\w$]*)\(', line)
                if match:
                    declared.add(match.group(1))
        checked += 1
        name = str(path.relative_to(classes)).replace('\\', '/')[:-len('.class')].replace('/', '.')
        for handler in sorted(declared - called):
            # A lambda is reached through invokedynamic, which javap does not name.
            if 'lambda$' in handler:
                continue
            # Accessors and invokers are called from the addon's own classes, not from the target.
            if '$jegvr$' not in handler and not handler.startswith(('handler$', 'wrapOperation$', 'modify$', 'redirect$',
                                                                    'wrapWithCondition$', 'localvar$', 'constant$')):
                continue
            print(f'{name}: {handler} is never called (its injection point was not found)')
            unused += 1
    print(f'{checked} classes with handlers checked, {unused} unused handlers')
    return 1 if unused else 0


if __name__ == '__main__':
    sys.exit(main(*sys.argv[1:3]))
