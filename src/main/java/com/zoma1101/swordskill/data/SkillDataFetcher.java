package com.zoma1101.swordskill.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkillDataFetcher {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 解放済みスキル ID のキャッシュ。
     *
     * <p>以前は参照のたびに {@link DataManager#loadPlayerData(ServerPlayer)} でディスクから
     * 読み直していたため、スキル発動のたびに JSON の読み込みとパースが走っていた。
     * ログイン時に読み込み、書き込み時に更新することでホットパスから I/O を追い出す。</p>
     */
    private static final Map<UUID, Set<Integer>> UNLOCKED_CACHE = new HashMap<>();

    /** ログイン時にディスクから読み込んでキャッシュする。 */
    public static void loadPlayer(ServerPlayer player) {
        UNLOCKED_CACHE.put(player.getUUID(), readUnlockedSet(DataManager.loadPlayerData(player)));
    }

    /** ログアウト時にキャッシュを破棄する。 */
    public static void unloadPlayer(UUID playerId) {
        UNLOCKED_CACHE.remove(playerId);
    }

    /**
     * キャッシュを捨てて次回アクセス時に読み直させる。
     * {@code unlockedskill} を {@link DataManager} で直接書き換えたときに呼ぶ。
     */
    public static void invalidate(ServerPlayer player) {
        UNLOCKED_CACHE.remove(player.getUUID());
    }

    private static Set<Integer> unlockedSet(ServerPlayer player) {
        return UNLOCKED_CACHE.computeIfAbsent(player.getUUID(),
                ignored -> readUnlockedSet(DataManager.loadPlayerData(player)));
    }

    private static Set<Integer> readUnlockedSet(JsonObject playerData) {
        Set<Integer> unlocked = new LinkedHashSet<>();
        for (JsonElement element : readUnlockedArray(playerData)) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                unlocked.add(element.getAsInt());
            }
        }
        return unlocked;
    }

    /** 解放済み ID を保存する。 */
    private static void writeUnlocked(ServerPlayer player, Set<Integer> unlocked) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonArray array = new JsonArray();
        for (int skillId : unlocked) {
            array.add(skillId);
        }
        playerData.add("unlockedskill", array);
        DataManager.savePlayerData(player, playerData);
    }

    public static int[] getUnlockedSkills(ServerPlayer player) {
        Set<Integer> unlocked = unlockedSet(player);
        int[] skills = new int[unlocked.size()];
        int index = 0;
        for (int skillId : unlocked) {
            skills[index++] = skillId;
        }
        return skills;
    }

    public static boolean isSkillUnlocked(ServerPlayer player, int targetSkillId) {
        return unlockedSet(player).contains(targetSkillId);
    }

    /**
     * 必要熟練度が0のスキル（および {@code default_unlocked} のスキル）を、未解放なら解放済みにする。
     *
     * <p>熟練度を消費せずに使える入門スキルを、アンロックアイテムなしで最初から使えるようにする。
     * ログインのたびに呼ばれるため、後からデータパックへ追加した初期スキルも次回ログインで付与される。</p>
     *
     * @return 新しく解放したスキルがあれば {@code true}
     */
    public static boolean grantDefaultUnlockedSkills(ServerPlayer player) {
        Set<Integer> unlocked = unlockedSet(player);

        boolean changed = false;
        for (SkillData skill : SwordSkillRegistry.SKILLS.values()) {
            if (skill == null || !skill.isAssignable()) {
                continue;
            }
            // 必要熟練度0 = 熟練度なしで修得できるスキル。データパックで0にしたものもここで拾う。
            if (!skill.isDefaultUnlocked() && skill.getUnlockProficiency() > 0) {
                continue;
            }
            if (unlocked.add(skill.getId())) {
                changed = true;
                LOGGER.debug("Granted default-unlocked skill {} to {}",
                        skill.getName(), player.getName().getString());
            }
        }

        if (changed) {
            writeUnlocked(player, unlocked);
        }
        return changed;
    }

    /**
     * スキルを解放済みにする。
     *
     * @return 新しく解放した場合 {@code true}、既に解放済みなら {@code false}
     */
    public static boolean unlockSkill(ServerPlayer player, int skillId) {
        Set<Integer> unlocked = unlockedSet(player);
        if (!unlocked.add(skillId)) {
            return false;
        }
        writeUnlocked(player, unlocked);
        return true;
    }

    /** {@code unlockedskill} を配列として取り出す（旧形式の単一数値も配列へ変換する）。 */
    private static JsonArray readUnlockedArray(JsonObject playerData) {
        if (playerData == null) {
            return new JsonArray();
        }
        JsonElement element = playerData.get("unlockedskill");
        if (element == null || element.isJsonNull()) {
            return new JsonArray();
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        JsonArray array = new JsonArray();
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            array.add(element.getAsInt());
        }
        return array;
    }
}
