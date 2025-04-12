package com.zoma1101.swordskill.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class SkillUnlockPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int unlockedskill;

    public SkillUnlockPacket(int unlockedskill) {
        this.unlockedskill = unlockedskill;
    }

    public SkillUnlockPacket(FriendlyByteBuf buf) {
        this.unlockedskill = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlockedskill);
    }

    public static void handle(SkillUnlockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキル選択を中止します。");
                return;
            }


            JsonObject playerData = DataManager.loadPlayerData(player);
            JsonArray unlockedSkillsArray;

            if (playerData.has("unlockedskill")) {
                JsonElement unlockedSkillsElement = playerData.get("unlockedskill");
                if (unlockedSkillsElement.isJsonArray()) {
                    unlockedSkillsArray = unlockedSkillsElement.getAsJsonArray();
                } else if (unlockedSkillsElement.isJsonPrimitive() && unlockedSkillsElement.getAsJsonPrimitive().isNumber()) {
                    // 古い形式の単一の int を配列に変換
                    unlockedSkillsArray = new JsonArray();
                    unlockedSkillsArray.add(unlockedSkillsElement.getAsInt());
                } else {
                    unlockedSkillsArray = new JsonArray(); // 予期しない形式の場合は新しい配列を作成
                    LOGGER.warn("予期しない unlockedskill のデータ形式です。新しい配列を作成します。");
                }
            } else {
                unlockedSkillsArray = new JsonArray();
            }

            // 新しいスキルIDが既に存在するか確認し、存在しない場合のみ追加
            boolean alreadyUnlocked = false;
            for (int i = 0; i < unlockedSkillsArray.size(); i++) {
                if (unlockedSkillsArray.get(i).getAsInt() == msg.unlockedskill) {
                    alreadyUnlocked = true;
                    break;
                }
            }

            if (!alreadyUnlocked) {
                unlockedSkillsArray.add(msg.unlockedskill);
            }

            // 配列を保存
            playerData.add("unlockedskill", unlockedSkillsArray);
            DataManager.savePlayerData(player, playerData);
        });
        ctx.get().setPacketHandled(true);
    }
}