import json
from pathlib import Path

skilldata_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

for json_path in skilldata_dir.glob("*.json"):
    try:
        content = json.loads(json_path.read_text(encoding="utf-8"))
        if not isinstance(content, dict):
            continue
        
        duration = content.get("duration_ticks", 1)
        max_tick = duration
        
        for m in content.get("motions", []):
            max_tick = max(max_tick, m.get("tick", 0))
        for h in content.get("hitboxes", []):
            max_tick = max(max_tick, h.get("tick", 0))
        for p in content.get("projectiles", []):
            max_tick = max(max_tick, p.get("tick", 0))
        for s in content.get("sounds", []):
            max_tick = max(max_tick, s.get("tick", 0))

        if max_tick > duration:
            print(f"Fixing {json_path.name}: increasing duration_ticks from {duration} to {max_tick}")
            content["duration_ticks"] = max_tick
            json_path.write_text(json.dumps(content, indent=2, ensure_ascii=False), encoding="utf-8")
    except Exception as e:
        print(f"Error processing {json_path.name}: {e}")
