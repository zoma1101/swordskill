path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\data\SkillDataLoader.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old = '              double yawLimitDegrees = GsonHelper.getAsDouble(projectile, "yaw_limit_degrees", 0.0D);\n'
new = ('              double yawLimitDegrees = GsonHelper.getAsDouble(projectile, "yaw_limit_degrees", 0.0D);\n'
       '              double pitchLimitDegrees = GsonHelper.getAsDouble(projectile, "pitch_limit_degrees", 0.0D);\n')
assert old in content, 'parse not found'
content = content.replace(old, new, 1)

# Fix constructor call: add pitchLimitDegrees after yawLimitDegrees
old2 = '                    singleTarget, aimAtContact, yawLimitDegrees, playerPitchAffected, easing, yawLimitDegrees));'
# The actual current call:
import re
# Find the constructor call
old_call = '                    singleTarget, aimAtContact, yawLimitDegrees, playerPitchAffected, easing));'
new_call = '                    singleTarget, aimAtContact, yawLimitDegrees, pitchLimitDegrees, playerPitchAffected, easing));'
assert old_call in content, 'call not found: ' + repr(old_call)
content = content.replace(old_call, new_call, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')