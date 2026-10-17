import json
from pathlib import Path

target_dir = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata")

MOTIONS_UPDATE = {
    "fad_edge.json": {
        "motions": [
            {"tick": 8, "forward": 0.0, "strafe": 0.0, "vertical": 0.8},
            {"tick": 10, "forward": 0.0, "strafe": 0.0, "vertical": -1.0}
        ]
    },
    "rapid_bite.json": {
        "type": "RUSH",
        "motions": [
            {"tick": 1, "forward": 1.5, "strafe": 0.0, "vertical": 1.5}
        ],
        "rush_start_ticks": 1,
        "rush_combo": {
            "start_motion": {"forward": 1.5, "strafe": 0.0, "vertical": 1.5},
            "pitch_limit_degrees": 45.0,
            "search_radius": 8.0,
            "contact_radius": 3.0,
            "latest_contact_tick": 30,
            "no_contact_timeout_tick": 30,
            "contact_motion_forward": 1.25,
            "invulnerability_ticks": 35,
            "contact_animation_offset": 0,
            "finish_contact_offset": 1,
            "use_charge_multiplier": False
        }
    },
    "sakura_mai.json": {
        "motions": [
            {"tick": 3, "forward": 1.0, "strafe": 0.0, "vertical": 0.0},
            {"tick": 6, "forward": 1.0, "strafe": 0.0, "vertical": 0.0},
            {"tick": 9, "forward": 1.0, "strafe": 0.0, "vertical": 0.0},
            {"tick": 12, "forward": 1.0, "strafe": 0.0, "vertical": 0.0},
            {"tick": 15, "forward": 1.0, "strafe": 0.0, "vertical": 0.0},
            {"tick": 18, "forward": 1.0, "strafe": 0.0, "vertical": 0.0}
        ]
    },
    "zekku.json": {
        "motions": [
            {"tick": 1, "forward": 4.0, "strafe": 0.0, "vertical": 0.0}
        ]
    },
    "requiem.json": {
        "motions": [
            {"tick": 18, "forward": 0.0, "strafe": 0.0, "vertical": 0.8}
        ]
    },
    "storm.json": {
        "motions": [
            {"tick": 1, "forward": 1.5, "strafe": 0.0, "vertical": 0.0}
        ]
    },
    "storm_mirror.json": {
        "motions": [
            {"tick": 1, "forward": 4.0, "strafe": 0.0, "vertical": 0.0}
        ]
    },
    "judgment.json": {
        "motions": [
            {"tick": 18, "forward": -1.5, "strafe": 0.0, "vertical": 0.0},
            {"tick": 25, "forward": 2.5, "strafe": 0.0, "vertical": 0.0}
        ]
    }
}

for filename, update_data in MOTIONS_UPDATE.items():
    file_path = target_dir / filename
    if file_path.exists():
        data = json.loads(file_path.read_text(encoding="utf-8"))
        for k, v in update_data.items():
            data[k] = v
        file_path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"Successfully updated motions for {filename}")
    else:
        print(f"Warning: {filename} not found")

