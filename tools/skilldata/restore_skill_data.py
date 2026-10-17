import json
import re
from pathlib import Path

java_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\skill")
migration_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\build\skilldata_migration")
target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")
target_dir.mkdir(parents=True, exist_ok=True)

# Texture to (ParticlePath, ColorHex)
TEXTURE_MAPPING = {
    "AxeRedSkillTexture": ("axe_particle/simple_red", "#FF3344"),
    "AxeBloodSkillTexture": ("axe_particle/simple_blackred", "#880022"),
    "AxeGreenSkillTexture": ("axe_particle/simple_green", "#33FF66"),
    "AxeKingSkillTexture": ("axe_particle/simple_king", "#FF8800"),
    "AxePinkSkillTexture": ("axe_particle/simple_pink", "#FF66CC"),
    "AxePurpleSkillTexture": ("axe_particle/simple_purple", "#AA33FF"),
    "AxeBlueSkillTexture": ("axe_particle/simple_blue", "#54D8FF"),
    "RedSkillTexture": ("skill_particle_sword/red", "#FF3344"),
    "GreenSkillTexture": ("skill_particle_sword/green", "#33FF66"),
    "PurpleSkillTexture": ("skill_particle_sword/purple", "#AA33FF"),
    "YellowSkillTexture": ("skill_particle_sword/holy", "#FFDD33"),
    "GoldSkillTexture": ("skill_particle_sword/gold", "#FFBB00"),
    "BlackSkillTexture": ("skill_particle_sword/black", "#333344"),
    "NomalSkillTexture": ("skill_particle_sword/blue", "#54D8FF"),
    "Spia_Particle_red": ("spear_particle/simple_red", "#FF3344"),
    "Spia_Particle_AxeGreen": ("spear_particle/simple_green", "#33FF66"),
    "Spia_Particle_SoftRed": ("spear_particle/simple_softred", "#FF6666"),
    "Spia_Particle_Purple": ("spear_particle/simple_purple", "#AA33FF"),
    "Spia_Particle": ("spear_particle/simple_blue", "#54D8FF"),
    "FlashingPenetrator_Texture": ("spear_particle/super_blue", "#00EEFF"),
    "MaceBlue_Texture": ("mace_particle/blue", "#54D8FF"),
    "MaceGreen_Texture": ("mace_particle/green", "#33FF66"),
    "MacePurple_Texture": ("mace_particle/purple", "#AA33FF"),
    "BlueRollTexture": ("skill_particle_sword/roll_blue", "#54D8FF"),
}

# Scan Java files to map class name to texture & sound info
java_info = {}
for java_file in java_dir.rglob("*.java"):
    code = java_file.read_text(encoding="utf-8")
    class_match = re.search(r"public\s+class\s+(\w+)", code)
    if not class_match:
        continue
    class_name = class_match.group(1)
    
    # Texture match
    found_texture = None
    for tex_key in TEXTURE_MAPPING:
        if tex_key in code:
            found_texture = tex_key
            break
            
    # Sound matches
    sounds = []
    if "PLAYER_ATTACK_SWEEP" in code:
        sounds.append("minecraft:entity.player.attack.sweep")
    if "PLAYER_ATTACK_STRONG" in code:
        sounds.append("minecraft:entity.player.attack.strong")
    if "PLAYER_ATTACK_KNOCKBACK" in code:
        sounds.append("minecraft:entity.player.attack.knockback")
    if "EXPLOSION" in code or "GENERIC_EXPLODE" in code:
        sounds.append("minecraft:entity.generic.explode")
    if "LIGHTNING" in code:
        sounds.append("minecraft:entity.lightning_bolt.thunder")
        
    # Check ModSounds
    for mod_sound in re.findall(r"ModSounds\.(\w+)\.get\(\)", code):
        sounds.append(f"swordskill:{mod_sound.lower()}")
        
    if not sounds:
        sounds.append("minecraft:entity.player.attack.sweep")

    # Motion extraction from Java setDeltaMovement or Vec3 move
    motions = []
    speed_match = re.search(r"(?:double|float)\s+(?:moveSpeed|speed|motion|speedFwd)\s*=\s*([\d.fF]+);", code)
    if speed_match:
        try:
            val = float(speed_match.group(1).replace("f", "").replace("F", ""))
            if val > 0:
                motions.append({"tick": 1, "forward": val, "strafe": 0.0, "vertical": 0.0})
        except ValueError:
            pass

    java_info[class_name] = {
        "texture": found_texture,
        "sounds": sounds,
        "motions": motions
    }

# Process migration files
count = 0
for json_file in migration_dir.glob("*.json"):
    if json_file.name == "conversion-report.json":
        continue
    
    data = json.loads(json_file.read_text(encoding="utf-8"))
    skill_name = data.get("name", "")
    
    # 1. Force legacy_fallback = False
    data["legacy_fallback"] = False
    
    # Find matching Java info
    matched_info = None
    for cls_name, info in java_info.items():
        if cls_name.lower() == skill_name.replace("_", "").lower() or skill_name.lower() in cls_name.lower():
            matched_info = info
            break
            
    # 2. Update Texture & Color
    particle_path = "skill_particle_sword/blue"
    color_hex = "#54D8FF"
    if matched_info and matched_info["texture"]:
        tex_name = matched_info["texture"]
        particle_path, color_hex = TEXTURE_MAPPING[tex_name]
    elif "axe" in skill_name:
        particle_path, color_hex = ("axe_particle/simple_red", "#FF3344")
    elif "spear" in skill_name or "thrust" in skill_name:
        particle_path, color_hex = ("spear_particle/simple_blue", "#54D8FF")
        
    data["color"] = color_hex
    
    # Update projectiles with correct particle
    for proj in data.get("projectiles", []):
        proj["particle"] = particle_path
        if not proj.get("sound") and matched_info:
            proj["sound"] = matched_info["sounds"][0]

    # Update hitboxes with correct sound
    for hb in data.get("hitboxes", []):
        if not hb.get("sound") and matched_info:
            hb["sound"] = matched_info["sounds"][0]

    # 3. Restore Motions & RUSH Types if present in migration or Java
    if matched_info and matched_info["motions"] and not data.get("motions"):
        data["motions"] = matched_info["motions"]

    # 4. Restore Sounds list
    if matched_info and matched_info["sounds"]:
        sound_events = []
        for idx, snd in enumerate(matched_info["sounds"]):
            sound_events.append({
                "tick": 1 + idx * 2,
                "sound": snd,
                "volume": 1.0,
                "pitch": 1.0
            })
        data["sounds"] = sound_events

    out_file = target_dir / json_file.name
    out_file.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    count += 1

print(f"Updated {count} skill JSONs with correct textures, colors, sounds, and restored motions.")
