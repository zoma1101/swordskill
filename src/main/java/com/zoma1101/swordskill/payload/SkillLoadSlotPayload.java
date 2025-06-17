package com.zoma1101.swordskill.payload;

import com.google.gson.JsonArray;
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
import org.jetbrains.annotations.NotNull;

import static com.zoma1101.swordskill.server.handler.ServerEventHandler.sendSkillSlotInfo;


public record SkillLoadSlotPayload(String weaponName) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();


    public static final Type<SkillLoadSlotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_load_slot"));
    public static final StreamCodec<FriendlyByteBuf, SkillLoadSlotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SkillLoadSlotPayload::weaponName,
            SkillLoadSlotPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillLoadSlotPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received SkillLoadSlotPayload from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            try {
                if (msg.weaponName() == null || msg.weaponName().isEmpty()) {
                    LOGGER.warn("Received SkillLoadSlotPayload with invalid weaponName from player {}", player.getName().getString());
                    return;
                }

                JsonObject playerData = DataManager.loadPlayerData(player);
                JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");

                if (weaponSkills == null) {
                    weaponSkills = new JsonObject();
                    playerData.add("weaponSkills", weaponSkills);
                }

                JsonArray skillSlot = weaponSkills.getAsJsonArray(msg.weaponName());
                boolean needsSave = false; // データ変更があった場合のみ保存するフラグ
                if (skillSlot == null || !skillSlot.isJsonArray()) {
                    skillSlot = new JsonArray();
                    final int defaultSlotCount = 5;
                    for (int i = 0; i < defaultSlotCount; i++) {
                        skillSlot.add(0);
                    }
                    weaponSkills.add(msg.weaponName(), skillSlot);
                    needsSave = true;
                    LOGGER.debug("Initialized skill slots for weapon '{}' for player {}", msg.weaponName(), player.getName().getString());
                } else {
                    LOGGER.debug("Loaded skill slots for weapon '{}' for player {}", msg.weaponName(), player.getName().getString());
                }

                if (needsSave) {
                    DataManager.savePlayerData(player, playerData);
                }
                sendSkillSlotInfo(player);

            } catch (Exception e) {
                LOGGER.error("Failed to process SkillLoadSlotPayload for player {} (weapon: {})",
                        player.getName().getString(), msg.weaponName(), e);
            }
        });
    }
}