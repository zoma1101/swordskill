path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\data\SkillDataLoader.java'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Remove easing parse line
for i, l in enumerate(lines):
    if 'String easing = GsonHelper.getAsString(projectile, "easing", "linear");' in l:
        del lines[i]
        break

# Fix constructor call
for i, l in enumerate(lines):
    if 'singleTarget, aimAtContact, yawLimitDegrees, pitchLimitDegrees, playerPitchAffected, easing));' in l:
        lines[i] = l.replace(', easing));', '));')
        break

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(lines)
print('done')