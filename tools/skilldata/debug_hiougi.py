import sys
from pathlib import Path
sys.path.insert(0, str(Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\skilldata")))

from convert_java_skills import (
    strip_java_comments, _method_bodies, _block_after,
    matching_parenthesis, split_java_arguments,
    _resolve_variables, _substitute, _resolve_expression, _number, _unwrap
)
import re

path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\skill\katana\Hiougi.java")
source = strip_java_comments(path.read_text(encoding="utf-8"))

methods = _method_bodies(source)
print("Methods:", list(methods.keys()))

# performSlash is called with slashIndex=0,1,2
# Check what happens when we resolve with slashIndex="0"
params, body = methods["performSlash"]
print("performSlash params:", params)
print()

for si in ["0", "1", "2"]:
    print(f"--- slashIndex={si} ---")
    values = _resolve_variables(body, dict(zip(params, ["level", "player", si, "0.2F", "AxeRedSkillTexture()"])))
    print("Pos =", values.get("Pos"))
    
    # Try resolving spawnPos  
    spawnpos_raw = values.get("spawnPos", "")
    print("spawnPos resolved =", spawnpos_raw[:80] if spawnpos_raw else "MISSING")
    
    try:
        # The key expression: lookVec.scale(Pos)
        pos_val = values.get("Pos", si)
        pos_resolved = _resolve_expression(pos_val, values, methods)
        print("Pos resolved =", pos_resolved)
        try:
            n = _number(pos_resolved)
            print("Pos as number =", n)
        except Exception as e:
            print("Pos numeric err:", e)
    except Exception as e:
        print("ERR:", e)
    print()
