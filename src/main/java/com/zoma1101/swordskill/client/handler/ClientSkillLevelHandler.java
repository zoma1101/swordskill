package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/** サーバーから同期されたスキルレベルと倍率カーブを保持するクライアント側キャッシュ。 */
@OnlyIn(Dist.CLIENT)
public class ClientSkillLevelHandler {

    private static final int DEFAULT_MAX_LEVEL = 10;
    private static final double DEFAULT_MIN_MULTIPLIER = 1.0D;
    private static final double DEFAULT_MAX_MULTIPLIER = 3.0D;

    private static Map<Integer, Integer> levels = new HashMap<>();
    private static int maxLevel = DEFAULT_MAX_LEVEL;
    private static double minMultiplier = DEFAULT_MIN_MULTIPLIER;
    private static double maxMultiplier = DEFAULT_MAX_MULTIPLIER;
    private static boolean synced;

    private ClientSkillLevelHandler() {
    }

    public static void update(int[] skillIds, int[] skillLevels, int max, double min, double maxMul) {
        Map<Integer, Integer> updated = new HashMap<>();
        int limit = Math.min(skillIds.length, skillLevels.length);
        for (int index = 0; index < limit; index++) {
            if (skillLevels[index] > 1) {
                updated.put(skillIds[index], skillLevels[index]);
            }
        }
        levels = updated;
        maxLevel = Math.max(1, max);
        minMultiplier = min;
        maxMultiplier = maxMul;
        synced = true;
    }

    public static boolean hasData() {
        return synced;
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    /** レベルが未同期の場合はサーバー既定のカーブで代用する。 */
    public static int getLevel(int skillId) {
        return Math.min(maxLevel, Math.max(1, levels.getOrDefault(skillId, 1)));
    }

    public static double getMultiplier(int skillId) {
        return SkillLevelManager.multiplierForLevel(getLevel(skillId), maxLevel, minMultiplier, maxMultiplier);
    }

    public static double getMultiplierForLevel(int level) {
        return SkillLevelManager.multiplierForLevel(level, maxLevel, minMultiplier, maxMultiplier);
    }

    public static void reset() {
        levels = new HashMap<>();
        maxLevel = DEFAULT_MAX_LEVEL;
        minMultiplier = DEFAULT_MIN_MULTIPLIER;
        maxMultiplier = DEFAULT_MAX_MULTIPLIER;
        synced = false;
    }
}
