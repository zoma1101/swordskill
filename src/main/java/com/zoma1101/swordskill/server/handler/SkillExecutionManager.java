package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.payload.PlayAnimationPayload;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
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

    /**
     * スキル実行インスタンスのキャッシュ。
     *
     * <p>{@link ISkill#execute} は状態を引数で受け取る設計で、実装（{@link DataDrivenSkill}）は
     * フィールドを持たない。毎 tick リフレクションで生成し直す必要はないのでクラスごとに使い回す。</p>
     */
    private static final Map<Class<? extends ISkill>, ISkill> SKILL_INSTANCES = new HashMap<>();

    /** スキルクラスのインスタンスを取得する（初回のみリフレクションで生成）。 */
    public static ISkill skillInstance(SkillData skill) throws ReflectiveOperationException {
        Class<? extends ISkill> skillClass = skill.getSkillClass();
        ISkill cached = SKILL_INSTANCES.get(skillClass);
        if (cached != null) {
            return cached;
        }
        ISkill created = skillClass.getDeclaredConstructor().newInstance();
        SKILL_INSTANCES.put(skillClass, created);
        return created;
    }

    public static void startSkillExecution(ServerPlayer player, int skillId, int finalTick, float chargeMultiplier) {
        DataDrivenSkill.clearPlayerState(player.getUUID());
        DataDrivenSkill.setChargeMultiplier(player.getUUID(), chargeMultiplier);
        // 現在のホットバースロットを保存してインスタンス生成
        SkillExecutionData execution = new SkillExecutionData(
                skillId, (int) player.level().getGameTime(), finalTick, player.getInventory().selected);
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill != null) {
            if (skill.getType() == SkillData.SkillType.RUSH && skill.getDataDefinition() != null) {
                execution.rushPhase = "start";
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new PlayAnimationPayload(player.getId(), skillId,
                                animationForPhase(skill, execution.rushPhase)));
            } else {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                        new PlayAnimationPayload(player.getId(), skillId, ""));
            }
        }
        skillExecutions.put(player.getUUID(), execution);
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
            // 重量ペナルティ: STR 不足時はソードスキルの進行が遅くなる
            double skillSpeed = com.zoma1101.swordskill.smithing.SmithingEquipmentHandler
                    .skillSpeedMultiplier(player);
            int effectiveFinalTick = skillSpeed < 1.0D
                    ? (int) Math.ceil(skillExecution.finalTick / skillSpeed)
                    : skillExecution.finalTick;
            if (currentTick - skillExecution.startTick <= effectiveFinalTick) {
                int elapsedTicks = currentTick - skillExecution.startTick;
                if (skillSpeed < 1.0D) {
                    elapsedTicks = (int) Math.floor(elapsedTicks * skillSpeed);
                }
                SkillData skill = SwordSkillRegistry.SKILLS.get(skillExecution.skillId);
                if (skill != null && skill.getDataDefinition() != null
                        && skill.getType() == SkillData.SkillType.RUSH
                        && elapsedTicks >= skill.getDataDefinition().rushStartTicks()
                        && "start".equals(skillExecution.rushPhase)) {
                    setRushPhase(player, skillExecution, "move");
                }
                executeSkill(level, player, skillExecution.skillId, elapsedTicks);
            } else {
                skillExecutions.remove(playerId);
                DataDrivenSkill.clearPlayerState(playerId);
                // データパック再読み込みでスキルが消えている可能性があるので null を許容する
                SkillData finishedSkill = SwordSkillRegistry.SKILLS.get(skillExecution.skillId);
                if (finishedSkill != null && finishedSkill.getType() == SkillData.SkillType.RUSH) {
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                            new PlayAnimationPayload(player.getId(), 0, ""));
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

    public static void finishRush(ServerPlayer player) {
        SkillExecutionData execution = skillExecutions.get(player.getUUID());
        if (execution == null) {
            return;
        }
        SkillData skill = SwordSkillRegistry.SKILLS.get(execution.skillId);
        if (skill == null || skill.getDataDefinition() == null
                || skill.getType() != SkillData.SkillType.RUSH) {
            return;
        }
        if (!"finish".equals(execution.rushPhase)) {
            setRushPhase(player, execution, "finish");
        }
        skillExecutions.remove(player.getUUID());
        DataDrivenSkill.clearPlayerState(player.getUUID());
    }

    public static void changeRushPhase(ServerPlayer player, String phase) {
        SkillExecutionData execution = skillExecutions.get(player.getUUID());
        SkillData skill = execution == null ? null : SwordSkillRegistry.SKILLS.get(execution.skillId);
        if (execution == null || skill == null || skill.getType() != SkillData.SkillType.RUSH
                || phase.equals(execution.rushPhase)) {
            return;
        }
        setRushPhase(player, execution, phase);
    }

    public static void cancelRush(ServerPlayer player) {
        SkillExecutionData execution = skillExecutions.remove(player.getUUID());
        if (execution == null)
            return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new PlayAnimationPayload(player.getId(), 0, ""));
        DataDrivenSkill.clearPlayerState(player.getUUID());
    }

    private static void setRushPhase(ServerPlayer player, SkillExecutionData execution, String phase) {
        execution.rushPhase = phase;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new PlayAnimationPayload(player.getId(), execution.skillId,
                        animationForPhase(SwordSkillRegistry.SKILLS.get(execution.skillId), phase)));
    }

    private static String animationForPhase(SkillData skill, String phase) {
        var definition = skill.getDataDefinition();
        return switch (phase) {
            case "start" -> definition.rushStartAnimation();
            case "move" -> definition.rushMoveAnimation();
            case "finish" -> definition.rushFinishAnimation();
            default -> phase;
        };
    }

    private static class SkillExecutionData {
        int skillId;
        int startTick;
        int finalTick;
        int initialSlot;
        String rushPhase;

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
                skillInstance(skill).execute(level, player, tickCount, skillId);
            } catch (Exception e) {
                LOGGER.error("スキル実行中にエラーが発生しました。", e);
            }
        }
    }
}
