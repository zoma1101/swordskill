package com.zoma1101.swordskill.playerlevel;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.chat.Component;

/**
 * ステータス割り振りの対象。
 *
 * <ul>
 *   <li>{@link #STR} … 攻撃力（ソードスキルの独自ダメージ式に組み込む）。将来的に鍛冶システムと連動して
 *       装備重量の緩和も担当する予定。</li>
 *   <li>{@link #AGI} … 移動速度</li>
 * </ul>
 */
public enum StatType {

    STR,
    AGI;

    /** 表示名の翻訳キー。 */
    public String translationKey() {
        return "stat." + SwordSkill.MOD_ID + "." + name().toLowerCase();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    /** 効果説明の翻訳キー。 */
    public String descriptionKey() {
        return "stat." + SwordSkill.MOD_ID + "." + name().toLowerCase() + ".desc";
    }

    public Component description() {
        return Component.translatable(descriptionKey());
    }

    public static StatType byName(String name) {
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
