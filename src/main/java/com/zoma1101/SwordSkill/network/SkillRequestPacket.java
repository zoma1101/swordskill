package com.zoma1101.SwordSkill.network;

import com.zoma1101.SwordSkill.client.handler.ClientForgeHandler;
import com.zoma1101.SwordSkill.data.DataManager;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Supplier;

public class SkillRequestPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    public SkillRequestPacket() {
    }

    public SkillRequestPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void handle(SkillRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキルリクエストを中止します。");
                return;
            }
            JsonObject playerData = DataManager.loadPlayerData(player); // ワールド情報を引数から削除
            int selectedSkillIndex = playerData.has("selectedSkillIndex") ? playerData.get("selectedSkillIndex").getAsInt() : 0;
            ClientForgeHandler.setSelectedSkillIndex(selectedSkillIndex);
        });
        ctx.get().setPacketHandled(true);
    }
}