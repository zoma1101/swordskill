package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider; // 追加
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
                return;
            }

            // ★修正: DataManagerを使わずCapabilityに保存
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                skills.setSelectedSlot(msg.selectedSkillIndex);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}