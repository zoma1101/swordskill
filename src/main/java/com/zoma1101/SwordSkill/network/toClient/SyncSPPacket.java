package com.zoma1101.swordskill.network.toClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncSPPacket {
    private final double currentSP;

    public SyncSPPacket(double currentSP) {
        this.currentSP = currentSP;
    }

    public SyncSPPacket(FriendlyByteBuf buf) {
        this.currentSP = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.currentSP);
    }

    public static void handle(SyncSPPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSPData.set(msg.currentSP);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
