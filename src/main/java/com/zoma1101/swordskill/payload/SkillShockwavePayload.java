package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ShockwaveHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 突きスキルの攻撃時に、ビームを装飾する衝撃波エフェクトをクライアントで表示するための通知。 */
public record SkillShockwavePayload(int entityId, int ticks, String color) implements CustomPacketPayload {
    public static final Type<SkillShockwavePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_shockwave"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillShockwavePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillShockwavePayload::entityId,
                    ByteBufCodecs.VAR_INT, SkillShockwavePayload::ticks,
                    ByteBufCodecs.STRING_UTF8, SkillShockwavePayload::color,
                    SkillShockwavePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SkillShockwavePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ShockwaveHandler.onShockwave(msg.entityId(), msg.ticks(), msg.color()));
    }
}
