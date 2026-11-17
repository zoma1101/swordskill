package com.zoma1101.swordskill.proficiency;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.payload.SyncProficiencyPayload;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.ProficiencyRules;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
 * カテゴリのスキルを当てたモンスターを撃破したときに上昇する（料理や鍛冶など、
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
    /** レベルアップで消費した熟練度の実数値（1.5 倍の端数を保持するため）。 */
    public static final String SPENT_PROF_KEY = "proficiency_spent_raw";

    /** ソードスキルを当てた敵に残す「撃破時に熟練度を入れる」マーカー。 */
    private static final String PENDING_KILL_PLAYER_KEY = "swordskill_pending_kill_player";
    private static final String PENDING_KILL_CATEGORY_KEY = "swordskill_pending_kill_category";

    private static final Map<UUID, Map<SkillData.WeaponType, Integer>> PROFICIENCY = new HashMap<>();
    private static final Map<UUID, Map<SkillData.WeaponType, Integer>> SPENT = new HashMap<>();
    private static final Map<UUID, Map<SkillData.WeaponType, Integer>> SPENT_PROF = new HashMap<>();

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

    /** 装備武器を考慮した実効カテゴリ。 */
    public static SkillData.WeaponType categoryOf(ServerPlayer player, SkillData skill) {
        return ProficiencyRules.effectiveCategory(skill, WeaponTypeUtils.getWeaponType(player));
    }

    /** 別カテゴリのスキルを装備武器で使うときは 1.5 倍の熟練度が必要。 */
    public static boolean isCrossCategory(ServerPlayer player, SkillData skill) {
        return ProficiencyRules.isCrossCategory(skill, WeaponTypeUtils.getWeaponType(player));
    }

    /** プレイヤーと装備武器を考慮した解放必要熟練度。 */
    public static int unlockRequirementOf(ServerPlayer player, SkillData skill) {
        int base = unlockRequirementOf(skill);
        return base > 0 && isCrossCategory(player, skill) ? ProficiencyRules.scale(base) : base;
    }

    /** レベルアップ1回に必要な熟練度。別カテゴリなら 1.5 倍（既定 100 → 150）。 */
    public static int levelUpCost(ServerPlayer player, SkillData skill) {
        int base = pointsPerLevel();
        return isCrossCategory(player, skill) ? ProficiencyRules.scale(base) : base;
    }

    // --- キャッシュ -----------------------------------------------------

    private static Map<SkillData.WeaponType, Integer> mapOf(UUID playerId,
            Map<UUID, Map<SkillData.WeaponType, Integer>> store) {
        return store.computeIfAbsent(playerId, ignored -> new EnumMap<>(SkillData.WeaponType.class));
    }

    private static Map<SkillData.WeaponType, Integer> proficiencyMap(ServerPlayer player) {
        return mapOf(player.getUUID(), PROFICIENCY);
    }

    private static Map<SkillData.WeaponType, Integer> spentProfMap(ServerPlayer player) {
        return mapOf(player.getUUID(), SPENT_PROF);
    }

    /** ディスクから読み込む。 */
    public static void loadPlayer(ServerPlayer player) {
        Map<SkillData.WeaponType, Integer> proficiency = new EnumMap<>(SkillData.WeaponType.class);
        Map<SkillData.WeaponType, Integer> spent = new EnumMap<>(SkillData.WeaponType.class);
        Map<SkillData.WeaponType, Integer> spentProf = new EnumMap<>(SkillData.WeaponType.class);
        JsonObject playerData = DataManager.loadPlayerData(player);
        readInto(playerData, PROFICIENCY_KEY, proficiency);
        readInto(playerData, SPENT_KEY, spent);
        readInto(playerData, SPENT_PROF_KEY, spentProf);
        if (!playerData.has(SPENT_PROF_KEY)) {
            // 旧セーブは消費ポイント数だけが残っているので熟練度に換算する
            for (Map.Entry<SkillData.WeaponType, Integer> entry : spent.entrySet()) {
                spentProf.put(entry.getKey(), entry.getValue() * pointsPerLevel());
            }
        }
        PROFICIENCY.put(player.getUUID(), proficiency);
        SPENT.put(player.getUUID(), spent);
        SPENT_PROF.put(player.getUUID(), spentProf);
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
        SPENT_PROF.remove(playerId);
    }

    private static void save(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        playerData.add(PROFICIENCY_KEY, toJson(proficiencyMap(player)));
        Map<SkillData.WeaponType, Integer> spentPoints = new EnumMap<>(SkillData.WeaponType.class);
        for (Map.Entry<SkillData.WeaponType, Integer> entry : spentProfMap(player).entrySet()) {
            spentPoints.put(entry.getKey(), entry.getValue() / Math.max(1, pointsPerLevel()));
        }
        SPENT.put(player.getUUID(), spentPoints);
        playerData.add(SPENT_KEY, toJson(spentPoints));
        playerData.add(SPENT_PROF_KEY, toJson(spentProfMap(player)));
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
        return spentProfMap(player).getOrDefault(category, 0) / Math.max(1, pointsPerLevel());
    }

    /** レベルアップに使える熟練度（消費済みを差し引いた実数値）。 */
    public static int getAvailableProficiency(ServerPlayer player, SkillData.WeaponType category) {
        if (category == null || category == SkillData.WeaponType.NONE) {
            return 0;
        }
        int earned = getProficiency(player, category);
        int used = spentProfMap(player).getOrDefault(category, 0);
        return Math.max(0, earned - used);
    }

    /** 使えるレベルアップポイント（表示用）。 */
    public static int getAvailablePoints(ServerPlayer player, SkillData.WeaponType category) {
        return getAvailableProficiency(player, category) / Math.max(1, pointsPerLevel());
    }

    /** 次のレベルアップまでの進捗（0 〜 pointsPerLevel-1）。 */
    public static int getProgress(ServerPlayer player, SkillData.WeaponType category) {
        return getAvailableProficiency(player, category) % Math.max(1, pointsPerLevel());
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
        SkillData.WeaponType category = categoryOf(player, skill);
        if (category == SkillData.WeaponType.NONE) {
            return LevelUpResult.NO_SKILL;
        }
        int cost = levelUpCost(player, skill);
        if (getAvailableProficiency(player, category) < cost) {
            return LevelUpResult.NO_POINT;
        }
        int current = SkillLevelManager.getLevel(player, skill.getId());
        if (current >= SkillLevelManager.maxLevel()) {
            return LevelUpResult.MAX_LEVEL;
        }
        SkillLevelManager.setLevel(player, skill.getId(), current + 1);
        spendProficiency(player, category, cost);
        return LevelUpResult.SUCCESS;
    }

    private static void spendProficiency(ServerPlayer player, SkillData.WeaponType category, int amount) {
        Map<SkillData.WeaponType, Integer> spent = spentProfMap(player);
        spent.put(category, spent.getOrDefault(category, 0) + amount);
        save(player);
    }

    // --- 撃破による上昇 -------------------------------------------------

    /**
     * ソードスキルが命中した敵に「撃破時に熟練度を入れる」マーカーを付ける。
     * この敵を後から通常攻撃などで撃破しても熟練度が入る。
     */
    public static void markSwordSkillHit(ServerPlayer player, LivingEntity target, int skillId) {
        if (proficiencyPerKill() <= 0) {
            return;
        }
        if (player == null || target == null || target == player || !target.isAlive()) {
            return;
        }
        SkillData usedSkill = skillId >= 0 ? SwordSkillRegistry.SKILLS.get(skillId) : null;
        SkillData.WeaponType category = categoryOf(player, usedSkill);
        if (category == SkillData.WeaponType.NONE) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        data.putString(PENDING_KILL_PLAYER_KEY, player.getUUID().toString());
        data.putString(PENDING_KILL_CATEGORY_KEY, category.name());
    }

    /**
     * スキルで撃破したとき、またはソードスキルを当てた敵を撃破したときに熟練度を加算する。
     */
    public static void awardForKill(ServerPlayer player, LivingEntity killed) {
        int amount = proficiencyPerKill();
        if (amount <= 0 || killed == null || killed == player) {
            return;
        }
        if (ServerConfig.proficiencyFromMonstersOnly.get()
                && killed.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        SkillData usedSkill = SwordSkillRegistry.SKILLS.get(SkillExecutionManager.getRunningSkillId(player));
        // 別カテゴリのスキルを装備武器で使っている場合は、その武器側のカテゴリに熟練度が入る
        SkillData.WeaponType category = categoryOf(player, usedSkill);
        if (category == SkillData.WeaponType.NONE) {
            category = pendingCategory(player, killed);
        }
        if (category == SkillData.WeaponType.NONE) {
            return;
        }
        addProficiency(player, category, amount);
        clearPendingKill(killed);
    }

    /**
     * プレイヤー以外（環境・別モブなど）が撃破した場合でも、
     * ソードスキルを当てたプレイヤーに熟練度を加算する。
     */
    public static void awardForMarkedKill(LivingEntity killed) {
        int amount = proficiencyPerKill();
        if (amount <= 0 || killed == null) {
            return;
        }
        if (ServerConfig.proficiencyFromMonstersOnly.get()
                && killed.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        CompoundTag data = killed.getPersistentData();
        if (!data.contains(PENDING_KILL_PLAYER_KEY, Tag.TAG_STRING)) {
            return;
        }
        UUID playerId = parseUuid(data.getString(PENDING_KILL_PLAYER_KEY));
        SkillData.WeaponType category = parseCategory(data.getString(PENDING_KILL_CATEGORY_KEY));
        if (playerId == null || category == null || killed.level().getServer() == null) {
            return;
        }
        ServerPlayer player = killed.level().getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        addProficiency(player, category, amount);
        clearPendingKill(killed);
    }

    /** 撃破マーカーがそのプレイヤーのものなら、保存されたカテゴリを返す。 */
    private static SkillData.WeaponType pendingCategory(ServerPlayer player, LivingEntity killed) {
        CompoundTag data = killed.getPersistentData();
        if (!data.contains(PENDING_KILL_PLAYER_KEY, Tag.TAG_STRING)) {
            return SkillData.WeaponType.NONE;
        }
        if (!player.getUUID().toString().equals(data.getString(PENDING_KILL_PLAYER_KEY))) {
            return SkillData.WeaponType.NONE;
        }
        SkillData.WeaponType category = parseCategory(data.getString(PENDING_KILL_CATEGORY_KEY));
        return category == null ? SkillData.WeaponType.NONE : category;
    }

    private static void clearPendingKill(LivingEntity killed) {
        CompoundTag data = killed.getPersistentData();
        data.remove(PENDING_KILL_PLAYER_KEY);
        data.remove(PENDING_KILL_CATEGORY_KEY);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    // --- クライアント同期 -----------------------------------------------

    public static void syncToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSyncPayload(player));
    }

    /** クライアント表示用の同期パケットを組み立てる。0 のカテゴリは省略する。 */
    public static SyncProficiencyPayload buildSyncPayload(ServerPlayer player) {
        Map<SkillData.WeaponType, Integer> values = proficiencyMap(player);
        Map<SkillData.WeaponType, Integer> spent = spentProfMap(player);
        int perLevel = Math.max(1, pointsPerLevel());
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
            spentPoints[index] = used / perLevel;
            index++;
        }
        return new SyncProficiencyPayload(categories, proficiencies, spentPoints, pointsPerLevel());
    }
}
