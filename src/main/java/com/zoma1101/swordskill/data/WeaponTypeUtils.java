package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeaponTypeUtils {
    public static final WeaponData NONE_WEAPON_DATA = new WeaponData(Set.of(SkillData.WeaponType.NONE), null);

    private static final Map<UUID, WeaponData> playerWeaponDataMap = new ConcurrentHashMap<>();


    public static void clearPlayerCache(Player player) {
        playerWeaponDataMap.remove(player.getUUID());
    }

    private static Set<SkillData.WeaponType> getWeaponTypes(ItemStack mainHandItem) {
        if (mainHandItem.isEmpty()) {
            return new HashSet<>();
        }
        // detectWeaponTypesがSetを返すと仮定して、変更可能な新しいSetを作成
        return new HashSet<>(WeaponTypeDetector.detectWeaponTypes(mainHandItem));
    }


    private static WeaponData determineDualWielding(Player player) {
        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        if (mainHandItem.isEmpty() || offHandItem.isEmpty()) {
            return null;
        }

        Set<SkillData.WeaponType> mainTypes = getWeaponTypes(mainHandItem);
        Set<SkillData.WeaponType> offHandTypes = getWeaponTypes(offHandItem);

        boolean mainHandOneHanded = mainTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD);
        boolean offHandOneHanded = offHandTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD);

        if (mainHandOneHanded && offHandOneHanded) {
            // 両手の武器種を統合
            mainTypes.addAll(offHandTypes);
            mainTypes.add(SkillData.WeaponType.DUALSWORD);
            return new WeaponData(mainTypes, "dual_sword");
        }

        boolean mainHandClaw = mainTypes.contains(SkillData.WeaponType.CLAW);
        boolean offHandClaw = offHandTypes.contains(SkillData.WeaponType.CLAW);

        if (mainHandClaw && offHandClaw) {
            // Attributeの変更処理はここで行わず、WeaponDataを受け取った呼び出し元やイベントで行うべき
            mainTypes.addAll(offHandTypes);
            return new WeaponData(mainTypes, "dual_claw");
        }

        return null;
    }

    private static WeaponData detectWeaponData(Player player) {
        if (player == null) return null;

        Set<SkillData.WeaponType> weaponTypes = getWeaponTypes(player.getMainHandItem());

        if (!weaponTypes.isEmpty()) {
            // 二刀流チェック
            WeaponData dualSwordData = determineDualWielding(player);
            if (dualSwordData != null) {
                return dualSwordData;
            }
            // 通常武器
            String weaponName = WeaponTypeDetector.getWeaponName(player.getMainHandItem());
            return new WeaponData(weaponTypes, weaponName);
        }
        // 自動判定フォールバック
        else if (ServerConfig.AUTOWEAPON_SETTING.get()) {
            return AutoWeaponDataSetter.AutoWeaponDataSetting(player.getMainHandItem());
        }

        return null;
    }

    public static void setWeaponType(Player player) {
        WeaponData weaponData = detectWeaponData(player);
        playerWeaponDataMap.put(player.getUUID(), Objects.requireNonNullElse(weaponData, NONE_WEAPON_DATA));
    }

    public static WeaponData getWeaponData(ServerPlayer player) {
        return playerWeaponDataMap.getOrDefault(player.getUUID(), NONE_WEAPON_DATA);
    }

    // 以下、Getter類
    public static String getWeaponName(ServerPlayer player) {
        WeaponData data = getWeaponData(player);
        return data != null ? data.weaponName() : null;
    }

    public static Set<SkillData.WeaponType> getWeaponType(ServerPlayer player) {
        WeaponData data = getWeaponData(player);
        return data != null ? data.weaponType() : Collections.emptySet();
    }
}