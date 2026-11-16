path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\blockbench\swordskill_skill_creator.js'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. values
old = "        yaw_limit_degrees: existing.yaw_limit_degrees ?? 0,\n        easing: existing.easing ?? 'linear'\n"
new = ("        yaw_limit_degrees: existing.yaw_limit_degrees ?? 0,\n"
       "        pitch_limit_degrees: existing.pitch_limit_degrees ?? 0,\n"
       "        easing: existing.easing ?? 'linear'\n")
assert old in content, 'values not found'
content = content.replace(old, new, 1)

# 2. form: add pitch_limit_degrees after yaw_limit_degrees form field
old_form = """            yaw_limit_degrees: {
                label: 'Yaw limit from player facing (0 = no limit)',
                type: 'number',
                value: values.yaw_limit_degrees,
                min: 0,
                max: 360,
                step: 1,
                description: 'Clamps the flying slash yaw relative to the player yaw.'
            },
            easing: {"""
new_form = """            yaw_limit_degrees: {
                label: 'Yaw limit from player facing (0 = no limit)',
                type: 'number',
                value: values.yaw_limit_degrees,
                min: 0,
                max: 360,
                step: 1,
                description: 'Clamps the flying slash yaw relative to the player yaw.'
            },
            pitch_limit_degrees: {
                label: 'Pitch limit from player pitch (0 = no limit)',
                type: 'number',
                value: values.pitch_limit_degrees,
                min: 0,
                max: 180,
                step: 1,
                description: 'Clamps the player pitch contribution to the flying slash rotation.'
            },
            easing: {"""
assert old_form in content, 'form not found'
content = content.replace(old_form, new_form, 1)

# 3. validation: add after yawLimitDegrees validation
old_val = """    const yawLimitDegrees = Number(form.yaw_limit_degrees ?? 0);
    if (!Number.isFinite(yawLimitDegrees) || yawLimitDegrees < 0 || yawLimitDegrees > 360) {
        throw new Error('Yaw limit must be between 0 and 360 degrees.');
    }"""
new_val = old_val + """
    const pitchLimitDegrees = Number(form.pitch_limit_degrees ?? 0);
    if (!Number.isFinite(pitchLimitDegrees) || pitchLimitDegrees < 0 || pitchLimitDegrees > 180) {
        throw new Error('Pitch limit must be between 0 and 180 degrees.');
    }"""
assert old_val in content, 'validation not found'
content = content.replace(old_val, new_val, 1)

# 4. metadata: add after yaw_limit_degrees
old_meta = "        yaw_limit_degrees: yawLimitDegrees,\n        easing: easing\n    };"
new_meta = ("        yaw_limit_degrees: yawLimitDegrees,\n"
            "        pitch_limit_degrees: pitchLimitDegrees,\n"
            "        easing: easing\n    };")
assert old_meta in content, 'metadata not found'
content = content.replace(old_meta, new_meta, 1)

# 5. import: add after yaw_limit_degrees import line
old_imp = "...(definition.yaw_limit_degrees != null ? { yaw_limit_degrees: definition.yaw_limit_degrees } : {}),"
new_imp = (old_imp + "\n                ...(definition.pitch_limit_degrees != null ? { pitch_limit_degrees: definition.pitch_limit_degrees } : {}),")
assert old_imp in content, 'import not found'
content = content.replace(old_imp, new_imp, 1)

# 6. export: add after yaw_limit_degrees export line
old_exp = '            yaw_limit_degrees: metadata.yaw_limit_degrees ?? 0,'
new_exp = old_exp + '\n            pitch_limit_degrees: metadata.pitch_limit_degrees ?? 0,'
assert old_exp in content, 'export not found'
content = content.replace(old_exp, new_exp, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')