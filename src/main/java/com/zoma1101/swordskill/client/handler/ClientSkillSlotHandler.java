package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientSkillSlotHandler {
    private static int[] currentSkillIds = new int[]{-1, -1, -1, -1, -1}; // 初期値を設定
    private static String currentWeaponName = "None";
    private static Set<SkillData.WeaponType> currentWeaponTypes = Collections.emptySet();

    public static void updateAllInfo(int[] skillIds, String weaponName, Set<SkillData.WeaponType> weaponTypes) {
        ClientSkillSlotHandler.currentSkillIds = skillIds;
        ClientSkillSlotHandler.currentWeaponName = (weaponName != null) ? weaponName : "None";
        ClientSkillSlotHandler.currentWeaponTypes = (weaponTypes != null) ? weaponTypes : Collections.emptySet(); // nullチェック
    }

    // HUDなどからスキルID配列を取得するためのメソッド
    public static int[] getSkillSlotInfo() {
        return currentSkillIds;
    }

    // HUDなどから現在の武器名を取得するためのメソッド
    public static String getCurrentWeaponName() {
        return currentWeaponName;
    }

    // 現在の武器タイプセットを取得するための新しいメソッド
    public static Set<SkillData.WeaponType> getCurrentWeaponTypes() {
        return currentWeaponTypes;
    }

}