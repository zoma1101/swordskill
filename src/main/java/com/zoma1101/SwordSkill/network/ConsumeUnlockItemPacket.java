package com.zoma1101.swordskill.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Supplier;

import static com.zoma1101.swordskill.item.SampleItemRegistry.UNLOCKITEM;

public class ConsumeUnlockItemPacket {
    public ConsumeUnlockItemPacket() {}

    public ConsumeUnlockItemPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ConsumeUnlockItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // サーバー側でアイテムを減少させる処理
                for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    if (player.getInventory().getItem(i).is(UNLOCKITEM.get())) {
                        player.getInventory().removeItem(i, 1);
                        return; // 1個消費したら終了
                    }
                }

            }
            ctx.get().setPacketHandled(true);
        });
    }
}