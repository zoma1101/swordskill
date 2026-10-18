package com.zoma1101.swordskill.smithing;

/** 鍛造武器を構成する部品の種類。 */
public enum SmithingPartKind {

    /** 刀身。武器種・攻撃力・攻撃速度の主体になる。 */
    BLADE,
    /** 鍔・ガード。省略可能。 */
    GUARD,
    /** 柄。 */
    HANDLE;

    public static SmithingPartKind byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
