package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.network.toClient.PlayAnimationPacket;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.server.handler.SPManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;

public class UseSkillPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int skillId;
    private final int finalTick;

    public UseSkillPacket(int skillId, int finalTick) {
        this.skillId = skillId;
        this.finalTick = finalTick;
    }

    public UseSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
        this.finalTick = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(skillId);
        buf.writeInt(finalTick);
    }

    public static void handle(UseSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;

            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId);
            if (skill == null)
                return;

            AtomicBoolean isUnlocked = new AtomicBoolean(false);
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS)
                    .ifPresent(cap -> isUnlocked.set(cap.isSkillUnlocked(msg.skillId)));

            // クリエイティブ、コンフィグ設定、または習得済みなら発動
            if (isUnlocked.get() || player.gameMode.isCreative() || !UnlockedSkill.get()) {
                double spCost = skill.getSpCost();
                if (!player.gameMode.isCreative() && SPManager.getCurrentSP(player) < spCost) {
                    LOGGER.info("SPが不足しています: {} / {}", SPManager.getCurrentSP(player), spCost);
                    return;
                }

                // SP消費
                if (!player.gameMode.isCreative()) {
                    SPManager.consumeSP(player, spCost);
                }

                // ★追加: サーバーから全クライアント（本人含む）へアニメーション再生パケットを送信
                String animationType = "";
                if (skill.getType() == SkillData.SkillType.RUSH) {
                    animationType = "start";
                }
                NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new PlayAnimationPacket(player.getId(), msg.skillId, animationType));

                if (msg.finalTick == 0) {
                    executeSkill(player, skill);
                } else {
                    SkillExecutionManager.startSkillExecution(player, msg.skillId, msg.finalTick);
                }
            } else {
                LOGGER.warn("Skill ID {} not unlocked or found.", msg.skillId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void executeSkill(ServerPlayer player, SkillData skill) {
        try {
            ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
            skillInstance.execute(player.level(), player, 0, skill.getId());
            LOGGER.info("スキル {} (ID: {}) を Tick {} で実行", skill.getName(), skill.getId(), 0);
        } catch (Exception e) {
            LOGGER.error("スキル実行中にエラーが発生しました。", e);
        }
    }
}