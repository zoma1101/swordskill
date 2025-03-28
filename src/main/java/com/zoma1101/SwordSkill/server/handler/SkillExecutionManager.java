package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;

public class SkillExecutionManager {
    public static final Map<UUID, SkillExecutionData> skillExecutions = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    public static void startSkillExecution(ServerPlayer player, int skillId, int finalTick) {
        skillExecutions.put(player.getUUID(), new SkillExecutionData(skillId, (int) player.level().getGameTime(), finalTick));
        LOGGER.info("スキル実行開始: プレイヤー={}, スキルID={}, 終了Tick={}", player.getName().getString(), skillId, finalTick); // ログ出力
    }

    public static void handleSkillExecution(Level level, ServerPlayer player) {
        UUID playerId = player.getUUID();
        SkillExecutionData skillExecution = skillExecutions.get(playerId);

        if (skillExecution != null) {
            int currentTick = (int) level.getGameTime();
            if (currentTick - skillExecution.startTick <= skillExecution.finalTick) {
                // スキル実行
                executeSkill(level, player, skillExecution.skillId, currentTick - skillExecution.startTick);
            } else {
                // スキル実行情報削除
                skillExecutions.remove(playerId);
                if (SwordSkillRegistry.SKILLS.get(skillExecution.skillId).getType() == SkillData.SkillType.RUSH){
                    PlayerAnimation(0,"");
                }
                LOGGER.info("スキル実行終了: プレイヤー={}, スキルID={}", player.getName().getString(), skillExecution.skillId); // ログ出力
            }
        }
    }

    // SkillExecutionData クラスの定義
    private static class SkillExecutionData {
        int skillId;
        int startTick;
        int finalTick;

        public SkillExecutionData(int skillId, int startTick, int finalTick) {
            this.skillId = skillId;
            this.startTick = startTick;
            this.finalTick = finalTick;
        }
    }

    private static void executeSkill(Level level, ServerPlayer player, int skillId, int tickCount) {
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill != null) {
            try {
                ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
                skillInstance.execute(level, player, tickCount,skillId);
                LOGGER.info("スキル {} (ID: {}) を Tick {} で実行", skill.getName(), skillId, tickCount);
            } catch (Exception e) {
                LOGGER.error("スキル実行中にエラーが発生しました。", e);
            }
        } else {
            LOGGER.warn("スキルID {} に対応するスキルが見つかりません。", skillId);
        }
    }
}