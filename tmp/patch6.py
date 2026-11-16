path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\DataSkillDefinition.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old = "            boolean playerPitchAffected,\n            String easing) {"
new = "            boolean playerPitchAffected) {"
assert old in content, 'field not found'
content = content.replace(old, new, 1)

old2 = '            randomGroup = randomGroup == null ? "" : randomGroup;\n            easing = easing == null || easing.isBlank() ? "linear" : easing;\n            yawLimitDegrees = Math.max(0.0D, Math.min(360.0D, yawLimitDegrees));\n            pitchLimitDegrees = Math.max(0.0D, Math.min(180.0D, pitchLimitDegrees));\n'
new2 = '            randomGroup = randomGroup == null ? "" : randomGroup;\n            yawLimitDegrees = Math.max(0.0D, Math.min(360.0D, yawLimitDegrees));\n            pitchLimitDegrees = Math.max(0.0D, Math.min(180.0D, pitchLimitDegrees));\n'
assert old2 in content, 'norm not found'
content = content.replace(old2, new2, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')