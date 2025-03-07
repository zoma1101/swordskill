package com.zoma.SwordSkill.data;

import com.zoma.SwordSkill.swordskills.SkillData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import java.util.Set;

public class WeaponTypeDetector {

    public static Set<SkillData.WeaponType> detectWeaponTypes(ItemStack heldItem) {
        if (heldItem.getItem() instanceof SwordItem) {
            return Set.of(SkillData.WeaponType.ONE_HANDED_SWORD);
        } else if (heldItem.getItem() instanceof AxeItem) {
            return Set.of(SkillData.WeaponType.AXE);
        } else if (heldItem.getItem() instanceof TridentItem) {
            return Set.of(SkillData.WeaponType.SPEAR);
        } else {
            return Set.of();
        }
    }
}