path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\data\SkillDataLoader.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

if 'pitch_limit_degrees' not in content:
    old2 = '              double yawLimitDegrees = GsonHelper.getAsDouble(projectile, "yaw_limit_degrees", 0.0D);\n'
    new2 = old2 + '              double pitchLimitDegrees = GsonHelper.getAsDouble(projectile, "pitch_limit_degrees", 0.0D);\n'
    assert old2 in content, 'parse not found'
    content = content.replace(old2, new2, 1)

old3 = '                    singleTarget, aimAtContact, yawLimitDegrees, playerPitchAffected, easing, yawLimitDegrees));'
new3 = '                    singleTarget, aimAtContact, yawLimitDegrees, pitchLimitDegrees, playerPitchAffected, easing, yawLimitDegrees));'
assert old3 in content, 'call not found'
content = content.replace(old3, new3, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')