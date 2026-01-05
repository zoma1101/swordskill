package com.zoma1101.swordskill.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.network.toClient.UnlockedSkillsResponsePacket; // 追加
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor; // 追加
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList; // 追加
import java.util.List; // 追加
import java.util.function.Supplier;

import static com.zoma1101.swordskill.item.SampleItemRegistry.UNLOCKITEM;

public class SkillUnlockPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int unlockedskill;

    public SkillUnlockPacket(int unlockedskill) {
        this.unlockedskill = unlockedskill;
    }

    public SkillUnlockPacket(FriendlyByteBuf buf) {
        this.unlockedskill = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlockedskill);
    }

    public static void handle(SkillUnlockPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // --- 1. アイテム消費処理 ---
            boolean canUnlock = false;

            if (player.isCreative()) {
                canUnlock = true;
            } else {
                for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
                    if (player.getInventory().getItem(i).is(UNLOCKITEM.get())) {
                        player.getInventory().removeItem(i, 1);
                        canUnlock = true;
                        break;
                    }
                }
            }

            // --- 2. スキル解放処理 ---
            if (canUnlock) {
                JsonObject playerData = DataManager.loadPlayerData(player);
                JsonArray unlockedSkillsArray;

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

                boolean alreadyUnlocked = false;
                for (int i = 0; i < unlockedSkillsArray.size(); i++) {
                    if (unlockedSkillsArray.get(i).getAsInt() == msg.unlockedskill) {
                        alreadyUnlocked = true;
                        break;
                    }
                }

                if (!alreadyUnlocked) {
                    unlockedSkillsArray.add(msg.unlockedskill);
                    playerData.add("unlockedskill", unlockedSkillsArray);
                    DataManager.savePlayerData(player, playerData);
                    LOGGER.info("Player {} unlocked skill ID: {}", player.getName().getString(), msg.unlockedskill);

                    List<Integer> skillList = new ArrayList<>();
                    for (JsonElement el : unlockedSkillsArray) {
                        skillList.add(el.getAsInt());
                    }
                    int[] skillArray = skillList.stream().mapToInt(i -> i).toArray();

                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new UnlockedSkillsResponsePacket(skillArray));
                }
            } else {
                LOGGER.warn("Player {} tried to unlock skill {} without the required item.", player.getName().getString(), msg.unlockedskill);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}