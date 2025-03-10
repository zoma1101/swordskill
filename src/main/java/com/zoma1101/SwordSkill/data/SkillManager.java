package com.zoma1101.SwordSkill.data;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

public class SkillManager {

    public static void updateSelectedSkillIndex(ServerPlayer player, int selectedSkillIndex) {
        JsonObject playerData = DataManager.loadPlayerData(player); // ワールド情報を引数から削除
        playerData.addProperty("selectedSkillIndex", selectedSkillIndex);
        DataManager.savePlayerData(player, playerData); // ワールド情報を引数から削除
    }

    public static int getSelectedSkillIndex(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player); // ワールド情報を引数から削除
        return playerData.has("selectedSkillIndex") ? playerData.get("selectedSkillIndex").getAsInt() : 0;
    }
}