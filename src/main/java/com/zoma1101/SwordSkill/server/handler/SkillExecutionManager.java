package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.event.SkillTryUseEvent; // ★追加
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.toClient.PlayAnimationPacket;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillExecutionManager {
    public static final Map<UUID, SkillExecutionData> skillExecutions = new HashMap<>();

    // ★追加: ISkillインスタンスのキャッシュ (リフレクション負荷対策)
    private static final Map<Integer, ISkill> skillInstanceCache = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger();

    public static void startSkillExecution(ServerPlayer player, int skillId, int finalTick) {
        // ★追加: スキル名を取得
        SkillData skillData = SwordSkillRegistry.SKILLS.get(skillId);
        String skillName = (skillData != null) ? skillData.getName() : "";

        // ★追加: スキル発動試行イベントの発火
        SkillTryUseEvent event = new SkillTryUseEvent(player, skillName);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
            // イベントがキャンセルされた場合、処理を中断（リターン）する
            LOGGER.info("スキル実行がキャンセルされました: プレイヤー={}, スキルID={}", player.getName().getString(), skillId);
            return;
        }

        // ★変更: 開始時のスロット番号を保存
        int initialSlot = player.getInventory().selected;
        skillExecutions.put(player.getUUID(), new SkillExecutionData(skillId, (int) player.level().getGameTime(), finalTick, initialSlot));
        LOGGER.info("スキル実行開始: プレイヤー={}, スキルID={}, 終了Tick={}", player.getName().getString(), skillId, finalTick);
    }

    public static void handleSkillExecution(Level level, ServerPlayer player) {
        UUID playerId = player.getUUID();
        SkillExecutionData skillExecution = skillExecutions.get(playerId);

        if (skillExecution != null) {
            // ★追加: アイテム切り替え防止処理
            if (player.getInventory().selected != skillExecution.initialSlot) {
                player.getInventory().selected = skillExecution.initialSlot;
                // クライアントにスロット変更を通知して同期ズレを防ぐ
                player.connection.send(new ClientboundSetCarriedItemPacket(skillExecution.initialSlot));
            }

            int currentTick = (int) level.getGameTime();
            if (currentTick - skillExecution.startTick <= skillExecution.finalTick) {
                // スキル実行
                executeSkill(level, player, skillExecution.skillId, currentTick - skillExecution.startTick);
            } else {
                // スキル実行情報削除
                skillExecutions.remove(playerId);
                if (SwordSkillRegistry.SKILLS.get(skillExecution.skillId).getType() == SkillData.SkillType.RUSH){
                    NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PlayAnimationPacket(player.getId(),0,""));;
                }
                LOGGER.info("スキル実行終了: プレイヤー={}, スキルID={}", player.getName().getString(), skillExecution.skillId);
            }
        }
    }

    // SkillExecutionData クラスの定義
    private static class SkillExecutionData {
        int skillId;
        int startTick;
        int finalTick;
        int initialSlot; // ★追加

        public SkillExecutionData(int skillId, int startTick, int finalTick, int initialSlot) {
            this.skillId = skillId;
            this.startTick = startTick;
            this.finalTick = finalTick;
            this.initialSlot = initialSlot;
        }
    }

    private static void executeSkill(Level level, ServerPlayer player, int skillId, int tickCount) {
        // ★変更: キャッシュからインスタンスを取得 (computeIfAbsentで初回のみ生成)
        ISkill skillInstance = skillInstanceCache.computeIfAbsent(skillId, id -> {
            SkillData skill = SwordSkillRegistry.SKILLS.get(id);
            if (skill != null) {
                try {
                    return skill.getSkillClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    LOGGER.error("スキルインスタンスの生成に失敗しました。ID: " + id, e);
                    return null;
                }
            }
            return null;
        });

        if (skillInstance != null) {
            try {
                skillInstance.execute(level, player, tickCount, skillId);
            } catch (Exception e) {
                LOGGER.error("スキル実行中にエラーが発生しました。ID: " + skillId, e);
            }
        } else {
            // キャッシュ生成に失敗した場合など
            if (tickCount == 0) { // ログスパム防止のため最初だけ警告
                LOGGER.warn("スキルID {} に対応するスキルが見つかりません。", skillId);
            }
        }
    }
}