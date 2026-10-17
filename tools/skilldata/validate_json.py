import json
from pathlib import Path

skilldata_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")
valid_types = {"SIMPLE", "RUSH", "TRANSFORM", "TRANSFORM_FINISH"}
valid_weapons = {"ONE_HANDED_SWORD", "TWO_HANDED_SWORD", "KATANA", "RAPIER", "DAGGER", "AXE", "MACE", "SPEAR", "SCYTHE", "CLAW", "DUALSWORD"}

errors = []

for json_path in skilldata_dir.glob("*.json"):
    try:
        content = json.loads(json_path.read_text(encoding="utf-8"))
        if not isinstance(content, dict):
            errors.append(f"{json_path.name}: Not a JSON object")
            continue
        
        duration = content.get("duration_ticks", 1)
        
        # Check motions
        for m in content.get("motions", []):
            if m.get("tick", 0) > duration:
                errors.append(f"{json_path.name}: motion tick {m.get('tick')} > duration_ticks {duration}")
                
        # Check hitboxes
        for h in content.get("hitboxes", []):
            if h.get("tick", 0) > duration:
                errors.append(f"{json_path.name}: hitbox tick {h.get('tick')} > duration_ticks {duration}")

        # Check projectiles
        for p in content.get("projectiles", []):
            if p.get("tick", 0) > duration:
                errors.append(f"{json_path.name}: projectile tick {p.get('tick')} > duration_ticks {duration}")

        # Check sounds
        for s in content.get("sounds", []):
            if s.get("tick", 0) > duration:
                errors.append(f"{json_path.name}: sound tick {s.get('tick')} > duration_ticks {duration}")

    except Exception as e:
        errors.append(f"{json_path.name}: JSON parse error: {e}")

if errors:
    print("ERRORS FOUND IN SKILLDATA:")
    for err in errors:
        print(" -", err)
else:
    print("ALL SKILLDATA JSON FILES ARE VALID!")
