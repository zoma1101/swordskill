package com.zoma1101.swordskill.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ★メモリキャッシュ: プレイヤーごとのデータを保持
    private static final Map<UUID, JsonObject> PLAYER_DATA_CACHE = new ConcurrentHashMap<>();

    // データファイルの保存先ディレクトリを取得
    private static File getDataFile(ServerPlayer player) {
        String uuid = player.getStringUUID();
        File folder = FMLPaths.GAMEDIR.get().resolve("swordskill_data").toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, uuid + ".json");
    }

    // データを読み込む（キャッシュ優先）
    public static JsonObject loadPlayerData(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // 1. キャッシュにあればそれを返す (高速)
        if (PLAYER_DATA_CACHE.containsKey(playerId)) {
            return PLAYER_DATA_CACHE.get(playerId);
        }

        // 2. キャッシュになければディスクから読み込む
        File file = getDataFile(player);
        JsonObject data;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                data = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) {
                e.printStackTrace();
                data = new JsonObject();
            }
        } else {
            data = new JsonObject();
        }

        // 3. 読み込んだデータをキャッシュに保存
        PLAYER_DATA_CACHE.put(playerId, data);
        return data;
    }

    // データを保存する（キャッシュ更新 + ディスク書き込み）
    public static void savePlayerData(ServerPlayer player, JsonObject data) {
        // キャッシュを更新
        PLAYER_DATA_CACHE.put(player.getUUID(), data);

        // ディスクに書き込み
        File file = getDataFile(player);
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ★ログアウト時にキャッシュを削除するメソッド
    public static void clearCache(ServerPlayer player) {
        PLAYER_DATA_CACHE.remove(player.getUUID());
    }
}