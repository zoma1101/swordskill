package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.GroundImpactHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 着地衝撃（メイス・インパクトなど）の地面揺れと土煙をクライアントで再生するための通知。
 *
 * @param x        衝撃の中心 X
 * @param y        衝撃の中心 Y（プレイヤーの足元）
 * @param z        衝撃の中心 Z
 * @param strength 揺れの強さ（0 で無効）
 * @param color    スキル色（0xRRGGBB）
 */
public record GroundImpactPayload(double x, double y, double z, float strength, int color)
        implements CustomPacketPayload {

    public static final Type<GroundImpactPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "ground_impact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GroundImpactPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, GroundImpactPayload::x,
                    ByteBufCodecs.DOUBLE, GroundImpactPayload::y,
                    ByteBufCodecs.DOUBLE, GroundImpactPayload::z,
                    ByteBufCodecs.FLOAT, GroundImpactPayload::strength,
                    ByteBufCodecs.INT, GroundImpactPayload::color,
                    GroundImpactPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(GroundImpactPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> GroundImpactHandler.onGroundImpact(
                msg.x(), msg.y(), msg.z(), msg.strength(), msg.color()));
    }
}
