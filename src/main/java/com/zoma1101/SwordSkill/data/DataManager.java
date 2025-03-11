package com.zoma1101.SwordSkill.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        playerFile.getParentFile().mkdirs(); // 親ディレクトリが存在しない場合は作成
        try (FileWriter writer = new FileWriter(playerFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getPlayerFile(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();
        Path worldFolderPath = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
        File dataDir = worldFolderPath.resolve("swordskill_data").toFile(); // ワールドフォルダ内にデータフォルダを作成
        String playerUUID = player.getUUID().toString();
        return new File(dataDir, playerUUID + ".json");
    }
}