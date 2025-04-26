package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.network.toClient.UnlockedSkillsResponsePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class CheckSkillUnlockedPacket {

    private static final Logger LOGGER = LogManager.getLogger();

    public CheckSkillUnlockedPacket() {}
    public CheckSkillUnlockedPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public static void handle(CheckSkillUnlockedPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                int[] unlockedSkills = SkillDataFetcher.getUnlockedSkills(player);
                UnlockedSkillsResponsePacket responsePacket = new UnlockedSkillsResponsePacket(unlockedSkills);
                NetworkHandler.INSTANCE.sendTo(responsePacket, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
                LOGGER.debug("Sent UnlockedSkillsResponsePacket to client {}", player.getName().getString()); // デバッグログ
            } else {
                LOGGER.warn("Sender player was null when handling CheckSkillUnlockedPacket!");
            }
        });
        // パケットが処理されたことを示す
        ctx.get().setPacketHandled(true);
    }
}