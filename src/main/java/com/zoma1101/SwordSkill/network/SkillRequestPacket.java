package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider; // 追加
import com.zoma1101.swordskill.network.toClient.SkillSyncPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
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

    // ★修正: staticメソッドに変更し、Capabilityを使用
    public static void handle(SkillRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // ★修正: DataManagerを使わずCapabilityから読み込み
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                int selectedSkillIndex = skills.getSelectedSlot();

                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new SkillSyncPacket(selectedSkillIndex));
                LOGGER.debug("Sent SkillSyncPacket to {}: Index {}", player.getName().getString(), selectedSkillIndex);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}