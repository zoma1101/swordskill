package com.zoma1101.swordskill.playerlevel;

import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * STR / AGI とレベル差を、バニラの属性とは別の独自式で戦闘計算に組み込む。
 *
 * <ul>
 *   <li>STR … ソードスキルの与ダメージ倍率（{@link #strAttackMultiplier}）。
 *       将来の鍛冶システムで装備重量の緩和も担当する予定。</li>
 *   <li>AGI … 移動速度（{@link #agilityMoveSpeedBonus}）</li>
 * </ul>
 *
 * <p>レベル差の項は {@link EnemyLevel} を持つエンティティにだけ作用する。レベル未設定の
 * エンティティ（他 MOD のモブなど）には一切影響しない。
 *
 * <p>防御（被ダメージ軽減）はステータスではなく防具（バニラの防御力）が担当する。
 */
public final class CombatStats {

    private CombatStats() {
    }

    private static int stat(ServerPlayer player, StatType type) {
        return Math.max(0, PlayerLevelManager.getStat(player, type));
    }

    private static int diffCap() {
        return Math.max(0, ServerConfig.levelDiffCap.get());
    }

    // --- ステータス効果 -------------------------------------------------

    /** STR によるソードスキルのダメージ倍率（1.0 = 補正なし）。 */
    public static double strAttackMultiplier(ServerPlayer player) {
        double bonus = stat(player, StatType.STR) * ServerConfig.strDamagePerPoint.get();
        return 1.0D + Mth.clamp(bonus, 0.0D, Math.max(0.0D, ServerConfig.strMaxBonus.get()));
    }

    /** AGI による移動速度ボーナス（倍率。0.3 = +30%）。 */
    public static double agilityMoveSpeedBonus(ServerPlayer player) {
        double bonus = stat(player, StatType.AGI) * ServerConfig.agilityMoveSpeedPerPoint.get();
        return Mth.clamp(bonus, 0.0D, Math.max(0.0D, ServerConfig.agilityMaxMoveSpeedBonus.get()));
    }

    // --- レベル差 -------------------------------------------------------

    /** 与ダメージに掛かるレベル差倍率（高レベルほど大きい）。 */
    public static double levelDiffAttackFactor(int playerLevel, int enemyLevel) {
        if (!ServerConfig.levelDiffEnabled.get() || enemyLevel <= 0) {
            return 1.0D;
        }
        int diff = Mth.clamp(playerLevel - enemyLevel, -diffCap(), diffCap());
        return Math.max(0.1D, 1.0D + diff * ServerConfig.levelDiffPerPoint.get());
    }

    // --- 合成 -----------------------------------------------------------

    /** ソードスキル1発の攻撃倍率（STR × レベル差）。 */
    public static double attackMultiplier(ServerPlayer player, LivingEntity target) {
        return strAttackMultiplier(player)
                * levelDiffAttackFactor(PlayerLevelManager.getLevel(player), EnemyLevel.get(target));
    }
}
