import json
import os
import re

root = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata'
count = 0
for filename in sorted(os.listdir(root)):
    if not filename.endswith('.json'):
        continue
    path = os.path.join(root, filename)
    with open(path, 'r', encoding='utf-8') as handle:
        data = json.load(handle)
    if data.get('legacy_id') != 0 or data.get('replace_builtin'):
        continue
    with open(path, 'r', encoding='utf-8') as handle:
        text = handle.read()
    updated = re.sub(r'^\s*"legacy_id"\s*:\s*0\s*,\r?\n', '', text, count=1, flags=re.MULTILINE)
    if updated != text:
        with open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(updated)
        count += 1
print('removed auto legacy_id from', count)
