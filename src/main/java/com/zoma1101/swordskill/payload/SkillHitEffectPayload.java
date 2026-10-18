package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.SkillHitEffectHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * スキルが対象に命中した瞬間の演出（火花バースト・光輪・斬痕・フラッシュ）をクライアントで出すための通知。
 * {@code position} は命中面（攻撃者側を向いた面）の座標、{@code direction} は攻撃者から対象への単位ベクトル。
 */
public record SkillHitEffectPayload(int attackerId, int targetId, int color, float power,
        Vec3 position, Vec3 direction) implements CustomPacketPayload {

    public static final Type<SkillHitEffectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_hit_effect"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillHitEffectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SkillHitEffectPayload::attackerId,
                    ByteBufCodecs.VAR_INT, SkillHitEffectPayload::targetId,
                    ByteBufCodecs.INT, SkillHitEffectPayload::color,
                    ByteBufCodecs.FLOAT, SkillHitEffectPayload::power,
                    VEC3_CODEC, SkillHitEffectPayload::position,
                    VEC3_CODEC, SkillHitEffectPayload::direction,
                    SkillHitEffectPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SkillHitEffectPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> SkillHitEffectHandler.onHitEffect(msg));
    }
}
