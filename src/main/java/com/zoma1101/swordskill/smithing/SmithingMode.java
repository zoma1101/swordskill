package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

/** 鍛冶台の動作モード。 */
public enum SmithingMode {

    /** 部品と素材から武器を作る。 */
    CREATE,
    /** 武器を強化する。 */
    ENHANCE,
    /** 武器を溶かしてインゴットにする。 */
    MELT,
    /** インゴットと素材から鍛造し直す（武器記憶を引き継ぐ）。 */
    REFORGE;

    public String translationKey() {
        return "gui." + SwordSkill.MOD_ID + ".smithing.mode." + name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public static SmithingMode byId(int id) {
        return values()[Mth.clamp(id, 0, values().length - 1)];
    }

    public static SmithingMode byName(String name) {
        if (name == null || name.isBlank()) {
            return CREATE;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CREATE;
        }
    }
}
