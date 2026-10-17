import json
from pathlib import Path

report_path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\build\skilldata_migration\conversion-report.json")
data = json.loads(report_path.read_text(encoding="utf-8"))

skills_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\skill")

print(f"Total skills in report: {len(data)}")

no_events = []
has_warnings = []
for entry in data:
    name = entry["name"]
    output_path = Path(entry["output"])
    if output_path.exists():
        skill_json = json.loads(output_path.read_text(encoding="utf-8"))
        projs = skill_json.get("projectiles", [])
        hitboxes = skill_json.get("hitboxes", [])
        motions = skill_json.get("motions", [])
        sounds = skill_json.get("sounds", [])
        rush_combo = skill_json.get("rush_combo")
        fallback = skill_json.get("legacy_fallback", False)
        
        has_attack = bool(projs or hitboxes or rush_combo)
        if not has_attack:
            no_events.append((name, skill_json.get("type"), entry.get("class"), entry.get("source")))

print(f"\nSkills with NO attack events/hitboxes extracted ({len(no_events)}):")
for name, stype, sclass, ssource in no_events:
    print(f"  - {name} ({stype}) -> Class: {sclass}")

print(f"\nSkills with legacy_fallback=True:")
fallback_count = 0
for entry in data:
    output_path = Path(entry["output"])
    if output_path.exists():
        skill_json = json.loads(output_path.read_text(encoding="utf-8"))
        if skill_json.get("legacy_fallback"):
            fallback_count += 1
            print(f"  - {skill_json['name']} ({skill_json.get('type')})")

print(f"Total legacy_fallback=True count: {fallback_count}")
