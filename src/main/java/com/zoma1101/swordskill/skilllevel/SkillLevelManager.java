package com.zoma1101.swordskill.skilllevel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.payload.SyncSkillLevelsPayload;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * スキルレベルの保管・同期・ダメージ倍率を扱う。
 *
 * <p>レベルはプレイヤーごと・スキルごとに 1〜{@code skillLevelMax} の範囲で保持し、
 * {@code swordskill_data/<uuid>.json} 内の {@code skilllevels} オブジェクトに保存する。
 * 攻撃倍率は Lv1 で {@code skillLevelMinMultiplier}、最大レベルで
 * {@code skillLevelMaxMultiplier} になるよう線形に補間する。
 */
public final class SkillLevelManager {

    public static final String STORAGE_KEY = "skilllevels";

    private static final Map<UUID, Map<Integer, Integer>> LEVELS = new HashMap<>();

    private SkillLevelManager() {
    }

    // --- 倍率カーブ -----------------------------------------------------

    /** レベルとカーブのパラメータから攻撃倍率を求める純関数（クライアントでも同じ式を使う）。 */
    public static double multiplierForLevel(int level, int maxLevel, double minMultiplier, double maxMultiplier) {
        int clampedMax = Math.max(1, maxLevel);
        int clampedLevel = Math.max(1, Math.min(level, clampedMax));
        if (clampedMax == 1) {
            return maxMultiplier;
        }
        double progress = (clampedLevel - 1) / (double) (clampedMax - 1);
        return minMultiplier + (maxMultiplier - minMultiplier) * progress;
    }

    public static int maxLevel() {
        return ServerConfig.skillLevelMax.get();
    }

    public static double minMultiplier() {
        return ServerConfig.skillLevelMinMultiplier.get();
    }

    public static double maxMultiplier() {
        return ServerConfig.skillLevelMaxMultiplier.get();
    }

    public static double multiplierForLevel(int level) {
        return multiplierForLevel(level, maxLevel(), minMultiplier(), maxMultiplier());
    }

    // --- キャッシュ -----------------------------------------------------

    private static Map<Integer, Integer> levels(ServerPlayer player) {
        return LEVELS.computeIfAbsent(player.getUUID(), ignored -> readFromDisk(player));
    }

    private static Map<Integer, Integer> readFromDisk(ServerPlayer player) {
        Map<Integer, Integer> result = new LinkedHashMap<>();
        JsonObject playerData = DataManager.loadPlayerData(player);
        if (playerData.has(STORAGE_KEY) && playerData.get(STORAGE_KEY).isJsonObject()) {
            JsonObject stored = playerData.getAsJsonObject(STORAGE_KEY);
            for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
                try {
                    int skillId = Integer.parseInt(entry.getKey());
                    int level = entry.getValue().getAsInt();
                    if (skillId >= 0 && level > 1) {
                        result.put(skillId, level);
                    }
                } catch (RuntimeException ignored) {
                    // 壊れたエントリは無視する
                }
            }
        }
        return result;
    }

    public static void loadPlayer(ServerPlayer player) {
        LEVELS.put(player.getUUID(), readFromDisk(player));
    }

    public static void unloadPlayer(UUID playerId) {
        LEVELS.remove(playerId);
    }

    // --- 取得・変更 -----------------------------------------------------

    public static int getLevel(ServerPlayer player, int skillId) {
        int max = maxLevel();
        return Math.min(max, Math.max(1, levels(player).getOrDefault(skillId, 1)));
    }

    /** 保存済みのレベルを返す（レベル1のスキルは含まない）。 */
    public static Map<Integer, Integer> getStoredLevels(ServerPlayer player) {
        return Map.copyOf(levels(player));
    }

    /** レベルを設定する。戻り値は実際に適用されたレベル。 */
    public static int setLevel(ServerPlayer player, int skillId, int level) {
        int clamped = Math.min(maxLevel(), Math.max(1, level));
        Map<Integer, Integer> playerLevels = levels(player);
        if (clamped <= 1) {
            playerLevels.remove(skillId);
        } else {
            playerLevels.put(skillId, clamped);
        }
        saveToDisk(player, playerLevels);
        return clamped;
    }

    /** レベルを加算する。戻り値は実際に適用されたレベル。 */
    public static int addLevel(ServerPlayer player, int skillId, int delta) {
        return setLevel(player, skillId, getLevel(player, skillId) + delta);
    }

    private static void saveToDisk(ServerPlayer player, Map<Integer, Integer> playerLevels) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonObject stored = new JsonObject();
        for (Map.Entry<Integer, Integer> entry : playerLevels.entrySet()) {
            stored.addProperty(Integer.toString(entry.getKey()), entry.getValue());
        }
        playerData.add(STORAGE_KEY, stored);
        DataManager.savePlayerData(player, playerData);
    }

    // --- ダメージへの適用 -----------------------------------------------

    /** いま実行中のスキル（無ければ 1.0）のレベル倍率を返す。 */
    public static double damageMultiplier(ServerPlayer player) {
        int runningSkillId = SkillExecutionManager.getRunningSkillId(player);
        if (runningSkillId < 0) {
            return 1.0D;
        }
        return multiplierForLevel(getLevel(player, runningSkillId));
    }

    // --- クライアント同期 -------------------------------------------------

    /** クライアント表示用の同期パケットを組み立てる。レベル1のスキルは省略する。 */
    public static SyncSkillLevelsPayload buildSyncPayload(ServerPlayer player) {
        Map<Integer, Integer> stored = getStoredLevels(player);
        int[] skillIds = new int[stored.size()];
        int[] levels = new int[stored.size()];
        int index = 0;
        for (Map.Entry<Integer, Integer> entry : stored.entrySet()) {
            skillIds[index] = entry.getKey();
            levels[index] = entry.getValue();
            index++;
        }
        return new SyncSkillLevelsPayload(skillIds, levels, maxLevel(), minMultiplier(), maxMultiplier());
    }
}
