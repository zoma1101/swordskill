package com.zoma1101.swordskill.payload;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public record SkillUnlockPayload(int unlockedskill) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    // 1. ペイロードIDを定義
    public static final Type<SkillUnlockPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_unlock"));

    // 2. StreamCodecを定義 (int 型 1 つなのでシンプル)
    public static final StreamCodec<FriendlyByteBuf, SkillUnlockPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SkillUnlockPayload::unlockedskill,
            SkillUnlockPayload::new
    );

    // 3. CustomPacketPayloadインターフェースのメソッドを実装
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 4. ハンドラーメソッド (IPayloadContextを受け取るように変更)
    public static void handle(SkillUnlockPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received SkillUnlockPacket from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            try {
                JsonObject playerData = DataManager.loadPlayerData(player);
                JsonArray unlockedSkillsArray;

                // 既存の unlockedskill データの読み込みと型チェック
                if (playerData.has("unlockedskill")) {
                    JsonElement unlockedSkillsElement = playerData.get("unlockedskill");
                    if (unlockedSkillsElement.isJsonArray()) {
                        unlockedSkillsArray = unlockedSkillsElement.getAsJsonArray();
                    } else if (unlockedSkillsElement.isJsonPrimitive() && unlockedSkillsElement.getAsJsonPrimitive().isNumber()) {
                        LOGGER.info("Migrating old 'unlockedskill' format for player {}", player.getName().getString());
                        unlockedSkillsArray = new JsonArray();
                        unlockedSkillsArray.add(unlockedSkillsElement.getAsInt());
                    } else {
                        LOGGER.warn("Unexpected data format for 'unlockedskill' for player {}. Creating new array.", player.getName().getString());
                        unlockedSkillsArray = new JsonArray();
                    }
                } else {
                    unlockedSkillsArray = new JsonArray();
                }

                // 新しいスキルIDが既に存在するか確認
                boolean alreadyUnlocked = false;
                for (JsonElement element : unlockedSkillsArray) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && element.getAsInt() == msg.unlockedskill()) {
                        alreadyUnlocked = true;
                        break;
                    }
                }

                // 存在しない場合のみ追加
                if (!alreadyUnlocked) {
                    unlockedSkillsArray.add(msg.unlockedskill());
                    // 更新された配列を保存
                    playerData.add("unlockedskill", unlockedSkillsArray);
                    DataManager.savePlayerData(player, playerData);
                    LOGGER.debug("Unlocked skill {} for player {}", msg.unlockedskill(), player.getName().getString());
                } else {
                    LOGGER.debug("Skill {} is already unlocked for player {}", msg.unlockedskill(), player.getName().getString());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to process SkillUnlockPacket for player {}", player.getName().getString(), e);
            }
        });
    }
}