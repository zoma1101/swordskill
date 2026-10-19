package com.zoma1101.swordskill.smithing;

import java.util.Locale;

/**
 * 5×5 パターンのマスに割り当てる役割。
 *
 * <p>役割ごとに、置かれた素材の {@code guard} / {@code handle} / {@code blades} から
 * 使う部品テクスチャが決まる。</p>
 */
public enum SmithingRole {

    /** 刀身。置いた素材が武器の主素材（強さ・メインカラー）になる。 */
    BLADE,
    /** 鍔。 */
    GUARD,
    /** 柄。 */
    HANDLE;

    /** 名前から引く（不明なら {@code null}）。 */
    public static SmithingRole byName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
