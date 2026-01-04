package com.zoma1101.swordskill.payload;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// アイテム消費とスキル解放を一括でリクエストするパケット
public record RequestUnlockSkillPayload(int skillId) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<RequestUnlockSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "request_unlock_skill"));

    public static final StreamCodec<FriendlyByteBuf, RequestUnlockSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RequestUnlockSkillPayload::skillId,
            RequestUnlockSkillPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // サーバーサイドハンドラー
    // サーバーサイドハンドラー
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }

            boolean canUnlock = false;

            if (player.isCreative()) {
                canUnlock = true;
            } else {
                // 1. インベントリからアイテムを探して消費する
                for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    ItemStack itemStack = player.getInventory().getItem(i);

                    if (itemStack.is(SampleItemRegistry.UNLOCKITEM.get())) {
                        itemStack.shrink(1);
                        if (itemStack.isEmpty()) {
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                        canUnlock = true;
                        player.getInventory().setChanged(); // インベントリの変更を通知
                        LOGGER.debug("Consumed unlock item for player {}", player.getName().getString());
                        break;
                    }
                }
            }

            // 2. 消費に成功した場合、またはクリエイティブの場合のみ、スキルを解放する
            if (canUnlock) {
                LOGGER.info("Unlocking skill ID {} for player {}", skillId, player.getName().getString());

                // --- ここから SkillUnlockPayload のロジックを移植 ---
                try {
                    JsonObject playerData = DataManager.loadPlayerData(player);
                    JsonArray unlockedSkillsArray;

                    // 既存データの読み込み
                    if (playerData.has("unlockedskill")) {
                        JsonElement unlockedSkillsElement = playerData.get("unlockedskill");
                        if (unlockedSkillsElement.isJsonArray()) {
                            unlockedSkillsArray = unlockedSkillsElement.getAsJsonArray();
                        } else if (unlockedSkillsElement.isJsonPrimitive() && unlockedSkillsElement.getAsJsonPrimitive().isNumber()) {
                            unlockedSkillsArray = new JsonArray();
                            unlockedSkillsArray.add(unlockedSkillsElement.getAsInt());
                        } else {
                            unlockedSkillsArray = new JsonArray();
                        }
                    } else {
                        unlockedSkillsArray = new JsonArray();
                    }

                    // 重複チェック
                    boolean alreadyUnlocked = false;
                    for (JsonElement element : unlockedSkillsArray) {
                        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && element.getAsInt() == skillId) {
                            alreadyUnlocked = true;
                            break;
                        }
                    }

                    // 保存処理
                    if (!alreadyUnlocked) {
                        unlockedSkillsArray.add(skillId);
                        playerData.add("unlockedskill", unlockedSkillsArray);
                        DataManager.savePlayerData(player, playerData);
                        LOGGER.debug("Unlocked skill {} for player {}", skillId, player.getName().getString());

                        int[] unlockedSkills = SkillDataFetcher.getUnlockedSkills(player);
                        List<Integer> skillList = Arrays.stream(unlockedSkills).boxed().collect(Collectors.toList());
                        PacketDistributor.sendToPlayer(player, new SyncUnlockedSkillsPayload(skillList));
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to process unlock skill data for player {}", player.getName().getString(), e);
                }


            } else {
                LOGGER.warn("Player {} tried to unlock skill {} but unlock item was not found.", player.getName().getString(), skillId);
            }
        });
    }
}