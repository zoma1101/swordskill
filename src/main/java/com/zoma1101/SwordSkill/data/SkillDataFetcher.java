package com.zoma1101.swordskill.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

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
                    } else {
                        LOGGER.warn("予期しない unlockedskill のデータ形式 (数値ではありません): " + unlockedSkillsArray.get(i));
                        unlockedSkills[i] = -1; // エラー値として -1 を設定 (必要に応じて変更)
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

    public static void printUnlockedSkills(ServerPlayer player) {
        int[] unlockedSkills = getUnlockedSkills(player);
        if (unlockedSkills.length > 0) {
            LOGGER.info(player.getName().getString() + " の解放済みスキル: " + java.util.Arrays.toString(unlockedSkills));
        } else {
            LOGGER.info(player.getName().getString() + " はまだスキルを解放していません。");
        }
    }

    // 他の場所で getUnlockedSkills メソッドを呼び出して使用できます
    public static void someOtherMethod(ServerPlayer player) {
        int[] unlocked = getUnlockedSkills(player);
        // 取り出した配列を使った処理
        for (int skillId : unlocked) {
            // スキル ID を使った処理
            LOGGER.info("解放済みスキル ID: " + skillId);
        }
    }

    public static boolean isSkillUnlocked(ServerPlayer player, int targetSkillId) {
        int[] unlockedSkills = getUnlockedSkills(player);
        return Arrays.stream(unlockedSkills).anyMatch(skillId -> skillId == targetSkillId);
    }

}