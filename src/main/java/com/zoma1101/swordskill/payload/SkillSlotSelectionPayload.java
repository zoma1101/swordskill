package com.zoma1101.swordskill.payload;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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

// record を使用
public record SkillSlotSelectionPayload(int skillId, int slotIndex, String weaponName) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    // 1. ペイロードIDを定義
    public static final Type<SkillSlotSelectionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_selection"));

    // 2. StreamCodecを定義 (int, int, String)
    public static final StreamCodec<FriendlyByteBuf, SkillSlotSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, // skillIdのコーデック
            SkillSlotSelectionPayload::skillId, // skillIdゲッター
            ByteBufCodecs.INT, // slotIndexのコーデック
            SkillSlotSelectionPayload::slotIndex, // slotIndexゲッター
            ByteBufCodecs.STRING_UTF8, // weaponNameのコーデック
            SkillSlotSelectionPayload::weaponName, // weaponNameゲッター
            SkillSlotSelectionPayload::new
    );

    // 3. CustomPacketPayloadインターフェースのメソッドを実装
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // 4. サーバーサイドハンドラーメソッド
    public static void handle(SkillSlotSelectionPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // ServerPlayer かどうかを確認
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received SkillSlotSelectionPayload from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            try {
                JsonObject playerData = DataManager.loadPlayerData(player);
                JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");
                // weaponSkills オブジェクトが存在しない場合は新規作成
                if (weaponSkills == null) {
                    weaponSkills = new JsonObject();
                    playerData.add("weaponSkills", weaponSkills);
                }
                // 対象武器のスキル配列を取得、存在しない場合は初期化して作成
                JsonArray skillSlot = weaponSkills.getAsJsonArray(msg.weaponName());
                if (skillSlot == null || !skillSlot.isJsonArray()) { // 配列でない場合も考慮
                    skillSlot = new JsonArray();
                    final int defaultSlotCount = 5;
                    for (int i = 0; i < defaultSlotCount; i++) {
                        skillSlot.add(0); // スロットを0 (空) で初期化
                    }
                    weaponSkills.add(msg.weaponName(), skillSlot);
                    LOGGER.debug("Initialized skill slots for weapon '{}' for player {}", msg.weaponName(), player.getName().getString());
                }

                // スロットインデックスが配列の範囲内か確認
                if (msg.slotIndex() >= 0 && msg.slotIndex() < skillSlot.size()) {
                    skillSlot.set(msg.slotIndex(), new JsonPrimitive(msg.skillId()));
                    LOGGER.debug("Set skill {} to slot {} for weapon '{}' for player {}",
                            msg.skillId(), msg.slotIndex(), msg.weaponName(), player.getName().getString());

                    // プレイヤーデータを保存
                    DataManager.savePlayerData(player, playerData);
                    sendSkillSlotInfo(player);
                } else {
                    LOGGER.warn("Invalid slot index {} for weapon '{}' (size: {}) for player {}",
                            msg.slotIndex(), msg.weaponName(), skillSlot.size(), player.getName().getString());
                }

            } catch (Exception e) {
                LOGGER.error("Failed to process SkillSlotSelectionPayload for player {} (weapon: {}, skill: {}, slot: {})",
                        player.getName().getString(), msg.weaponName(), msg.skillId(), msg.slotIndex(), e);
            }
        });
        // ctx.setPacketHandled(true) は不要
    }
}