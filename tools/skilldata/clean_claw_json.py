import json
from pathlib import Path

target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

CLAW_CONFIG = {
    "talon": ("skill_particle_sword/green", "#33FF66", 0.8, [1]),
    "canine": ("skill_particle_sword/blue", "#54D8FF", 0.9, [1]),
    "hant": ("skill_particle_sword/green", "#33FF66", 0.8, [1]),
    "hant_killer": ("skill_particle_sword/green", "#33FF66", 0.8, [4, 8, 13, 16]),
    "tiger_claw": ("skill_particle_sword/red", "#FF3344", 0.8, [4, 8, 13, 16]),
    "dragon_claw": ("skill_particle_sword/red", "#FF3344", 0.9, [3, 7, 12, 15]),
    "beast_claw": ("skill_particle_sword/red", "#FF2233", 0.8, [2, 6, 11, 14]),
    "golden_claw": ("skill_particle_sword/gold", "#FFBB00", 1.0, [3, 7]),
    "fast": ("skill_particle_sword/green", "#33FF66", 0.8, [1]),
}

for json_file in target_dir.glob("*.json"):
    data = json.loads(json_file.read_text(encoding="utf-8"))
    name = data.get("name", "")
    
    if name in CLAW_CONFIG:
        particle, color, dmg_mult, ticks = CLAW_CONFIG[name]
        data["color"] = color
        
        projectiles = []
        hitboxes = []
        for t in ticks:
            # Single standard trail defined in JSON (SkillUtils will handle the 3-claw trail bonus on CLAW weapon)
            projectiles.append({
                "tick": t,
                "offset": [0.0, 1.25, 2.0],
                "rotation": [0.0, 5.0, 70.0],
                "size": [4.2, 3.0, 1.4],
                "damage_multiplier": dmg_mult,
                "damage_type": "BASE",
                "knockback": 0.95,
                "knockback_multiplier": -1.0,
                "duration_ticks": 12,
                "particle": particle,
                "travel_distance": 2.5,
                "horizontal": False,
                "sound": "minecraft:entity.player.attack.sweep",
                "trigger": "TICK",
                "movement": None,
                "player_pitch_x_multiplier": 1.0,
                "player_pitch_z_multiplier": 0.0,
                "render_pitch": 0.0
            })
            hitboxes.append({
                "tick": t,
                "hitbox_enabled": True,
                "offset": [0.0, 1.25, 1.5],
                "size": [2.5, 2.0, 2.5],
                "damage_multiplier": dmg_mult,
                "damage_type": "BASE",
                "knockback": 0.95,
                "knockback_multiplier": -1.0,
                "effects": [],
                "sound": "minecraft:entity.player.attack.sweep",
                "trigger": "TICK",
                "repeat_while_rushing": False
            })
            
        data["projectiles"] = projectiles
        data["hitboxes"] = hitboxes
        
        json_file.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

print("Cleaned up claw skill JSONs to standard single trail definitions.")
