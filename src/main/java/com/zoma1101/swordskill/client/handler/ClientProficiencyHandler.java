package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

/** サーバーから同期された熟練度を保持するクライアント側キャッシュ。 */
@OnlyIn(Dist.CLIENT)
public class ClientProficiencyHandler {

    private static final int DEFAULT_POINTS_PER_LEVEL = 100;

    private static Map<SkillData.WeaponType, Integer> proficiencies = new EnumMap<>(SkillData.WeaponType.class);
    private static Map<SkillData.WeaponType, Integer> spent = new EnumMap<>(SkillData.WeaponType.class);
    private static int pointsPerLevel = DEFAULT_POINTS_PER_LEVEL;
    private static boolean synced;

    private ClientProficiencyHandler() {
    }

    public static void update(String[] categories, int[] values, int[] spentPoints, int perLevel) {
        Map<SkillData.WeaponType, Integer> updatedProficiencies = new EnumMap<>(SkillData.WeaponType.class);
        Map<SkillData.WeaponType, Integer> updatedSpent = new EnumMap<>(SkillData.WeaponType.class);
        int limit = Math.min(categories.length, Math.min(values.length, spentPoints.length));
        for (int index = 0; index < limit; index++) {
            SkillData.WeaponType category = parse(categories[index]);
            if (category == null) {
                continue;
            }
            if (values[index] > 0) {
                updatedProficiencies.put(category, values[index]);
            }
            if (spentPoints[index] > 0) {
                updatedSpent.put(category, spentPoints[index]);
            }
        }
        proficiencies = updatedProficiencies;
        spent = updatedSpent;
        pointsPerLevel = Math.max(1, perLevel);
        synced = true;
    }

    private static SkillData.WeaponType parse(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            SkillData.WeaponType category = SkillData.WeaponType.valueOf(name);
            return category == SkillData.WeaponType.NONE ? null : category;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static boolean hasData() {
        return synced;
    }

    public static int getPointsPerLevel() {
        return pointsPerLevel;
    }

    public static int getProficiency(SkillData.WeaponType category) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        return proficiencies.getOrDefault(category, 0);
    }

    public static int getSpentPoints(SkillData.WeaponType category) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        return spent.getOrDefault(category, 0);
    }

    /** 使えるレベルアップポイント。 */
    public static int getAvailablePoints(SkillData.WeaponType category) {
        int earned = getProficiency(category) / pointsPerLevel;
        return Math.max(0, earned - getSpentPoints(category));
    }

    /** 次のレベルアップポイントまでの進捗。 */
    public static int getProgress(SkillData.WeaponType category) {
        return getProficiency(category) % pointsPerLevel;
    }

    public static void reset() {
        proficiencies = new EnumMap<>(SkillData.WeaponType.class);
        spent = new EnumMap<>(SkillData.WeaponType.class);
        pointsPerLevel = DEFAULT_POINTS_PER_LEVEL;
        synced = false;
    }
}
