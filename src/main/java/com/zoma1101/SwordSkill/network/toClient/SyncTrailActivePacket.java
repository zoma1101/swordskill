package com.zoma1101.swordskill.network.toClient;

import com.zoma1101.swordskill.client.renderer.layer.SwordTrailManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncTrailActivePacket {
    private final int entityId;
    private final boolean active;

    public SyncTrailActivePacket(int entityId, boolean active) {
        this.entityId = entityId;
        this.active = active;
    }

    public SyncTrailActivePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(active);
    }

    public static void handle(SyncTrailActivePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
                if (level != null) {
                    net.minecraft.world.entity.Entity entity = level.getEntity(msg.entityId);
                    if (entity != null) {
                        SwordTrailManager.getSession(entity.getUUID()).active = msg.active;
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
