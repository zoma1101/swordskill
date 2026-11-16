path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\blockbench\swordskill_skill_creator.js'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. values
old = "        yaw_limit_degrees: existing.yaw_limit_degrees ?? 0,\n        pitch_limit_degrees: existing.pitch_limit_degrees ?? 0,\n        easing: existing.easing ?? 'linear'\n"
new = "        yaw_limit_degrees: existing.yaw_limit_degrees ?? 0,\n        pitch_limit_degrees: existing.pitch_limit_degrees ?? 0\n"
assert old in content, 'values not found'
content = content.replace(old, new, 1)

# 2. form: remove easing field
old_form = """            easing: {
                label: 'Movement easing',
                type: 'select',
                value: values.easing,
                options: {
                    linear: 'Linear',
                    ease_in: 'Ease In',
                    ease_out: 'Ease Out',
                    ease_in_out: 'Ease In-Out'
                },
                description: 'Easing curve applied to the flying slash movement.'
            }
        },"""
new_form = """        },"""
assert old_form in content, 'form not found'
content = content.replace(old_form, new_form, 1)

# 3. validation: remove easing validation
old_val = """
    const easing = String(form.easing || 'linear');
    if (!['linear', 'ease_in', 'ease_out', 'ease_in_out'].includes(easing)) {
        throw new Error('Unknown easing type.');
    }"""
assert old_val in content, 'validation not found'
content = content.replace(old_val, '', 1)

# 4. metadata: remove easing
old_meta = "        yaw_limit_degrees: yawLimitDegrees,\n        pitch_limit_degrees: pitchLimitDegrees,\n        easing: easing\n    };"
new_meta = "        yaw_limit_degrees: yawLimitDegrees,\n        pitch_limit_degrees: pitchLimitDegrees\n    };"
assert old_meta in content, 'metadata not found'
content = content.replace(old_meta, new_meta, 1)

# 5. import: remove easing import line
old_imp = "...(definition.pitch_limit_degrees != null ? { pitch_limit_degrees: definition.pitch_limit_degrees } : {}),\n                ...(definition.easing != null ? { easing: definition.easing } : {}),"
new_imp = "...(definition.pitch_limit_degrees != null ? { pitch_limit_degrees: definition.pitch_limit_degrees } : {}),"
assert old_imp in content, 'import not found'
content = content.replace(old_imp, new_imp, 1)

# 6. export: remove easing export line
old_exp = '            pitch_limit_degrees: metadata.pitch_limit_degrees ?? 0,\n            easing: metadata.easing ?? "linear",'
new_exp = '            pitch_limit_degrees: metadata.pitch_limit_degrees ?? 0,'
assert old_exp in content, 'export not found'
content = content.replace(old_exp, new_exp, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')