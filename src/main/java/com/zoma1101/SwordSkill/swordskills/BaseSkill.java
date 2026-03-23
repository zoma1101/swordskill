package com.zoma1101.swordskill.swordskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * すべてのソードスキルの基底クラスです。
 * スキル実装で頻繁に使用される共通メソッド（ダメージ計算、エフェクト生成、トレイル制御など）を提供します。
 */
public abstract class BaseSkill implements ISkill {

    /**
     * スキルのメイン処理を実行します。
     * 各スキルはこのメソッドをオーバーライドして具体的な動作を記述します。
     */
    @Override
    public abstract void execute(Level level, ServerPlayer player, int tickCount, int SkillID);

    /**
     * 指定されたタグを使用して攻撃エフェクトを生成します。
     */
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags,
            Vec3 movement) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, "",
                tags, movement, false);
    }

    /**
     * 指定されたタグと追従設定を使用して攻撃エフェクトを生成します。
     */
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, java.util.List<SkillTag> tags,
            Vec3 movement, boolean followOwner) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                tags, movement, followOwner);
    }

    /**
     * 攻撃エフェクトを生成します（追従なし）。
     */
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, Vec3 movement) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                movement);
    }

    /**
     * 攻撃エフェクトを生成します（追従設定あり）。
     */
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, Vec3 movement, boolean followOwner) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                movement, followOwner);
    }

    /**
     * プレイヤーの現在の攻撃力に基づいた基本ダメージを取得します。
     */
    protected static double getBaseDamage(ServerPlayer player) {
        return SkillUtils.BaseDamage(player);
    }

    /**
     * プレイヤーの現在のノックバック強度に基づいた基本ノックバック値を取得します。
     */
    protected static float getBaseKnockback(ServerPlayer player) {
        return SkillUtils.BaseKnowBack(player);
    }

    /**
     * ラッシュ系スキルのダメージ計算（移動速度ボーナス等を含む）を行います。
     */
    protected double getRushDamage(ServerPlayer player) {
        return SkillUtils.RushDamage(player);
    }

    /**
     * 腕振りのアニメーションを実行します（二刀流などの判定を含む）。
     */
    protected void swingArm(ServerPlayer player) {
        SkillUtils.SwingArm(player);
    }

    /**
     * 剣の軌跡（トレイル）の発生を能動的に切り替えます。
     *
     * @param active trueで発生、falseで停止
     */
    protected void setTrailActive(ServerPlayer player, boolean active) {
        SkillUtils.setTrailActive(player, active);
    }

    /**
     * 指定された実体がスキルのターゲットとして有効かどうかを判定します。
     */
    protected boolean isValidSkillTarget(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.entity.LivingEntity owner) {
        return SkillUtils.SkillTargetEntity(entity, owner);
    }

    /**
     * エンティティの視線方向を基準に回転させたベクトルを取得します。
     */
    protected Vec3 rotateLookVec(net.minecraft.world.entity.Entity entity, double pitch, double yaw) {
        return SkillUtils.rotateLookVec(entity, pitch, yaw);
    }

    /**
     * 標準的なスキル発動音を再生します。
     */
    protected void playSimpleSkillSound(Level level, Vec3 pos) {
        SkillSound.SimpleSkillSound(level, pos);
    }

    /**
     * トレイル付きの飛翔斬撃を生成します。（スキルIDから自動的に色を取得します）
     */
    protected void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, int SkillID, Vec3 movement) {
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor()
                : com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer.DEFAULT_COLOR;
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration,
                java.util.List.of(SkillTag.TRAIL), trailColor, movement);
    }

    /**
     * タグとスキルIDを指定して飛翔斬撃を生成します。（色を自動取得します）
     */
    protected void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags, int SkillID,
            Vec3 movement) {
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor()
                : com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer.DEFAULT_COLOR;
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration, tags, trailColor,
                movement);
    }

    /**
     * トレイル付きの飛翔斬撃を生成します。（色を直接指定します）
     */
    protected void spawnFlyingSlashColor(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, int trailColor, Vec3 movement) {
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration,
                java.util.List.of(SkillTag.TRAIL,SkillTag.SHAPE_ARC), trailColor, movement);
    }

    /**
     * タグとトレイル色を指定して飛翔斬撃を生成します。（色を直接指定します）
     */
    protected void spawnFlyingSlashColor(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags, int trailColor,
            Vec3 movement) {
        java.util.List<SkillTag> finalTags = new java.util.ArrayList<>(tags);
        if (!finalTags.contains(SkillTag.TRAIL))
            finalTags.add(SkillTag.TRAIL);
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration,
                getNormalSkillTexture(), finalTags, trailColor, movement, false);
    }

    /**
     * 標準的なスキルパーティクルのテクスチャパスを取得します。
     */
    protected String getNormalSkillTexture() {
        return SkillTexture.NomalSkillTexture();
    }

    /**
     * プレイヤーの視線を基準に相対的な位置・回転で斬撃エフェクトを生成します。
     * 連撃スキルでの利用を想定し、ダメージ・ノックバック・位置計算の定型文を減らします。
     *
     * @param forwardOffset 前方へのオフセット距離（ブロック数）
     * @param rightOffset   右方向へのオフセット距離
     * @param upOffset      上方向へのオフセット距離（視点の高さ基準）
     * @param rotation      X/Y/Z の回転角度（度）
     * @param size          エフェクトのサイズ (x, y, z)
     * @param damageMult    基本ダメージに対する倍率
     * @param knockbackMult 基本ノックバックに対する倍率
     * @param duration      持続時間（tick）
     * @param tags          付与するタグのリスト（nullでタグなし）
     * @param trailColor    トレイルのカラー（SkillIDを渡して自動取得したい場合は別途呼び出してください。今回は0を指定で通常）
     */
    protected void spawnRelativeSlash(Level level, ServerPlayer player,
            double forwardOffset, double rightOffset, double upOffset,
            Vec3 rotation, Vector3f size,
            double damageMult, double knockbackMult,
            int duration, java.util.List<SkillTag> tags) {

        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        Vec3 spawnPos = player.position()
                .add(0, player.getEyeHeight() * 0.75, 0)
                .add(lookVec.scale(forwardOffset))
                .add(rightVec.scale(rightOffset))
                .add(upVec.scale(upOffset));

        double damage = getBaseDamage(player) * damageMult;
        double knockback = getBaseKnockback(player) * knockbackMult;

        java.util.List<SkillTag> finalTags = tags != null ? tags : java.util.Collections.emptyList();

        spawnAttackEffect(level, spawnPos, rotation, size, player, damage, knockback, duration, getNormalSkillTexture(),
                finalTags, Vec3.ZERO, false);
        playSimpleSkillSound(level, spawnPos);
    }

    protected void spawnRelativeSlash(Level level, ServerPlayer player,
            double forwardOffset, double rightOffset, double upOffset,
            Vec3 rotation, Vector3f size,
            double damageMult, double knockbackMult,
            int duration, java.util.List<SkillTag> tags, int SkillID) {

        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        Vec3 spawnPos = player.position()
                .add(0, player.getEyeHeight() * 0.75, 0)
                .add(lookVec.scale(forwardOffset))
                .add(rightVec.scale(rightOffset))
                .add(upVec.scale(upOffset));

        double damage = getBaseDamage(player) * damageMult;
        double knockback = getBaseKnockback(player) * knockbackMult;

        java.util.List<SkillTag> finalTags = tags != null ? new java.util.ArrayList<>(tags) : new java.util.ArrayList<>();
        if (!finalTags.contains(SkillTag.TRAIL)) {
            finalTags.add(SkillTag.TRAIL);
        }

        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;

        SkillUtils.spawnAttackEffect(level, spawnPos, rotation, size, player, damage, knockback, duration, getNormalSkillTexture(),
                finalTags, trailColor, Vec3.ZERO, false);
        playSimpleSkillSound(level, spawnPos);
    }

    /**
     * シンプルな相対斬撃生成ヘルパー（タグなし・持続12tick）
     */
    protected void spawnRelativeSlash(Level level, ServerPlayer player,
            double forwardOffset, double rightOffset, double upOffset,
            Vec3 rotation, Vector3f size,
            double damageMult, double knockbackMult) {
        spawnRelativeSlash(level, player, forwardOffset, rightOffset, upOffset, rotation, size, damageMult,
                knockbackMult, 12, null);
    }

    protected void spawnMovingRelativeSlash(Level level, ServerPlayer player,
            double forwardOffset, double rightOffset, double upOffset,
            Vec3 rotation, Vector3f size,
            double damageMult, double knockbackMult, int SkillID, Vec3 move) {

        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        Vec3 spawnPos = player.position()
                .add(0, player.getEyeHeight() * 0.75, 0)
                .add(lookVec.scale(forwardOffset))
                .add(rightVec.scale(rightOffset))
                .add(upVec.scale(upOffset));

        double damage = getBaseDamage(player) * damageMult;
        double knockback = getBaseKnockback(player) * knockbackMult;

        spawnFlyingSlashColor(level, spawnPos, rotation, size, player, damage, knockback, 12,
                SkillID, move);
        playSimpleSkillSound(level, spawnPos);
    }

}
