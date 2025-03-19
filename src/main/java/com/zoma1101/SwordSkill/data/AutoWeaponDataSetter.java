package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Set;

import static com.zoma1101.SwordSkill.data.WeaponTypeUtils.None_WeaponData;

public class AutoWeaponDataSetter {

    public static WeaponData AutoWeaponDataSetting(ItemStack heldItem){
        String itemName = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(heldItem.getItem())).getPath();

        if (itemName.contains("great_sword") || itemName.contains("greatsword") || itemName.contains("claymore")) {
            return new WeaponData(Set.of(SkillData.WeaponType.TWO_HANDED_SWORD),"two_handed_sword");
        } else if (itemName.contains("katana") || itemName.contains("cutlass")) {
            return new WeaponData(Set.of(SkillData.WeaponType.KATANA),"katana");
        } else if (itemName.contains("axe")) {
            return new WeaponData(Set.of(SkillData.WeaponType.AXE),"axe");
        } else if (itemName.contains("rapier")) {
            return new WeaponData(Set.of(SkillData.WeaponType.RAPIER),"rapier");
        } else if (itemName.contains("claw")) {
            return new WeaponData(Set.of(SkillData.WeaponType.CLAW),"claw");
        } else if (itemName.contains("spear") || itemName.contains("trident") || heldItem.getItem() instanceof TridentItem) {
            return new WeaponData(Set.of(SkillData.WeaponType.SPEAR),"spear");
        } else if (itemName.contains("whip")) {
            return new WeaponData(Set.of(SkillData.WeaponType.WHIP),"whip");
        } else if (itemName.contains("scythe")) {
            return new WeaponData(Set.of(SkillData.WeaponType.SCYTHE),"scythe");
        } else if (itemName.contains("dagger") || itemName.contains("short_sword")) {
            return new WeaponData(Set.of(SkillData.WeaponType.DAGGER),"dagger");
        } else if (itemName.contains("sword") || heldItem.getItem() instanceof SwordItem) {
            return new WeaponData(Set.of(SkillData.WeaponType.ONE_HANDED_SWORD),"one_handed_sword");
        }
        return None_WeaponData;
    }

}

