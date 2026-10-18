package com.zoma1101.swordskill.smithing;

/** 武器記憶で数える攻撃の傾向。 */
public enum WeaponMemoryCategory {

    /** 通常斬撃。 */
    NORMAL,
    /** 突き。 */
    THRUST,
    /** 凪払い（横薙ぎ）。 */
    SWEEP,
    /** 飛翔斬撃。 */
    FLYING;

    public static WeaponMemoryCategory byName(String name) {
        if (name == null || name.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return NORMAL;
        }
    }
}
