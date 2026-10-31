package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * カウンタースキルのチャージ状態をサーバー側で管理する。
 *
 * <p>クライアントはチャージ開始/終了時に {@code CounterChargePayload} を送る。
 * サーバーはチャージ中の被ダメージを軽減しつつ「実際に受けたダメージ」を蓄積し、
 * 解放時に蓄積量から威力倍率を算出してスキルへ渡す。</p>
 */
public final class CounterManager {
    private CounterManager() {
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private static final class State {
        private final int skillId;
        private boolean charging = true;
        private double damageTaken = 0.0D;

        private State(int skillId) {
            this.skillId = skillId;
        }
    }

    /** チャージ開始。既存の状態は破棄して新しく開始する。 */
    public static void begin(UUID playerId, int skillId) {
        STATES.put(playerId, new State(skillId));
    }

    /**
     * チャージ終了。蓄積したダメージは解放時の計算まで保持する。
     * 画面を開くなど発動せずに終わった場合も、次回の {@link #begin} で上書きされる。
     */
    public static void end(UUID playerId, int skillId) {
        State state = STATES.get(playerId);
        if (state != null && state.skillId == skillId) {
            state.charging = false;
        }
    }

    public static boolean isCharging(UUID playerId) {
        State state = STATES.get(playerId);
        return state != null && state.charging;
    }

    /** チャージ中に軽減するダメージ割合 (0.0〜1.0)。 */
    public static double damageCutOf(UUID playerId) {
        State state = STATES.get(playerId);
        if (state == null || !state.charging) {
            return 0.0D;
        }
        SkillData skill = SwordSkillRegistry.SKILLS.get(state.skillId);
        return skill == null ? 0.0D : skill.getCounterDamageCut();
    }

    /** 実際に受けたダメージを蓄積する。 */
    public static void accumulate(UUID playerId, double amount) {
        State state = STATES.get(playerId);
        if (state != null && state.charging && amount > 0.0D) {
            state.damageTaken += amount;
        }
    }

    /**
     * 蓄積した被ダメージから威力ボーナス倍率を求めて状態を破棄する。
     *
     * @return {@code 1.0 + min(damageTaken * scale, maxBonus)}（maxBonus が 0 なら上限なし）
     */
    public static float consumeMultiplier(UUID playerId) {
        State state = STATES.remove(playerId);
        if (state == null) {
            return 1.0F;
        }
        SkillData skill = SwordSkillRegistry.SKILLS.get(state.skillId);
        if (skill == null || !skill.isCounter()) {
            return 1.0F;
        }
        double bonus = Math.max(0.0D, state.damageTaken * skill.getCounterDamageScale());
        double maxBonus = skill.getCounterMaxBonus();
        if (maxBonus > 0.0D) {
            bonus = Math.min(bonus, maxBonus);
        }
        return (float) (1.0D + bonus);
    }

    public static void forget(UUID playerId) {
        STATES.remove(playerId);
    }
}
