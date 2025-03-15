package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.*;

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
        }
        return availableWeaponTypes;
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<SkillData.WeaponType> getWeaponTypes() {
        ItemStack mainHandItem = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getMainHandItem() : null;
        ItemStack offHandItem = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getOffhandItem() : null;
        return getWeaponTypes(Objects.requireNonNull(mainHandItem), offHandItem);
    }

    @OnlyIn(Dist.CLIENT)
    private static SkillData.WeaponType getWeaponDetector() {
        Set<SkillData.WeaponType> weaponTypes = getWeaponTypes();
        if (!weaponTypes.isEmpty()) {
            return weaponTypes.iterator().next();
        }
        return null;
    }
    
    private static final Map<UUID, SkillData.WeaponType> playerWeaponTypeMap = new HashMap<>();

    public static void setWeaponType(Player player) {
        SkillData.WeaponType WeaponType =getWeaponDetector();
        playerWeaponTypeMap.put(player.getUUID(), WeaponType);
    }

    @OnlyIn(Dist.CLIENT)
    public static SkillData.WeaponType getWeaponType() {
        LocalPlayer player = Minecraft.getInstance().player;
        return playerWeaponTypeMap.get(Objects.requireNonNull(player).getUUID());
    }
    public static SkillData.WeaponType getWeaponType(ServerPlayer player) {
        return playerWeaponTypeMap.get(player.getUUID());
    }

}
