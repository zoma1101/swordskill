package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.RegistryFriendlyByteBuf; // ByteBuf から変更
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;
// import org.jetbrains.annotations.NotNull; // id() メソッドには通常不要

public record PlayAnimationPayload(int skillId, String animationType) implements CustomPacketPayload {
    // CustomPacketPayload.Type の代わりに ResourceLocation ID を使用
    public static final Type<PlayAnimationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "play_animation"));

    // StreamCodec の型と参照を PlayAnimationPayload に合わせる
    // ByteBuf を RegistryFriendlyByteBuf に変更
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,          // skillId のコーデック
            PlayAnimationPayload::skillId, // skillId のゲッター
            ByteBufCodecs.STRING_UTF8,  // animationType のコーデック
            PlayAnimationPayload::animationType, // animationType のゲッター
            PlayAnimationPayload::new      // コンストラクタ参照
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(PlayAnimationPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> PlayerAnimation(msg.skillId, msg.animationType));
    }

}