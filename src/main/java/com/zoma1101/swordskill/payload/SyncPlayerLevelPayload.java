package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientPlayerLevelHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * サーバーからクライアントへプレイヤーレベルとステータスを送る。
 * {@code xpToNext} が 0 のときは最大レベル。
 */
public record SyncPlayerLevelPayload(int level, int maxLevel, long xp, long xpToNext,
        int spBonus, int str, int agility,
        int availablePoints, int totalPoints, int spentPoints) implements CustomPacketPayload {

    public static final Type<SyncPlayerLevelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_player_level"));

    public static final StreamCodec<FriendlyByteBuf, SyncPlayerLevelPayload> STREAM_CODEC = StreamCodec.of(
            SyncPlayerLevelPayload::encode, SyncPlayerLevelPayload::decode);

    private static void encode(FriendlyByteBuf buffer, SyncPlayerLevelPayload payload) {
        buffer.writeVarInt(Math.max(1, payload.level()));
        buffer.writeVarInt(Math.max(1, payload.maxLevel()));
        buffer.writeVarLong(Math.max(0L, payload.xp()));
        buffer.writeVarLong(Math.max(0L, payload.xpToNext()));
        buffer.writeVarInt(payload.spBonus());
        buffer.writeVarInt(payload.str());
        buffer.writeVarInt(payload.agility());
        buffer.writeVarInt(payload.availablePoints());
        buffer.writeVarInt(payload.totalPoints());
        buffer.writeVarInt(payload.spentPoints());
    }

    private static SyncPlayerLevelPayload decode(FriendlyByteBuf buffer) {
        return new SyncPlayerLevelPayload(
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(), buffer.readVarLong(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncPlayerLevelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPlayerLevelHandler.update(payload));
    }
}
