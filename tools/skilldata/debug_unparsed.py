import sys
import traceback
from pathlib import Path
import json

from convert_java_skills import (
    load_registrations,
    strip_java_comments,
    extract_projectile_events,
    extract_touch_rush_events,
    extract_contact_combo_rush,
    inspect_skill_source,
)

project_root = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill")
registry_path = project_root / "src/main/java/com/zoma1101/swordskill/swordskills/SwordSkillRegistry.java"
skills_dir = project_root / "src/main/java/com/zoma1101/swordskill/swordskills/skill"

registrations = load_registrations(registry_path)

target_names = [
    "how_to_use", "sorvelte_charge", "falling_star", "hiougi", "flashing_penetrator",
    "rapid_bite", "talon", "hant", "hant_killer", "tiger_claw", "dragon_claw",
    "beast_claw", "golden_claw", "fast", "mace_impact"
]

for reg in registrations:
    if reg.name not in target_names:
        continue
    print(f"=================== {reg.name} ({reg.skill_class}) ===================")
    path, event_ticks, warnings = inspect_skill_source(skills_dir, reg.skill_class)
    if not path:
        print("  [ERROR] Source path not found!")
        continue
    
    source = strip_java_comments(path.read_text(encoding="utf-8"))
    
    hitboxes = []
    try:
        projectiles, attack_warnings = extract_projectile_events(source, reg.final_tick, hitboxes)
        print(f"  Standard extract -> projectiles: {len(projectiles)}, hitboxes: {len(hitboxes)}")
        if attack_warnings:
            print(f"  Standard attack_warnings: {attack_warnings}")
    except Exception as e:
        print(f"  Standard extract EXCEPTION: {e}")
        traceback.print_exc()

    if reg.skill_type == "RUSH":
        try:
            r_motions, r_hitboxes, r_warnings = extract_touch_rush_events(source, reg.final_tick)
            print(f"  Touch Rush -> motions: {len(r_motions)}, hitboxes: {len(r_hitboxes)}")
            if r_warnings:
                print(f"  Touch Rush warnings: {r_warnings}")
        except Exception as e:
            print(f"  Touch Rush EXCEPTION: {e}")

        try:
            combo_meta, combo_hb, combo_warn = extract_contact_combo_rush(source, reg.final_tick)
            print(f"  Combo Rush -> hitboxes: {len(combo_hb) if combo_hb else 0}")
            if combo_warn:
                print(f"  Combo Rush warnings: {combo_warn}")
        except Exception as e:
            print(f"  Combo Rush EXCEPTION: {e}")
