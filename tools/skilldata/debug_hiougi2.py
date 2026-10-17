import sys
from pathlib import Path
sys.path.insert(0, str(Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\skilldata")))

from convert_java_skills import (
    strip_java_comments, _method_bodies, _block_after,
    matching_parenthesis, split_java_arguments,
    _resolve_variables, _substitute, _resolve_expression, _number, _unwrap,
    _position_offset, _vector, _rotation_vector
)
import re

path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\skill\katana\Hiougi.java")
source = strip_java_comments(path.read_text(encoding="utf-8"))
methods = _method_bodies(source)
params, body = methods["performSlash"]

# Simulate passing slashIndex=0
values = _resolve_variables(body, dict(zip(params, ["level", "player", "0", "0.2F", "AxeRedSkillTexture()"])))

spawnPos_raw = values.get("spawnPos", "")
print("Raw spawnPos value (substituted):")
print(repr(spawnPos_raw))
print()

# The issue is spawnPos has player.position() and getLookAngle() - dynamic values
# But _position_offset should handle lookVec.scale(...)
# Let's check: after substitution, what does lookVec become?
print("lookVec in values:", values.get("lookVec", "NOT FOUND"))
print()

# Try calling _position_offset directly
try:
    offset = _position_offset(spawnPos_raw, values, methods)
    print("Offset:", offset)
except Exception as e:
    print("ERROR in _position_offset:", e)
    import traceback
    traceback.print_exc()
