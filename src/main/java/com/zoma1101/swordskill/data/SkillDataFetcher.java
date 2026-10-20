package com.zoma1101.swordskill.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SkillDataFetcher {
    private static final Logger LOGGER = LogManager.getLogger();

    public static int[] getUnlockedSkills(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        if (playerData.has("unlockedskill")) {
            JsonElement unlockedSkillsElement = playerData.get("unlockedskill");
            if (unlockedSkillsElement.isJsonArray()) {
                JsonArray unlockedSkillsArray = unlockedSkillsElement.getAsJsonArray();
                int[] unlockedSkills = new int[unlockedSkillsArray.size()];
                for (int i = 0; i < unlockedSkillsArray.size(); i++) {
                    if (unlockedSkillsArray.get(i).isJsonPrimitive() && unlockedSkillsArray.get(i).getAsJsonPrimitive().isNumber()) {
                        unlockedSkills[i] = unlockedSkillsArray.get(i).getAsInt();
                    }
                }
                return unlockedSkills;
            } else {
                LOGGER.warn("予期しない unlockedskill のデータ形式 (配列ではありません): " + unlockedSkillsElement);
                return new int[0]; // 配列でない場合は空の配列を返す
            }
        } else {
            // unlockedskill キーが存在しない場合は空の配列を返す
            return new int[0];
        }
    }

    public static boolean isSkillUnlocked(ServerPlayer player, int targetSkillId) {
        int[] unlockedSkills = getUnlockedSkills(player);
        return Arrays.stream(unlockedSkills).anyMatch(skillId -> skillId == targetSkillId);
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
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonArray unlockedSkillsArray = readUnlockedArray(playerData);

        Set<Integer> unlocked = new HashSet<>();
        for (JsonElement element : unlockedSkillsArray) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                unlocked.add(element.getAsInt());
            }
        }

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
                unlockedSkillsArray.add(skill.getId());
                changed = true;
                LOGGER.debug("Granted default-unlocked skill {} to {}",
                        skill.getName(), player.getName().getString());
            }
        }

        if (changed) {
            playerData.add("unlockedskill", unlockedSkillsArray);
            DataManager.savePlayerData(player, playerData);
        }
        return changed;
    }

    /** {@code unlockedskill} を配列として取り出す（旧形式の単一数値も配列へ変換する）。 */
    private static JsonArray readUnlockedArray(JsonObject playerData) {
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