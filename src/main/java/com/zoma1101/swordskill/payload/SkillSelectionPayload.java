package com.zoma1101.swordskill.payload;

import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public record SkillSelectionPayload(int selectedSkillIndex) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();


    public static final Type<SkillSelectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_selection"));
    public static final StreamCodec<FriendlyByteBuf, SkillSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SkillSelectionPayload::selectedSkillIndex,
            SkillSelectionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillSelectionPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received SkillSelectionPayload from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            try {
                // プレイヤーデータをロード
                JsonObject playerData = DataManager.loadPlayerData(player);
                // 新しい選択インデックスを保存
                playerData.addProperty("selectedSkillIndex", msg.selectedSkillIndex());
                // 変更を保存
                DataManager.savePlayerData(player, playerData);
                LOGGER.debug("Saved selected skill index {} for player {}", msg.selectedSkillIndex(), player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to process SkillSelectionPayload for player {}", player.getName().getString(), e);
            }
        });
    }
}