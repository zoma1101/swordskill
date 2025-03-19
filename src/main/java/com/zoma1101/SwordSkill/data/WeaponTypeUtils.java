package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.config.ServerConfig;
import com.zoma1101.SwordSkill.effects.SwordSkillAttribute;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.*;



public class WeaponTypeUtils {
    public static final WeaponData None_WeaponData = new WeaponData(Set.of(SkillData.WeaponType.NONE),null);

    private static Set<SkillData.WeaponType> getWeaponTypes(ItemStack mainHandItem) {
        Set<SkillData.WeaponType> availableWeaponTypes = new HashSet<>();

        if (mainHandItem.isEmpty()) {
            return availableWeaponTypes;
        }
        availableWeaponTypes.addAll(WeaponTypeDetector.detectWeaponTypes(mainHandItem));

        return availableWeaponTypes;
    }

    private static WeaponData DualSwordSetter(Player player){

        ItemStack mainHandItem = player.getMainHandItem();
        ItemStack offHandItem = player.getOffhandItem();

        if (!mainHandItem.isEmpty() && !offHandItem.isEmpty()) {
            Set<SkillData.WeaponType> availableWeaponTypes = new HashSet<>(WeaponTypeDetector.detectWeaponTypes(mainHandItem));
            Set<SkillData.WeaponType> offHandAvailableWeaponTypes = new HashSet<>(WeaponTypeDetector.detectWeaponTypes(offHandItem));
            boolean mainHandOneHandedSword = availableWeaponTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD);
            boolean offHandOneHandedSword = offHandAvailableWeaponTypes.contains(SkillData.WeaponType.ONE_HANDED_SWORD);
            boolean mainHandClaw = availableWeaponTypes.contains(SkillData.WeaponType.CLAW);
            boolean offHandClaw = offHandAvailableWeaponTypes.contains(SkillData.WeaponType.CLAW);

            if (mainHandOneHandedSword && offHandOneHandedSword) {
                // 武器種をまとめる
                availableWeaponTypes.addAll(offHandAvailableWeaponTypes);
                // DUAL_SWORD を追加
                availableWeaponTypes.add(SkillData.WeaponType.DUALSWORD);
                // 他の武器種が両手に共通する場合も残すことを考慮し、重複を削除
                Set<SkillData.WeaponType> combinedTypes = new HashSet<>(availableWeaponTypes);
                availableWeaponTypes.clear();
                availableWeaponTypes.addAll(combinedTypes);
                return new WeaponData(availableWeaponTypes,"dual_sword");
            }
            if (mainHandClaw && offHandClaw) {
                float default_cooldown = (float) player.getAttributeValue(SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get());
                Objects.requireNonNull(player.getAttribute(SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get())).setBaseValue(default_cooldown-0.5);
                return new WeaponData(availableWeaponTypes,"dual_claw");
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<SkillData.WeaponType> getWeaponTypes() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return Collections.emptySet();
        }
        ItemStack mainHandItem = player.getMainHandItem();
        return getWeaponTypes(mainHandItem);
    }

    @OnlyIn(Dist.CLIENT)
    private static WeaponData getWeaponDataDetector() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
        Set<SkillData.WeaponType> weaponTypes = getWeaponTypes();
        if (!weaponTypes.isEmpty()) {
            WeaponData DualSwordData = DualSwordSetter(player);
            if (DualSwordData==null){
                String weaponName = WeaponTypeDetector.getWeaponName(player.getMainHandItem());
                return new WeaponData(weaponTypes, weaponName);
            }
            else {
                return DualSwordData;
            }
        }
        else if (ServerConfig.AUTOWEAPON_SETTING.get()) {
            return AutoWeaponDataSetter.AutoWeaponDataSetting(player.getMainHandItem());
        } //Configでtureなら仕様をONにする。
        }return null;
    }

    private static final Map<UUID, WeaponData> playerWeaponDataMap = new HashMap<>();

    public static void setWeaponType(Player player) {
        WeaponData weaponData = getWeaponDataDetector();
        // weaponData が null の可能性
        playerWeaponDataMap.put(player.getUUID(), Objects.requireNonNullElse(weaponData, None_WeaponData));
    }

    @OnlyIn(Dist.CLIENT)
    public static WeaponData getWeaponData() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return playerWeaponDataMap.get(player.getUUID());
    }

    public static WeaponData getWeaponData(ServerPlayer player) {
        return playerWeaponDataMap.get(player.getUUID());
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<SkillData.WeaponType> getWeaponType() {
        WeaponData weaponData = getWeaponData();
        // weaponData が null の可能性
        if (weaponData != null) {
            return weaponData.getWeaponType();
        }
        return Collections.emptySet(); // null を返さないようにする
    }

    @OnlyIn(Dist.CLIENT)
    public static String getWeaponName() {
        WeaponData weaponData = getWeaponData();
        // weaponData が null の可能性
        if (weaponData != null) {
            return weaponData.getWeaponName();
        }
        return null; // null を返す可能性を残す (呼び出し元で null チェックが必要)
    }

    public static Set<SkillData.WeaponType> getWeaponType(ServerPlayer player) {
        WeaponData weaponData = getWeaponData(player);
        // weaponData が null の可能性
        if (weaponData != null) {
            return weaponData.getWeaponType();
        }
        return Collections.emptySet(); // null を返さないようにする
    }
}