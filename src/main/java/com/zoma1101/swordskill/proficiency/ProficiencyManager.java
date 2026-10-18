package com.zoma1101.swordskill.proficiency;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.payload.SyncProficiencyPayload;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 熟練度（プロフィシエンシー）の管理。
 *
 * <p>熟練度は「武器種などのカテゴリ」ごとに貯まる。既定では
 * カテゴリのスキルでモンスターを撃破したときに上昇する（料理や鍛冶など、
 * 別カテゴリの上昇条件は {@link #addProficiency(ServerPlayer, SkillData.WeaponType, int)}
 * を呼ぶ側で実装する）。
 *
 * <p>{@code proficiencyPerLevelUp}（既定 100）貯まるごとに
 * そのカテゴリのスキルを1つレベルアップできるポイントが手に入る。
 * ポイントは「獲得済み - 使用済み」で求めるため、レベルアップ以外では消費されない。
 *
 * <p>保存先は {@code swordskill_data/<uuid>.json} の
 * {@value #PROFICIENCY_KEY} / {@value #SPENT_KEY} オブジェクト。
 */
public final class ProficiencyManager {

    public static final String PROFICIENCY_KEY = "proficiencies";
    public static final String SPENT_KEY = "proficiency_spent";

    private static final Map<UUID, Map<SkillData.WeaponType, Integer>> PROFICIENCY = new HashMap<>();
    private static final Map<UUID, Map<SkillData.WeaponType, Integer>> SPENT = new HashMap<>();

    private ProficiencyManager() {
    }

    // --- 設定 -----------------------------------------------------------

    /** レベルアップ1回に必要な熟練度。 */
    public static int pointsPerLevel() {
        return Math.max(1, ServerConfig.proficiencyPerLevelUp.get());
    }

    public static int proficiencyPerKill() {
        return Math.max(0, ServerConfig.proficiencyPerKill.get());
    }

    // --- カテゴリ -------------------------------------------------------

    /** スキルの熟練度カテゴリ（JSON 未指定なら先頭の武器種）。 */
    public static SkillData.WeaponType categoryOf(SkillData skill) {
        return skill == null ? SkillData.WeaponType.NONE : skill.getProficiencyCategory();
    }

    public static SkillData.WeaponType categoryOf(int skillId) {
        return categoryOf(SwordSkillRegistry.SKILLS.get(skillId));
    }

    /** そのスキルの解放に必要な熟練度。 */
    public static int unlockRequirementOf(SkillData skill) {
        return skill == null ? 0 : skill.getUnlockProficiency();
    }

    // --- キャッシュ -----------------------------------------------------

    private static Map<SkillData.WeaponType, Integer> mapOf(UUID playerId,
            Map<UUID, Map<SkillData.WeaponType, Integer>> store) {
        return store.computeIfAbsent(playerId, ignored -> new EnumMap<>(SkillData.WeaponType.class));
    }

    private static Map<SkillData.WeaponType, Integer> proficiencyMap(ServerPlayer player) {
        return mapOf(player.getUUID(), PROFICIENCY);
    }

    private static Map<SkillData.WeaponType, Integer> spentMap(ServerPlayer player) {
        return mapOf(player.getUUID(), SPENT);
    }

    /** ディスクから読み込む。 */
    public static void loadPlayer(ServerPlayer player) {
        Map<SkillData.WeaponType, Integer> proficiency = new EnumMap<>(SkillData.WeaponType.class);
        Map<SkillData.WeaponType, Integer> spent = new EnumMap<>(SkillData.WeaponType.class);
        JsonObject playerData = DataManager.loadPlayerData(player);
        readInto(playerData, PROFICIENCY_KEY, proficiency);
        readInto(playerData, SPENT_KEY, spent);
        PROFICIENCY.put(player.getUUID(), proficiency);
        SPENT.put(player.getUUID(), spent);
    }

    private static void readInto(JsonObject playerData, String key, Map<SkillData.WeaponType, Integer> target) {
        if (!playerData.has(key) || !playerData.get(key).isJsonObject()) {
            return;
        }
        JsonObject stored = playerData.getAsJsonObject(key);
        for (Map.Entry<String, JsonElement> entry : stored.entrySet()) {
            SkillData.WeaponType category = parseCategory(entry.getKey());
            if (category == null) {
                continue;
            }
            try {
                int value = entry.getValue().getAsInt();
                if (value > 0) {
                    target.put(category, value);
                }
            } catch (RuntimeException ignored) {
                // 壊れたエントリは無視する
            }
        }
    }

    private static SkillData.WeaponType parseCategory(String name) {
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

    public static void unloadPlayer(UUID playerId) {
        PROFICIENCY.remove(playerId);
        SPENT.remove(playerId);
    }

    private static void save(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        playerData.add(PROFICIENCY_KEY, toJson(proficiencyMap(player)));
        playerData.add(SPENT_KEY, toJson(spentMap(player)));
        DataManager.savePlayerData(player, playerData);
    }

    private static JsonObject toJson(Map<SkillData.WeaponType, Integer> values) {
        JsonObject json = new JsonObject();
        for (Map.Entry<SkillData.WeaponType, Integer> entry : values.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                json.addProperty(entry.getKey().name(), entry.getValue());
            }
        }
        return json;
    }

    // --- 取得・変更 -----------------------------------------------------

    public static int getProficiency(ServerPlayer player, SkillData.WeaponType category) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        return proficiencyMap(player).getOrDefault(category, 0);
    }

    public static int getSpentPoints(ServerPlayer player, SkillData.WeaponType category) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        return spentMap(player).getOrDefault(category, 0);
    }

    /** 使えるレベルアップポイント（獲得済み - 使用済み）。 */
    public static int getAvailablePoints(ServerPlayer player, SkillData.WeaponType category) {
        int earned = getProficiency(player, category) / pointsPerLevel();
        return Math.max(0, earned - getSpentPoints(player, category));
    }

    /** 次のレベルアップポイントまでの進捗（0 〜 pointsPerLevel-1）。 */
    public static int getProgress(ServerPlayer player, SkillData.WeaponType category) {
        return getProficiency(player, category) % pointsPerLevel();
    }

    /** 熟練度を加算する。戻り値は加算後の値。 */
    public static int addProficiency(ServerPlayer player, SkillData.WeaponType category, int amount) {
        if (category == null || category == SkillData.WeaponType.NONE || amount == 0) {
            return getProficiency(player, category);
        }
        Map<SkillData.WeaponType, Integer> values = proficiencyMap(player);
        int before = values.getOrDefault(category, 0);
        int updated = Math.max(0, before + amount);
        values.put(category, updated);
        save(player);
        syncToClient(player);
        notifyNewPoints(player, category, before, updated);
        return updated;
    }

    /** 熟練度を設定する。戻り値は設定後の値。 */
    public static int setProficiency(ServerPlayer player, SkillData.WeaponType category, int value) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        int clamped = Math.max(0, value);
        Map<SkillData.WeaponType, Integer> values = proficiencyMap(player);
        values.put(category, clamped);
        save(player);
        syncToClient(player);
        return clamped;
    }

    private static void notifyNewPoints(ServerPlayer player, SkillData.WeaponType category, int before, int after) {
        int perLevel = pointsPerLevel();
        if (after / perLevel <= before / perLevel) {
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message." + SwordSkill.MOD_ID + ".proficiency_point",
                Component.translatable(SwordSkill.MOD_ID + ".weapon." + category.name().toLowerCase()),
                getAvailablePoints(player, category)), true);
    }

    // --- レベルアップ ---------------------------------------------------

    public enum LevelUpResult {
        SUCCESS,
        NO_SKILL,
        NOT_ASSIGNABLE,
        NOT_UNLOCKED,
        NO_POINT,
        MAX_LEVEL
    }

    /**
     * 熟練度ポイントを1つ消費して、そのスキルを1レベル上げる。
     * ポイントはスキルの熟練度カテゴリから消費する。
     */
    public static LevelUpResult levelUpSkill(ServerPlayer player, SkillData skill) {
        if (skill == null) {
            return LevelUpResult.NO_SKILL;
        }
        if (!skill.isAssignable()) {
            return LevelUpResult.NOT_ASSIGNABLE;
        }
        if (ServerConfig.UnlockedSkill.get() && !SkillDataFetcher.isSkillUnlocked(player, skill.getId())) {
            return LevelUpResult.NOT_UNLOCKED;
        }
        SkillData.WeaponType category = categoryOf(skill);
        if (category == SkillData.WeaponType.NONE) {
            return LevelUpResult.NO_SKILL;
        }
        if (getAvailablePoints(player, category) <= 0) {
            return LevelUpResult.NO_POINT;
        }
        int current = SkillLevelManager.getLevel(player, skill.getId());
        if (current >= SkillLevelManager.maxLevel()) {
            return LevelUpResult.MAX_LEVEL;
        }
        SkillLevelManager.setLevel(player, skill.getId(), current + 1);
        spendPoint(player, category);
        return LevelUpResult.SUCCESS;
    }

    private static void spendPoint(ServerPlayer player, SkillData.WeaponType category) {
        Map<SkillData.WeaponType, Integer> spent = spentMap(player);
        spent.put(category, spent.getOrDefault(category, 0) + 1);
        save(player);
    }

    // --- 撃破による上昇 -------------------------------------------------

    /** スキルで撃破したときに熟練度を加算する。 */
    public static void awardForKill(ServerPlayer player, LivingEntity killed) {
        int amount = proficiencyPerKill();
        if (amount <= 0 || killed == null || killed == player) {
            return;
        }
        if (ServerConfig.proficiencyFromMonstersOnly.get()
                && killed.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        SkillData.WeaponType category = categoryOf(SkillExecutionManager.getRunningSkillId(player));
        if (category == SkillData.WeaponType.NONE) {
            return;
        }
        addProficiency(player, category, amount);
    }

    // --- クライアント同期 -----------------------------------------------

    public static void syncToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSyncPayload(player));
    }

    /** クライアント表示用の同期パケットを組み立てる。0 のカテゴリは省略する。 */
    public static SyncProficiencyPayload buildSyncPayload(ServerPlayer player) {
        Map<SkillData.WeaponType, Integer> values = proficiencyMap(player);
        Map<SkillData.WeaponType, Integer> spent = spentMap(player);
        int count = 0;
        for (SkillData.WeaponType category : SkillData.WeaponType.values()) {
            if (category == SkillData.WeaponType.NONE) {
                continue;
            }
            if (values.getOrDefault(category, 0) > 0 || spent.getOrDefault(category, 0) > 0) {
                count++;
            }
        }
        String[] categories = new String[count];
        int[] proficiencies = new int[count];
        int[] spentPoints = new int[count];
        int index = 0;
        for (SkillData.WeaponType category : SkillData.WeaponType.values()) {
            if (category == SkillData.WeaponType.NONE) {
                continue;
            }
            int value = values.getOrDefault(category, 0);
            int used = spent.getOrDefault(category, 0);
            if (value <= 0 && used <= 0) {
                continue;
            }
            categories[index] = category.name();
            proficiencies[index] = value;
            spentPoints[index] = used;
            index++;
        }
        return new SyncProficiencyPayload(categories, proficiencies, spentPoints, pointsPerLevel());
    }
}
