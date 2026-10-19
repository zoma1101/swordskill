package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 鍛造武器・インゴットのツールチップ表示をまとめる。 */
final class SmithingTooltip {

    private SmithingTooltip() {
    }

    static void append(List<Component> tooltip, ForgedWeaponData data) {
        if (data.mainMaterial() != null) {
            tooltip.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".smithing.tooltip.material",
                    data.mainMaterial().displayName()));
        }
        if (!data.partIds().isEmpty()) {
            tooltip.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".smithing.tooltip.parts",
                    data.partsName()));
        }
        tooltip.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".smithing.tooltip.required_str",
                data.requiredStr()));
        if (data.quality() > 0) {
            tooltip.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".smithing.tooltip.quality",
                    data.quality()));
        }
    }
}
