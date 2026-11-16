import json, os, re

root = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata'
count = 0
for filename in sorted(os.listdir(root)):
    if not filename.endswith('.json'):
        continue
    path = os.path.join(root, filename)
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    data = json.loads(text)
    old_id = data.get('id')
    name = data.get('name')
    if not isinstance(old_id, int) or not isinstance(name, str):
        continue
    if 'legacy_id' in data:
        continue
    pattern = re.compile(r'("id"\s*:\s*)' + str(old_id) + r'(\s*,)')
    match = pattern.search(text)
    if not match:
        print('SKIP (pattern):', filename)
        continue
    replacement = match.group(1) + '"' + name + '",\n  "legacy_id": ' + str(old_id) + match.group(2)
    text = text[:match.start()] + replacement + text[match.end():]
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    count += 1
print('converted', count)
