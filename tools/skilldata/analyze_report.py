import json

report_path = r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\build\skilldata_migration\conversion-report.json"
data = json.load(open(report_path, encoding="utf-8"))

print(f"Total skills: {len(data)}")

warnings_by_type = {}
for skill in data:
    name = skill["name"]
    warnings = skill.get("warnings", [])
    print(f"--- {name} (Class: {skill.get('class')}) ---")
    for w in warnings:
        print(f"  - {w}")
