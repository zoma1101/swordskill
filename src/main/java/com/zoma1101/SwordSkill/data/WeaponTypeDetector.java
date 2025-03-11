package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;


import java.util.HashSet;
import java.util.Set;

public class WeaponTypeDetector {


    public static Set<SkillData.WeaponType> detectWeaponTypes(ItemStack heldItem) {
        Set<SkillData.WeaponType> weaponTypes = new HashSet<>();
        String itemName = String.valueOf(ForgeRegistries.ITEMS.getKey(heldItem.getItem()));

        if (itemName.contains("two-handed sword") || itemName.contains("greatsword")) {
            weaponTypes.add(SkillData.WeaponType.TWO_HANDED_SWORD);
        } else if (itemName.contains("katana")) {
            weaponTypes.add(SkillData.WeaponType.KATANA);
        } else if (itemName.contains("axe")) {
            weaponTypes.add(SkillData.WeaponType.AXE);
        } else if (itemName.contains("rapier")) {
            weaponTypes.add(SkillData.WeaponType.RAPIER);
        } else if (itemName.contains("mace")) {
            weaponTypes.add(SkillData.WeaponType.MACE);
        } else if (itemName.contains("claw")) {
            weaponTypes.add(SkillData.WeaponType.ONE_HANDED_CLAW);
        } else if (itemName.contains("spear") || itemName.contains("trident") || heldItem.getItem() instanceof TridentItem) {
            weaponTypes.add(SkillData.WeaponType.SPEAR);
        } else if (itemName.contains("whip")) {
            weaponTypes.add(SkillData.WeaponType.WHIP);
        } else if (itemName.contains("scythe")) {
            weaponTypes.add(SkillData.WeaponType.SCYTHE);
        } else if (itemName.contains("dagger") || itemName.contains("short_sword") ) {
            weaponTypes.add(SkillData.WeaponType.DAGGER);
        } else if (itemName.contains("sword")) {
            weaponTypes.add(SkillData.WeaponType.ONE_HANDED_SWORD);
        }

        //else if (heldItem.getItem() instanceof SwordItem) {weaponTypes.add(SkillData.WeaponType.ONE_HANDED_SWORD);}

        return weaponTypes;
    }
}