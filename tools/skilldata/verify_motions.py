import json
from pathlib import Path

migration_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\build\skilldata_migration")
target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

restored_count = 0
for json_file in migration_dir.glob("*.json"):
    if json_file.name == "conversion-report.json":
        continue
    orig = json.loads(json_file.read_text(encoding="utf-8"))
    target_path = target_dir / json_file.name
    if not target_path.exists():
        continue
    cur = json.loads(target_path.read_text(encoding="utf-8"))
    
    # Restore motions if original had motions
    if orig.get("motions") and not cur.get("motions"):
        cur["motions"] = orig["motions"]
        restored_count += 1
        
    # Restore rush_combo / rush params if original was RUSH
    if orig.get("type") == "RUSH":
        cur["type"] = "RUSH"
        for key in ["rush_start_ticks", "rush_start_animation", "rush_move_animation", "rush_finish_animation", "rush_combo"]:
            if key in orig:
                cur[key] = orig[key]
                
    target_path.write_text(json.dumps(cur, indent=2, ensure_ascii=False), encoding="utf-8")

print(f"Restored motions/rush settings for {restored_count} skills.")
