package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.payload.PlayAnimationPayload;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillExecutionManager {
    public static final Map<UUID, SkillExecutionData> skillExecutions = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    public static void startSkillExecution(ServerPlayer player, int skillId, int finalTick) {
        // 現在のホットバースロットを保存してインスタンス生成
        skillExecutions.put(player.getUUID(), new SkillExecutionData(skillId, (int) player.level().getGameTime(), finalTick, player.getInventory().selected));
        LOGGER.info("スキル実行開始: プレイヤー={}, スキルID={}, 終了Tick={}", player.getName().getString(), skillId, finalTick);
    }

    public static void handleSkillExecution(Level level, ServerPlayer player) {
        UUID playerId = player.getUUID();
        SkillExecutionData skillExecution = skillExecutions.get(playerId);

        if (skillExecution != null) {
            // --- アイテム切り替え防止処理 ---
            if (player.getInventory().selected != skillExecution.initialSlot) {
                player.getInventory().selected = skillExecution.initialSlot;
                player.connection.send(new ClientboundSetCarriedItemPacket(skillExecution.initialSlot));
            }
            // -----------------------------

            int currentTick = (int) level.getGameTime();
            if (currentTick - skillExecution.startTick <= skillExecution.finalTick) {
                executeSkill(level, player, skillExecution.skillId, currentTick - skillExecution.startTick);
            } else {
                skillExecutions.remove(playerId);
                if (SwordSkillRegistry.SKILLS.get(skillExecution.skillId).getType() == SkillData.SkillType.RUSH){
                    PacketDistributor.sendToPlayer(player, new PlayAnimationPayload(0,""));
                }
                LOGGER.info("スキル実行終了: プレイヤー={}, スキルID={}", player.getName().getString(), skillExecution.skillId);
            }
        }
    }

    // 外部からスキル実行中か確認するためのメソッド
    public static boolean isSkillActive(ServerPlayer player) {
        return skillExecutions.containsKey(player.getUUID());
    }

    // ★追加: 現在実行中のスキルIDを取得するメソッド
    public static int getRunningSkillId(ServerPlayer player) {
        SkillExecutionData data = skillExecutions.get(player.getUUID());
        return data != null ? data.skillId : -1;
    }

    private static class SkillExecutionData {
        int skillId;
        int startTick;
        int finalTick;
        int initialSlot;

        public SkillExecutionData(int skillId, int startTick, int finalTick, int initialSlot) {
            this.skillId = skillId;
            this.startTick = startTick;
            this.finalTick = finalTick;
            this.initialSlot = initialSlot;
        }
    }

    private static void executeSkill(Level level, ServerPlayer player, int skillId, int tickCount) {
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill != null) {
            try {
                ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
                skillInstance.execute(level, player, tickCount, skillId);
            } catch (Exception e) {
                LOGGER.error("スキル実行中にエラーが発生しました。", e);
            }
        }
    }
}