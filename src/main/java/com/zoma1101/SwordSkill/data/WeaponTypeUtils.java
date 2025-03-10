package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import java.util.HashSet;
import java.util.Set;

public class WeaponTypeUtils {

    private static Set<SkillData.WeaponType> getWeaponTypes(ItemStack mainHandItem, ItemStack offHandItem) {
        Set<SkillData.WeaponType> availableWeaponTypes = new HashSet<>();
        if (mainHandItem.isEmpty()) {
            return availableWeaponTypes;
        }
        availableWeaponTypes.addAll(WeaponTypeDetector.detectWeaponTypes(mainHandItem));

        if (offHandItem.isEmpty()) {
            return availableWeaponTypes;
        }

        if (mainHandItem.getItem() instanceof SwordItem && offHandItem.getItem() instanceof SwordItem) {
            Set<SkillData.WeaponType> mainHandTypes = WeaponTypeDetector.detectWeaponTypes(mainHandItem);
            Set<SkillData.WeaponType> offHandTypes = WeaponTypeDetector.detectWeaponTypes(offHandItem);
            if (mainHandTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD) && offHandTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD)
                    || mainHandTypes.contains(SkillData.WeaponType.DAGGER) && offHandTypes.contains(SkillData.WeaponType.DAGGER)
                    || mainHandTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD) && offHandTypes.contains(SkillData.WeaponType.DAGGER)
                    || mainHandTypes.contains(SkillData.WeaponType.DAGGER) && offHandTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD)) {
                availableWeaponTypes = Set.of(SkillData.WeaponType.DUALSWORD);
            }
        } else if (mainHandItem.getItem() instanceof ShieldItem) {
            availableWeaponTypes.addAll(WeaponTypeDetector.detectWeaponTypes(offHandItem));
            availableWeaponTypes.add(SkillData.WeaponType.SHIELD);
        } else if (offHandItem.getItem() instanceof ShieldItem) {
            availableWeaponTypes.addAll(WeaponTypeDetector.detectWeaponTypes(mainHandItem));
            availableWeaponTypes.add(SkillData.WeaponType.SHIELD);
        }
        return availableWeaponTypes;
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<SkillData.WeaponType> getWeaponTypes() {
        ItemStack mainHandItem = Minecraft.getInstance().player.getMainHandItem();
        ItemStack offHandItem = Minecraft.getInstance().player.getOffhandItem();
        return getWeaponTypes(mainHandItem, offHandItem);
    }

    public static Set<SkillData.WeaponType> getWeaponTypes(ServerPlayer player) {
        if (EffectiveSide.get() == LogicalSide.SERVER) {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            return getWeaponTypes(mainHandItem, offHandItem);
        } else {
            return new HashSet<>();
        }
    }

    public static SkillData.WeaponType getWeaponType(ServerPlayer player) {
        Set<SkillData.WeaponType> weaponTypes = getWeaponTypes(player);
        if (!weaponTypes.isEmpty()) {
            return weaponTypes.iterator().next();
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static SkillData.WeaponType getWeaponType() {
        Set<SkillData.WeaponType> weaponTypes = getWeaponTypes();
        if (!weaponTypes.isEmpty()) {
            return weaponTypes.iterator().next();
        }
        return null;
    }
}
