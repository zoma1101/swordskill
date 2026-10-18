package com.zoma1101.swordskill.smithing;

/**
 * 武器記憶から導出される隠しパラメータ。
 *
 * <p>プレイヤーには数値をそのまま見せず、傾向に合わせて自然に成長する。</p>
 */
public record HiddenParams(
        double criticalDamageBonus,
        double thrustBonus,
        double sweepBonus,
        double flyingBonus,
        double cooldownReduction) {

    public static final HiddenParams NONE = new HiddenParams(0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    /** 攻撃の傾向に応じた与ダメージ倍率（1.0 = 補正なし）。 */
    public double damageBonus(WeaponMemoryCategory category) {
        return switch (category) {
            case THRUST -> thrustBonus;
            case SWEEP -> sweepBonus;
            case FLYING -> flyingBonus;
            case NORMAL -> 0.0D;
        };
    }
}
