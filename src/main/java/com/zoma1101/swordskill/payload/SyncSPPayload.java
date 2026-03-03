package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncSPPayload(double currentSP, double maxSP) implements CustomPacketPayload {

    public static final Type<SyncSPPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_sp"));

    public static final StreamCodec<FriendlyByteBuf, SyncSPPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            SyncSPPayload::currentSP,
            ByteBufCodecs.DOUBLE,
            SyncSPPayload::maxSP,
            SyncSPPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncSPPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // クライアント側のデータ保持クラスに保存
            com.zoma1101.swordskill.network.toClient.ClientSPData.set(msg.currentSP(), msg.maxSP());

            // 念のためプレイヤーの PersistentData にも保持
            if (ctx.player() != null) {
                ctx.player().getPersistentData().putDouble("SS_CurrentSP", msg.currentSP());
                ctx.player().getPersistentData().putDouble("SS_MaxSP", msg.maxSP());
            }
        });
    }
}
