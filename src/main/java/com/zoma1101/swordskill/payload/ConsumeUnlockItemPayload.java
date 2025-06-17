package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;


public record ConsumeUnlockItemPayload() implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<ConsumeUnlockItemPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "consume_unlock_item"));

    public static final StreamCodec<FriendlyByteBuf, ConsumeUnlockItemPayload> STREAM_CODEC = StreamCodec.unit(new ConsumeUnlockItemPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // サーバーサイドハンドラー
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }

            boolean consumed = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                ItemStack itemStack = player.getInventory().getItem(i);

                // UNLOCKITEM かどうかを確認
                if (itemStack.is(SampleItemRegistry.UNLOCKITEM.get())) {
                    itemStack.shrink(1);
                    if (itemStack.isEmpty()) {
                        player.getInventory().setItem(i, ItemStack.EMPTY);
                    }
                    consumed = true;
                    LOGGER.debug("Consumed unlock item for player {}", player.getName().getString());
                    player.getInventory().setChanged();
                    break;
                }
            }

            if (!consumed) {
                LOGGER.warn("Player {} tried to consume unlock item but specific unlock item was not found.", player.getName().getString());
            }
        });
    }
}