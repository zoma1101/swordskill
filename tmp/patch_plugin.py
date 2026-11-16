path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\tools\blockbench\swordskill_skill_creator.js'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. values
old = "        hitstop_ticks: existing.hitstop_ticks ?? -1,\n        ground_impact_strength: existing.ground_impact_strength ?? 0\n"
new = ("        hitstop_ticks: existing.hitstop_ticks ?? -1,\n"
       "        ground_impact_strength: existing.ground_impact_strength ?? 0,\n"
       "        player_pitch_affected: existing.player_pitch_affected ?? true,\n"
       "        yaw_limit_degrees: existing.yaw_limit_degrees ?? 0,\n"
       "        easing: existing.easing ?? 'linear'\n")
assert old in content, 'values not found'
content = content.replace(old, new)

# 2. form
old_form = """            ground_impact_strength: {
                label: 'Ground impact strength (0 = off)',
                type: 'number',
                value: values.ground_impact_strength,
                min: 0,
                max: 3,
                step: 0.1,
                description: 'Shakes the ground blocks and camera when a GROUND-triggered event lands.'
            }
        },"""
new_form = """            ground_impact_strength: {
                label: 'Ground impact strength (0 = off)',
                type: 'number',
                value: values.ground_impact_strength,
                min: 0,
                max: 3,
                step: 0.1,
                description: 'Shakes the ground blocks and camera when a GROUND-triggered event lands.'
            },
            player_pitch_affected: {
                label: 'Player pitch affects slash direction',
                type: 'checkbox',
                value: values.player_pitch_affected,
                description: 'When off, the flying slash keeps its fixed rotation regardless of the player look angle.'
            },
            yaw_limit_degrees: {
                label: 'Yaw limit from player facing (0 = no limit)',
                type: 'number',
                value: values.yaw_limit_degrees,
                min: 0,
                max: 360,
                step: 1,
                description: 'Clamps the flying slash yaw relative to the player yaw.'
            },
            easing: {
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
assert old_form in content, 'form not found'
content = content.replace(old_form, new_form)

# 3. validation
old_val = """    const hitstopTicks = Math.floor(Number(form.hitstop_ticks ?? -1));
    if (!Number.isSafeInteger(hitstopTicks) || hitstopTicks < -1 || hitstopTicks > 40) {
        throw new Error('Hit-stop ticks must be -1 (server default), 0 (disabled), or up to 40.');
    }"""
new_val = old_val + """
    const yawLimitDegrees = Number(form.yaw_limit_degrees ?? 0);
    if (!Number.isFinite(yawLimitDegrees) || yawLimitDegrees < 0 || yawLimitDegrees > 360) {
        throw new Error('Yaw limit must be between 0 and 360 degrees.');
    }
    const easing = String(form.easing || 'linear');
    if (!['linear', 'ease_in', 'ease_out', 'ease_in_out'].includes(easing)) {
        throw new Error('Unknown easing type.');
    }"""
assert old_val in content, 'validation not found'
content = content.replace(old_val, new_val)

# 4. metadata
old_meta = '        ground_impact_strength: groundImpactStrength\n    };'
new_meta = ('        ground_impact_strength: groundImpactStrength,\n'
            '        player_pitch_affected: form.player_pitch_affected !== false,\n'
            '        yaw_limit_degrees: yawLimitDegrees,\n'
            '        easing: easing\n    };')
assert old_meta in content, 'metadata not found'
content = content.replace(old_meta, new_meta)

# 5. import
old_imp = "...(definition.hitstop_ticks != null ? { hitstop_ticks: definition.hitstop_ticks } : {}),"
new_imp = (old_imp + "\n                ...(definition.player_pitch_affected != null ? { player_pitch_affected: definition.player_pitch_affected } : {}),\n"
           "                ...(definition.yaw_limit_degrees != null ? { yaw_limit_degrees: definition.yaw_limit_degrees } : {}),\n"
           "                ...(definition.easing != null ? { easing: definition.easing } : {}),")
assert old_imp in content, 'import not found'
content = content.replace(old_imp, new_imp)

# 6. export
old_exp = '            hitstop_ticks: metadata.hitstop_ticks ?? -1,'
new_exp = (old_exp + '\n            player_pitch_affected: metadata.player_pitch_affected ?? true,\n'
           '            yaw_limit_degrees: metadata.yaw_limit_degrees ?? 0,\n'
           '            easing: metadata.easing ?? "linear",')
assert old_exp in content, 'export not found'
content = content.replace(old_exp, new_exp)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')