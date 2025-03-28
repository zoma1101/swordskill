package com.zoma1101.swordskill.network;

import com.google.gson.JsonObject;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class SkillSelectionPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int selectedSkillIndex;

    public SkillSelectionPacket(int selectedSkillIndex) {
        this.selectedSkillIndex = selectedSkillIndex;
    }

    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.selectedSkillIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(selectedSkillIndex);
    }

    public static void handle(SkillSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキル選択を中止します。");
                return;
            }
            JsonObject playerData = DataManager.loadPlayerData(player);
            playerData.addProperty("selectedSkillIndex", msg.selectedSkillIndex);
            DataManager.savePlayerData(player, playerData);
        });
        ctx.get().setPacketHandled(true);
    }
}