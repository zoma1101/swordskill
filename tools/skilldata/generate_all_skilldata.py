import json
import re
from pathlib import Path

source_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\build\skilldata_migration")
target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")
target_dir.mkdir(parents=True, exist_ok=True)

# Map skill texture names / keywords to hex colors
COLOR_MAP = {
    "red": "#FF3344",
    "blood": "#AA0022",
    "rage": "#FF2222",
    "fire": "#FF4400",
    "green": "#33FF66",
    "wind": "#33FF88",
    "poison": "#44FF44",
    "macegreen": "#33FF66",
    "purple": "#AA33FF",
    "shadow": "#8822FF",
    "dark": "#442266",
    "holy": "#FFDD33",
    "gold": "#FFBB00",
    "king": "#FF8800",
    "black": "#333344",
    "reaper": "#443355",
    "blue": "#54D8FF",
    "flashing": "#00EEFF",
    "star": "#54D8FF",
    "sakura": "#FF88CC",
}

def determine_color(skill_name: str, data: dict) -> str:
    name_lower = skill_name.lower()
    for kw, color in COLOR_MAP.items():
        if kw in name_lower:
            return color
    
    # Check particle textures in projectiles
    for proj in data.get("projectiles", []):
        part = proj.get("particle", "").lower()
        for kw, color in COLOR_MAP.items():
            if kw in part:
                return color
                
    return "#54D8FF"

processed_count = 0

for json_file in source_dir.glob("*.json"):
    if json_file.name == "conversion-report.json":
        continue
        
    data = json.loads(json_file.read_text(encoding="utf-8"))
    skill_name = data.get("name", json_file.stem)
    weapons = data.get("weapons", [])
    
    # 1. Force data-driven mode
    data["legacy_fallback"] = False
    
    # 2. Color determination
    data["color"] = determine_color(skill_name, data)
    
    # 3. Check motions and RUSH type: if motion forward <= 1.0 or non-rush skill has small motion, simplify
    motions = data.get("motions", [])
    filtered_motions = []
    for m in motions:
        fwd = m.get("forward", 0.0)
        # Keep motion only if forward > 1.0 or significant vertical
        if abs(fwd) > 1.0 or abs(m.get("vertical", 0.0)) > 1.5:
            filtered_motions.append(m)
    data["motions"] = filtered_motions
    
    if data.get("type") == "RUSH":
        rush_combo = data.get("rush_combo")
        start_fwd = 0.0
        if rush_combo and "start_motion" in rush_combo:
            start_fwd = rush_combo["start_motion"].get("forward", 0.0)
        elif filtered_motions:
            start_fwd = filtered_motions[0].get("forward", 0.0)
            
        # If speed is <= 1.0, convert RUSH type back to SIMPLE
        if start_fwd <= 1.0 and not rush_combo:
            data["type"] = "SIMPLE"
            data.pop("rush_start_ticks", None)
            data.pop("rush_start_animation", None)
            data.pop("rush_move_animation", None)
            data.pop("rush_finish_animation", None)
            data.pop("rush_combo", None)

    # 4. Claw skills: transform single projectile/hitbox events into 3 claw trails (Right, Center, Left)
    if "CLAW" in weapons:
        # Check projectiles
        projs = data.get("projectiles", [])
        if projs and len(projs) <= 4:
            new_projs = []
            for p in projs:
                offset = p.get("offset", [0.0, 1.2, 2.0])
                rot = p.get("rotation", [0.0, 0.0, 0.0])
                
                # Right claw
                p_r = json.loads(json.dumps(p))
                p_r["offset"] = [offset[0] + 0.4, offset[1], offset[2]]
                p_r["rotation"] = [rot[0] + 5, rot[1] + 5, rot[2] + 20]
                
                # Center claw
                p_c = json.loads(json.dumps(p))
                p_c["offset"] = [offset[0], offset[1], offset[2]]
                p_c["rotation"] = [rot[0] - 5, rot[1], rot[2] + 20]
                
                # Left claw
                p_l = json.loads(json.dumps(p))
                p_l["offset"] = [offset[0] - 0.4, offset[1], offset[2]]
                p_l["rotation"] = [rot[0] - 15, rot[1] - 5, rot[2] + 20]
                
                new_projs.extend([p_r, p_c, p_l])
            data["projectiles"] = new_projs
            
        # Check hitboxes
        hbs = data.get("hitboxes", [])
        if hbs and len(hbs) <= 4:
            new_hbs = []
            for h in hbs:
                offset = h.get("offset", [0.0, 1.2, 1.5])
                
                # Right claw hitbox
                h_r = json.loads(json.dumps(h))
                h_r["offset"] = [offset[0] + 0.4, offset[1], offset[2]]
                
                # Center claw hitbox
                h_c = json.loads(json.dumps(h))
                h_c["offset"] = [offset[0], offset[1], offset[2]]
                
                # Left claw hitbox
                h_l = json.loads(json.dumps(h))
                h_l["offset"] = [offset[0] - 0.4, offset[1], offset[2]]
                
                new_hbs.extend([h_r, h_c, h_l])
            data["hitboxes"] = new_hbs

    # Write out to target directory
    out_file = target_dir / json_file.name
    out_file.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    processed_count += 1

print(f"Successfully processed and generated {processed_count} skill JSONs in {target_dir}")
