package com.zoma1101.swordskill.network;

import com.google.gson.JsonObject;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.network.toClient.SkillSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor; // 追加
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class SkillRequestPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    public SkillRequestPacket() {
    }

    public SkillRequestPacket(FriendlyByteBuf buf) {
        // このパケット自体にデータは不要な場合が多い
    }

    public void encode(FriendlyByteBuf buf) {
        // このパケット自体にデータは不要な場合が多い
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキルリクエスト処理を中止します。");
                return;
            }
            LOGGER.debug("プレイヤー {} からスキルリクエストを受信しました。", player.getName().getString()); // デバッグログ追加推奨

            // プレイヤーデータを読み込む
            JsonObject playerData = DataManager.loadPlayerData(player);
            int selectedSkillIndex = playerData.has("selectedSkillIndex") ? playerData.get("selectedSkillIndex").getAsInt() : 0;

            LOGGER.debug("プレイヤー {} の選択スキルインデックス: {}", player.getName().getString(), selectedSkillIndex); // デバッグログ追加推奨

            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SkillSyncPacket(selectedSkillIndex));
            LOGGER.debug("プレイヤー {} に SkillSyncPacket (Index: {}) を送信しました。", player.getName().getString(), selectedSkillIndex); // デバッグログ追加推奨

            // ClientForgeHandler の呼び出しは削除する
            // ClientForgeHandler.setSelectedSkillIndex(selectedSkillIndex); // ← 削除！
        });
        ctx.get().setPacketHandled(true);
    }
}