package com.zoma1101.swordskill.smithing;

/** 部品テクスチャに素材の色1 / 色2 のどちらを乗せるか。 */
public enum SmithingColorSlot {

    /** メインカラー（素材の {@code color_main}）。 */
    MAIN,
    /** サブカラー（素材の {@code color_sub}）。 */
    SUB,
    /** 着色しない（テクスチャそのまま）。 */
    NONE;

    public static SmithingColorSlot byName(String name) {
        if (name == null || name.isBlank()) {
            return MAIN;
        }
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return MAIN;
        }
    }
}
