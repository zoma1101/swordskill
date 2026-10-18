package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.playerlevel.PlayerLevelManager;
import com.zoma1101.swordskill.playerlevel.StatType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * ステータスポイントを STR / AGI に割り振るリクエスト。
 * 実際に消費できるかはサーバー側で検証する。
 */
public record AllocateStatPayload(String statName, int amount) implements CustomPacketPayload {

    public static final Type<AllocateStatPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "allocate_stat"));

    public static final StreamCodec<FriendlyByteBuf, AllocateStatPayload> STREAM_CODEC = StreamCodec.of(
            AllocateStatPayload::encode, AllocateStatPayload::decode);

    private static void encode(FriendlyByteBuf buffer, AllocateStatPayload payload) {
        buffer.writeUtf(payload.statName() == null ? "" : payload.statName(), 16);
        buffer.writeVarInt(payload.amount());
    }

    private static AllocateStatPayload decode(FriendlyByteBuf buffer) {
        return new AllocateStatPayload(buffer.readUtf(16), buffer.readVarInt());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            StatType type = StatType.byName(statName);
            if (type == null) {
                return;
            }
            PlayerLevelManager.allocate(player, type, amount);
            // 成功・失敗どちらでも最新状態を送り直して UI を揃える
            PlayerLevelManager.syncToClient(player);
        });
    }
}
