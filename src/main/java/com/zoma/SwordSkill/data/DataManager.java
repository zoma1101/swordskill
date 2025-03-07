package com.zoma.SwordSkill.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_DIR = new File("sword_skill_data");

    public static JsonObject loadPlayerData(ServerPlayer player) {
        File playerFile = getPlayerFile(player);
        if (playerFile.exists()) {
            try (FileReader reader = new FileReader(playerFile)) {
                return GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new JsonObject();
    }

    public static void savePlayerData(ServerPlayer player, JsonObject data) {
        File playerFile = getPlayerFile(player);
        if (!DATA_DIR.exists()) {
            DATA_DIR.mkdirs();
        }
        try (FileWriter writer = new FileWriter(playerFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getPlayerFile(ServerPlayer player) {
        Level level = player.level(); // プレイヤーがいるワールドを取得
        String worldUUID = level.dimension().location().toString(); // ワールドの UUID を取得
        return new File(DATA_DIR, worldUUID + "_" + player.getStringUUID() + ".json"); // ワールドの UUID をファイル名に含める
    }
}