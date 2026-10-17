import sys
from pathlib import Path
sys.path.insert(0, str(Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\skilldata")))

from convert_java_skills import (
    strip_java_comments, _method_bodies, _block_after,
    matching_parenthesis, split_java_arguments,
    _resolve_variables, _projectile_from_call, _is_zero_movement,
    _hitbox_from_attack_effect
)
import re

path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\skill\claw\Talon.java")
source = strip_java_comments(path.read_text(encoding="utf-8"))

methods = _method_bodies(source)
print("Methods found:", list(methods.keys()))

execute_match = re.search(r"\bvoid\s+execute\s*\([^)]*\)\s*\{", source)
print("Execute match:", execute_match)

execute_body, _ = _block_after(source, execute_match.end() - 1)
print("Execute body:")
print(execute_body)

# Now check for FinalTick conditions
condition = re.compile(r"\bFinalTick\s*==\s*(\d+)\s*\)")
ticks = list(condition.finditer(execute_body))
print("FinalTick conditions found:", len(ticks))

# Check helper calls in body
print("\nHelper calls in execute body:")
for mname in methods:
    count = len(list(re.finditer(rf"\b{re.escape(mname)}\s*\(", execute_body)))
    print(f"  {mname}: {count} calls")

# Check which helpers have spawnAttackEffect
print("\nMethods with spawnAttackEffect:")
for mname, (params, mbody) in methods.items():
    if "spawnAttackEffect" in mbody:
        print(f"  {mname}({params})")
        spawn_calls = list(re.finditer(r"\bspawnAttackEffect\s*\(", mbody))
        print(f"    -> {len(spawn_calls)} spawnAttackEffect calls")
