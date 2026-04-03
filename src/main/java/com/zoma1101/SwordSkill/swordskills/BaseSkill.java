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
 
    @Override
    public abstract void execute(Level level, ServerPlayer player, int tickCount, int SkillID);
 
    /**
     * 指定されたタグとスキルIDを使用して攻撃エフェクトを生成します。
     * スキルの設定色を自動的に取得して適用します。
     */
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, java.util.List<SkillTag> tags,
            int SkillID, Vec3 movement, boolean followOwner) {
        spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle, tags,
                SkillID, movement, followOwner, true);
    }
 
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, java.util.List<SkillTag> tags,
            int SkillID, Vec3 movement, boolean followOwner, boolean isRelativeRotation) {
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                tags, trailColor, movement, followOwner, isRelativeRotation);
    }
 
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags,
            Vec3 movement) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, "",
                tags, movement, false);
    }
 
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, java.util.List<SkillTag> tags,
            Vec3 movement, boolean followOwner) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                tags, movement, followOwner);
    }
 
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, Vec3 movement) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                movement);
    }
 
    protected void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, String particle, Vec3 movement, boolean followOwner) {
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration, particle,
                movement, followOwner);
    }
 
    protected static double getBaseDamage(ServerPlayer player) {
        return SkillUtils.BaseDamage(player);
    }
 
    protected static float getBaseKnockback(ServerPlayer player) {
        return SkillUtils.BaseKnowBack(player);
    }
 
    protected double getRushDamage(ServerPlayer player) {
        return SkillUtils.RushDamage(player);
    }
 
    protected void swingArm(ServerPlayer player) {
        SkillUtils.SwingArm(player);
    }
 
    protected void setTrailActive(ServerPlayer player, boolean active) {
        SkillUtils.setTrailActive(player, active);
    }
 
    protected boolean isValidSkillTarget(net.minecraft.world.entity.LivingEntity entity,
            net.minecraft.world.entity.LivingEntity owner) {
        return SkillUtils.SkillTargetEntity(entity, owner);
    }
 
    protected Vec3 rotateLookVec(net.minecraft.world.entity.Entity entity, double pitch, double yaw) {
        return SkillUtils.rotateLookVec(entity, pitch, yaw);
    }
 
    protected void playSimpleSkillSound(Level level, Vec3 pos) {
        SkillSound.SimpleSkillSound(level, pos);
    }
 
    /**
     * トレイル付きの飛翔斬撃を生成します。（スキルIDから自動的に色を取得します）
     */
    protected void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, int SkillID, Vec3 movement) {
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration,
                java.util.List.of(SkillTag.TRAIL, SkillTag.SHAPE_ARC), trailColor, movement);
    }
 
    protected void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags, int SkillID,
            Vec3 movement) {
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;
        
        java.util.List<SkillTag> finalTags = new java.util.ArrayList<>(tags);
        if (!finalTags.contains(SkillTag.SHAPE_ARC)) finalTags.add(SkillTag.SHAPE_ARC);
        
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration, finalTags, trailColor,
                movement);
    }
 
    protected void spawnFlyingSlashColor(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, int trailColor, Vec3 movement) {
        spawnFlyingSlashColor(level, position, rotation, size, player, damage, knockback, duration,
                java.util.List.of(SkillTag.TRAIL, SkillTag.SHAPE_ARC), trailColor, movement);
    }
 
    protected void spawnFlyingSlashColor(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer player,
            double damage, double knockback, int duration, java.util.List<SkillTag> tags, int trailColor,
            Vec3 movement) {
        java.util.List<SkillTag> finalTags = new java.util.ArrayList<>(tags);
        if (!finalTags.contains(SkillTag.TRAIL)) finalTags.add(SkillTag.TRAIL);
        if (!finalTags.contains(SkillTag.SHAPE_ARC)) finalTags.add(SkillTag.SHAPE_ARC);
 
        SkillUtils.spawnAttackEffect(level, position, rotation, size, player, damage, knockback, duration,
                getNormalSkillTexture(), finalTags, trailColor, movement, false);
    }
 
    protected String getNormalSkillTexture() {
        return SkillTexture.NomalSkillTexture();
    }
 
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
        // 強制的にタグを挿入するのをやめ、呼び出し側（各スキルクラス）の指定を尊重するように修正
 
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;
 
        SkillUtils.spawnAttackEffect(level, spawnPos, rotation, size, player, damage, knockback, duration, getNormalSkillTexture(),
                finalTags, trailColor, Vec3.ZERO, false);
        playSimpleSkillSound(level, spawnPos);
    }
 
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
 
        // --- 修正箇所: ここで単に SkillID を渡すのではなく、色を取得して渡す ---
        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;
 
        spawnFlyingSlashColor(level, spawnPos, rotation, size, player, damage, knockback, 12,
                trailColor, move);
        playSimpleSkillSound(level, spawnPos);
    }

    /**
     * 固定ダメージと自由な高さ補正を使用して相対的な斬撃を生成します。
     * 足技など、目の高さ以外から発生させたい場合に便利です。
     */
    protected void spawnFixedRelativeSlash(Level level, ServerPlayer player,
            double forwardOffset, double rightOffset, double upOffset,
            double verticalBaseOffsetMult, // 0.0で足元、0.75で通常の目線
            Vec3 rotation, Vector3f size,
            double damage, double knockback,
            int duration, java.util.List<SkillTag> tags, int SkillID) {

        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 upVec = rightVec.cross(lookVec).normalize();

        // 指定された倍率で高さを計算
        Vec3 spawnPos = player.position()
                .add(0, player.getEyeHeight() * verticalBaseOffsetMult, 0)
                .add(lookVec.scale(forwardOffset))
                .add(rightVec.scale(rightOffset))
                .add(upVec.scale(upOffset));

        java.util.List<SkillTag> finalTags = tags != null ? new java.util.ArrayList<>(tags) : new java.util.ArrayList<>();

        SkillData data = SwordSkillRegistry.SKILLS.get(SkillID);
        int trailColor = data != null ? data.getTrailColor() : 0xFF33AAFF;

        SkillUtils.spawnAttackEffect(level, spawnPos, rotation, size, player, damage, knockback, duration, getNormalSkillTexture(),
                finalTags, trailColor, Vec3.ZERO, false);
        playSimpleSkillSound(level, spawnPos);
    }
}
