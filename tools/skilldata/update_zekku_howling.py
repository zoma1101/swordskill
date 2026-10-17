import json
from pathlib import Path

target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

# 1. Update zekku.json
zekku_path = target_dir / "zekku.json"
if zekku_path.exists():
    zekku = json.loads(zekku_path.read_text(encoding="utf-8"))
    
    # Instant stop at tick 2
    zekku["motions"] = [
        {"tick": 1, "forward": 4.0, "strafe": 0.0, "vertical": 0.0},
        {"tick": 2, "forward": 0.0, "strafe": 0.0, "vertical": 0.0}
    ]
    
    # Stationary slash effect spawned at tick 1 before/during movement
    zekku["projectiles"] = [
        {
            "tick": 1,
            "offset": [0.0, 1.25, 2.0],
            "rotation": [-6.0, 0.0, -45.0],
            "size": [6.0, 3.0, 0.5],
            "damage_multiplier": 3.5,
            "damage_type": "BASE",
            "knockback": 0.5,
            "knockback_multiplier": -1.0,
            "duration_ticks": 12,
            "particle": "skill_particle_sword/blue",
            "travel_distance": 0.0,
            "horizontal": False,
            "sound": "minecraft:entity.player.attack.strong",
            "trigger": "TICK",
            "movement": None,
            "player_pitch_x_multiplier": 1.0,
            "player_pitch_z_multiplier": 0.0,
            "render_pitch": 0.0
        }
    ]
    
    zekku_path.write_text(json.dumps(zekku, indent=2, ensure_ascii=False), encoding="utf-8")
    print("Updated zekku.json with instant stop and stationary slash projectile.")

# 2. Update howling_octave.json
howling_path = target_dir / "howling_octave.json"
if howling_path.exists():
    howling = json.loads(howling_path.read_text(encoding="utf-8"))
    
    howling["motions"] = [
        {"tick": 22, "forward": 0.0, "strafe": 0.0, "vertical": 0.8},
        {"tick": 32, "forward": 0.0, "strafe": 0.0, "vertical": -0.8},
        {"tick": 33, "forward": 0.0, "strafe": 0.0, "vertical": 0.0} # Instant stop
    ]
    
    howling_path.write_text(json.dumps(howling, indent=2, ensure_ascii=False), encoding="utf-8")
    print("Updated howling_octave.json with instant stop.")

