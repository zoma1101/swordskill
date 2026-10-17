package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.HitstopHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** ソードスキル命中時のヒットストップをクライアントへ通知する。 */
public record HitstopPayload(int attackerId, int targetId, int ticks) implements CustomPacketPayload {
    public static final Type<HitstopPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "hitstop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HitstopPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HitstopPayload::attackerId,
            ByteBufCodecs.VAR_INT, HitstopPayload::targetId,
            ByteBufCodecs.VAR_INT, HitstopPayload::ticks,
            HitstopPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(HitstopPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HitstopHandler.onHitstop(msg.attackerId(), msg.targetId(), msg.ticks()));
    }
}
