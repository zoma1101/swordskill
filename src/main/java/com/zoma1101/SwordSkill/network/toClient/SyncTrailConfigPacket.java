package com.zoma1101.swordskill.network.toClient;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTrailConfigPacket {
    private final boolean enabled;

    public SyncTrailConfigPacket(boolean enabled) {
        this.enabled = enabled;
    }

    public SyncTrailConfigPacket(FriendlyByteBuf buf) {
        this.enabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static void handle(SyncTrailConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                    skills.setTrailEnabled(msg.enabled);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
