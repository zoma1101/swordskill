package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.network.toClient.UnlockedSkillsResponsePacket; // 追加
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor; // 追加
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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
            if (player == null) return;

            // --- 1. アイテム消費処理 (変更なし) ---
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

            // --- 2. スキル解放処理 (Capabilityへ変更) ---
            if (canUnlock) {
                // ★修正: DataManagerを使わずCapabilityを使用
                player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                    if (!skills.isSkillUnlocked(msg.unlockedskill)) {
                        skills.unlockSkill(msg.unlockedskill);
                        LOGGER.info("Player {} unlocked skill ID: {}", player.getName().getString(), msg.unlockedskill);

                        // クライアントへ同期
                        int[] skillArray = skills.getUnlockedSkills().stream().mapToInt(i -> i).toArray();
                        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new UnlockedSkillsResponsePacket(skillArray, skills.isMartialArtsUnlocked()));
                    }
                });
            } else {
                LOGGER.warn("Player {} tried to unlock skill {} without item.", player.getName().getString(), msg.unlockedskill);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}