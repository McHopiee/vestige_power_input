#!/usr/bin/env python3
"""Merge the compiled keybind mod into the SVM Powers jar so players only need ONE file.
usage: merge_jar.py <svm base jar> <compiled rpkeys jar> <output jar>"""
import json, sys, zipfile
base, mod, out = sys.argv[1:4]
zb, zm = zipfile.ZipFile(base), zipfile.ZipFile(mod)
meta = json.loads(zb.read('fabric.mod.json').decode('utf-8-sig'))
meta['environment'] = '*'
ep = meta.setdefault('entrypoints', {})
ep['client'] = list(dict.fromkeys(ep.get('client', []) + ['rp.keybinds.RpKeybindsClient']))
have = set(zb.namelist())
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as zo:
    for item in zb.infolist():
        if item.filename == 'fabric.mod.json':
            zo.writestr('fabric.mod.json', json.dumps(meta, indent=2))
        elif not item.is_dir():
            zo.writestr(item, zb.read(item.filename))
    added = 0
    for item in zm.infolist():
        n = item.filename
        if item.is_dir() or n == 'fabric.mod.json' or n.startswith('META-INF/') or n.upper().startswith('LICENSE') or n in have:
            continue
        zo.writestr(n, zm.read(n)); added += 1
print('merged %d files from the keybind mod into %s' % (added, out))
