package com.zoma1101.swordskill.payload;

import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

// データを持たないパケットなので空の record で定義
public record SkillRequestPayload() implements CustomPacketPayload {


    // 1. ペイロードIDを定義
    public static final Type<SkillRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_request"));
    public static final StreamCodec<FriendlyByteBuf, SkillRequestPayload> STREAM_CODEC = StreamCodec.unit(new SkillRequestPayload());
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    private static final Logger LOGGER = LogManager.getLogger();
    // 4. サーバーサイドハンドラーメソッド
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // ServerPlayer かどうかを確認
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received SkillRequestPacket from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            try {
                JsonObject playerData = DataManager.loadPlayerData(player);
                int selectedSkillIndex = playerData.has("selectedSkillIndex") ? playerData.get("selectedSkillIndex").getAsInt() : 0;
                SyncSkillIndexPayload syncPacket = new SyncSkillIndexPayload(selectedSkillIndex);
                PacketDistributor.sendToPlayer(player, syncPacket); // プレイヤー本人に送り返す

            } catch (Exception e) {
                LOGGER.error("Failed to process SkillRequestPacket for player {}", player.getName().getString(), e);
            }
        });
    }
}