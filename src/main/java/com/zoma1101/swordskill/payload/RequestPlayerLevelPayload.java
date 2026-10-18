package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.playerlevel.PlayerLevelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** ステータス画面を開いたときに最新のレベル／ステータスを要求する。 */
public record RequestPlayerLevelPayload() implements CustomPacketPayload {

    public static final Type<RequestPlayerLevelPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "request_player_level"));

    public static final StreamCodec<FriendlyByteBuf, RequestPlayerLevelPayload> STREAM_CODEC = StreamCodec.unit(
            new RequestPlayerLevelPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                PlayerLevelManager.syncToClient(player);
            }
        });
    }
}
