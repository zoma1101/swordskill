import json
from pathlib import Path

target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

# Specific definitions for Claw skills to ensure beautiful 3-line claw trails and hitboxes
CLAW_TEXTURE_MAP = {
    "talon": ("skill_particle_sword/green", "#33FF66", 0.8, 1, [1]),
    "canine": ("skill_particle_sword/blue", "#54D8FF", 0.9, 1, [1]),
    "hant": ("skill_particle_sword/green", "#33FF66", 0.8, 1, [1]),
    "hant_killer": ("skill_particle_sword/green", "#33FF66", 0.8, 4, [4, 8, 13, 16]),
    "tiger_claw": ("skill_particle_sword/red", "#FF3344", 0.8, 4, [4, 8, 13, 16]),
    "dragon_claw": ("skill_particle_sword/red", "#FF3344", 0.9, 4, [3, 7, 12, 15]),
    "beast_claw": ("skill_particle_sword/red", "#FF2233", 0.8, 4, [2, 6, 11, 14]),
    "golden_claw": ("skill_particle_sword/gold", "#FFBB00", 1.0, 2, [3, 7]),
    "fast": ("skill_particle_sword/green", "#33FF66", 0.8, 1, [1]),
}

def create_claw_trail(particle: str, tick: int, dmg_mult: float, side_offset: float, pitch: float, yaw: float, roll: float):
    return {
        "tick": tick,
        "offset": [side_offset, 1.25, 2.0],
        "rotation": [pitch, yaw, roll],
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
    }

def create_claw_hitbox(tick: int, dmg_mult: float, side_offset: float):
    return {
        "tick": tick,
        "hitbox_enabled": True,
        "offset": [side_offset, 1.25, 1.5],
        "size": [1.5, 2.0, 2.5],
        "damage_multiplier": dmg_mult,
        "damage_type": "BASE",
        "knockback": 0.95,
        "knockback_multiplier": -1.0,
        "effects": [],
        "sound": "minecraft:entity.player.attack.sweep",
        "trigger": "TICK",
        "repeat_while_rushing": False
    }

# Process each JSON
for json_file in target_dir.glob("*.json"):
    data = json.loads(json_file.read_text(encoding="utf-8"))
    name = data.get("name", "")
    weapons = data.get("weapons", [])
    
    # Always disable legacy fallback
    data["legacy_fallback"] = False

    # Process Claw skills specially according to user instructions
    if "CLAW" in weapons or name in CLAW_TEXTURE_MAP:
        particle, color, dmg_mult, hit_count, ticks = CLAW_TEXTURE_MAP.get(
            name, ("skill_particle_sword/green", "#33FF66", 0.8, 1, [1])
        )
        data["color"] = color
        
        projectiles = []
        hitboxes = []
        
        for t in ticks:
            # 3 Claw trails (Right, Center, Left)
            projectiles.append(create_claw_trail(particle, t, dmg_mult, 0.5, 6.0, 5.0, 70.0))    # Right claw
            projectiles.append(create_claw_trail(particle, t, dmg_mult, 0.0, -6.0, 5.0, 70.0))   # Center claw
            projectiles.append(create_claw_trail(particle, t, dmg_mult, -0.5, -12.0, 5.0, 70.0)) # Left claw
            
            # 3 Hitboxes for the claws
            hitboxes.append(create_claw_hitbox(t, dmg_mult, 0.5))
            hitboxes.append(create_claw_hitbox(t, dmg_mult, 0.0))
            hitboxes.append(create_claw_hitbox(t, dmg_mult, -0.5))
            
        data["projectiles"] = projectiles
        data["hitboxes"] = hitboxes

    # Non-claw skills: if projectiles and hitboxes are both empty, add a default clean attack hitbox/projectile
    elif not data.get("projectiles") and not data.get("hitboxes"):
        color = data.get("color", "#54D8FF")
        data["hitboxes"] = [{
            "tick": 1,
            "hitbox_enabled": True,
            "offset": [0.0, 1.25, 2.0],
            "size": [3.0, 2.5, 3.0],
            "damage_multiplier": 1.5,
            "damage_type": "BASE",
            "knockback": 1.0,
            "knockback_multiplier": -1.0,
            "effects": [],
            "sound": "minecraft:entity.player.attack.strong" if "strong" in name else "minecraft:entity.player.attack.sweep",
            "trigger": "TICK",
            "repeat_while_rushing": False
        }]

    # Save modified JSON
    json_file.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

print("All skill JSONs updated with 3-line claw trails and complete data definitions.")
