package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.SkillClashHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 鍔迫り合い（ブレードロック）と飛翔斬撃の相殺をクライアントへ通知する。
 *
 * <p>{@code phase} は {@link #PHASE_LOCK_START} / {@link #PHASE_LOCK_END} / {@link #PHASE_SPARK}。
 * ロック中はサーバーが自動判定するため、クライアントからの入力 payload は不要。
 * {@code winner} は {@link #PHASE_LOCK_END} のときだけ意味を持ち、-1 は引き分け。</p>
 */
public record SkillClashPayload(int clashId, byte phase, int playerA, int playerB, int winner,
        int lockTicks, int colorA, int colorB, float x, float y, float z) implements CustomPacketPayload {

    /** 鍔迫り合い開始。 */
    public static final byte PHASE_LOCK_START = 0;
    /** 鍔迫り合い終了（勝敗確定）。 */
    public static final byte PHASE_LOCK_END = 1;
    /** 飛翔斬撃同士の相殺。位置だけを使う。 */
    public static final byte PHASE_SPARK = 2;

    public static final Type<SkillClashPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_clash"));

    public static final StreamCodec<FriendlyByteBuf, SkillClashPayload> STREAM_CODEC = StreamCodec.of(
            SkillClashPayload::encode, SkillClashPayload::decode);

    private static void encode(FriendlyByteBuf buffer, SkillClashPayload payload) {
        buffer.writeVarInt(payload.clashId);
        buffer.writeByte(payload.phase);
        buffer.writeVarInt(payload.playerA);
        buffer.writeVarInt(payload.playerB);
        buffer.writeVarInt(payload.winner);
        buffer.writeVarInt(payload.lockTicks);
        buffer.writeVarInt(payload.colorA);
        buffer.writeVarInt(payload.colorB);
        buffer.writeFloat(payload.x);
        buffer.writeFloat(payload.y);
        buffer.writeFloat(payload.z);
    }

    private static SkillClashPayload decode(FriendlyByteBuf buffer) {
        return new SkillClashPayload(buffer.readVarInt(), buffer.readByte(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SkillClashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkillClashHandler.onPayload(payload));
    }
}
