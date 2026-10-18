package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SkillUtils {
    /** スキル色が未指定のときに使う既定のトレイル色。 */
    private static final int DEFAULT_SKILL_COLOR = 0x54D8FF;

    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner,
            double damage, double knockback, int duration, String skill_particle, Vec3 Movement) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skill_particle,
                Movement, false, 0);
    }

    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance) {
        spawnFlyingSlash(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                travelDistance, 0);
    }

    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance, double renderPitch) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                null, true, travelDistance, renderPitch);
    }

    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance, Vec3 movement) {
        spawnFlyingSlash(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                travelDistance, movement, 0);
    }

    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance, Vec3 movement, double renderPitch) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                movement, true, travelDistance, renderPitch);
    }

    private static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            Vec3 movement, boolean flyingSlash, double travelDistance) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                movement, flyingSlash, travelDistance, 0);
    }

    private static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            Vec3 movement, boolean flyingSlash, double travelDistance, double renderPitch) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                movement, flyingSlash, travelDistance, renderPitch, DEFAULT_SKILL_COLOR);
    }

    /** トレイル色と同じ色で飛び道具を出す版。色は命中演出に使う。 */
    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance, double renderPitch, int skillColor) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                null, true, travelDistance, renderPitch, skillColor);
    }

    /** トレイル色と同じ色で飛び道具（移動ベクトル指定）を出す版。 */
    public static void spawnFlyingSlash(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            double travelDistance, Vec3 movement, double renderPitch, int skillColor) {
        spawnAttackEffect(level, position, rotation, size, owner, damage, knockback, duration, skillParticle,
                movement, true, travelDistance, renderPitch, skillColor);
    }

    private static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size,
            LivingEntity owner, double damage, double knockback, int duration, String skillParticle,
            Vec3 movement, boolean flyingSlash, double travelDistance, double renderPitch, int skillColor) {
        if (!level.isClientSide) {
            AttackEffectEntity effect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(), level);
            effect.setPos(position.x, position.y, position.z); // 位置を設定
            effect.setRotation((float) rotation.z);
            effect.setRenderPitch((float) renderPitch);
            effect.setFlyingSlash(flyingSlash);
            effect.setXRot((float) (owner.getXRot() + rotation.x));
            effect.setYRot((float) (owner.getYRot() + rotation.y));
            effect.setOwner(owner); // 所有者を設定
            effect.setDamage(damage); // ダメージ設定
            effect.setKnockbackStrength(knockback); // ノックバック設定
            effect.setEffectRadius(size); // 範囲設定
            effect.setSkillParticle(skillParticle);
            effect.setSkillColor(skillColor);
            effect.setDuration(duration);
            if (flyingSlash) {
                if (movement != null) {
                    effect.setMovement(movement);
                } else {
                    effect.setTravelDistance(travelDistance);
                }
            } else {
                effect.setMovement(movement);
            }
            level.addFreshEntity(effect); // エンティティを追加
            SwingArm((ServerPlayer) owner);

            // 爪武器使用時の例外的な3本爪トレイル処理
            if (owner instanceof ServerPlayer player) {
                var weaponTypes = WeaponTypeUtils.getWeaponType(player);
                if (weaponTypes != null && weaponTypes.contains(SkillData.WeaponType.CLAW)) {
                    Vec3 forward = player.getLookAngle().normalize();
                    Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

                    // 右爪トレイル
                    AttackEffectEntity rightEffect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(),
                            level);
                    Vec3 rightPos = position.add(right.scale(0.4));
                    rightEffect.setPos(rightPos.x, rightPos.y, rightPos.z);
                    rightEffect.setRotation((float) (rotation.z + 5));
                    rightEffect.setRenderPitch((float) renderPitch);
                    rightEffect.setFlyingSlash(flyingSlash);
                    rightEffect.setXRot((float) (owner.getXRot() + rotation.x + 5));
                    rightEffect.setYRot((float) (owner.getYRot() + rotation.y + 5));
                    rightEffect.setOwner(owner);
                    rightEffect.setDamage(damage * 0.8);
                    rightEffect.setKnockbackStrength(knockback);
                    rightEffect.setEffectRadius(size);
                    rightEffect.setSkillParticle(skillParticle);
                    rightEffect.setSkillColor(skillColor);
                    rightEffect.setDuration(duration);
                    if (flyingSlash) {
                        if (movement != null)
                            rightEffect.setMovement(movement);
                        else
                            rightEffect.setTravelDistance(travelDistance);
                    } else
                        rightEffect.setMovement(movement);
                    level.addFreshEntity(rightEffect);

                    // 左爪トレイル
                    AttackEffectEntity leftEffect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(),
                            level);
                    Vec3 leftPos = position.add(right.scale(-0.4));
                    leftEffect.setPos(leftPos.x, leftPos.y, leftPos.z);
                    leftEffect.setRotation((float) (rotation.z - 5));
                    leftEffect.setRenderPitch((float) renderPitch);
                    leftEffect.setFlyingSlash(flyingSlash);
                    leftEffect.setXRot((float) (owner.getXRot() + rotation.x - 5));
                    leftEffect.setYRot((float) (owner.getYRot() + rotation.y - 5));
                    leftEffect.setOwner(owner);
                    leftEffect.setDamage(damage * 0.8);
                    leftEffect.setKnockbackStrength(knockback);
                    leftEffect.setEffectRadius(size);
                    leftEffect.setSkillParticle(skillParticle);
                    leftEffect.setSkillColor(skillColor);
                    leftEffect.setDuration(duration);
                    if (flyingSlash) {
                        if (movement != null)
                            leftEffect.setMovement(movement);
                        else
                            leftEffect.setTravelDistance(travelDistance);
                    } else
                        leftEffect.setMovement(movement);
                    level.addFreshEntity(leftEffect);
                }
            }
        }
    }

    public static double BaseDamage(ServerPlayer player) {
        double levelMultiplier = SkillLevelManager.damageMultiplier(player);
        // STR を独自式に組み込む（バニラの攻撃力属性には触れない）
        double statMultiplier = com.zoma1101.swordskill.playerlevel.CombatStats.strAttackMultiplier(player);
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD)) {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            // アイテムから攻撃力を取得
            double mainHandDamage = getDamageFromStack(mainHandItem);
            double offHandDamage = getDamageFromStack(offHandItem);

            // 両手の平均攻撃力を計算
            double totalDamageHalf = (mainHandDamage + offHandDamage) / 2;

            // プレイヤーの現在の攻撃力からメインハンド武器分を引く（素手攻撃力 + バフ分などを抽出）
            double currentAttackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double attackDamageMinusMainHand = currentAttackDamage - mainHandDamage;

            // (武器平均 + 基礎・バフ分) * 倍率 * スキルレベル倍率 * STR
            return (totalDamageHalf + attackDamageMinusMainHand) * ServerConfig.damageMultiplier.get()
                    * levelMultiplier * statMultiplier;
        } else {
            return player.getAttributeValue(Attributes.ATTACK_DAMAGE) * ServerConfig.damageMultiplier.get()
                    * levelMultiplier * statMultiplier;
        }
    }

    private static double getDamageFromStack(ItemStack stack) {
        final double[] damage = { 0.0 };
        // forEachModifier はアイテムのデフォルト属性とカスタム属性の両方を含んで反復します
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                damage[0] += modifier.amount();
            }
        });
        return damage[0];
    }

    public static float BaseKnowBack(ServerPlayer player) {
        return (float) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_KNOCKBACK)).getValue() + 1;
    }

    public static double RushDamage(ServerPlayer player) {
        double MoveX_ABS = Math.abs(player.getDeltaMovement().x);
        double MoveY_ABS = Math.abs(player.getDeltaMovement().y);
        double MoveZ_ABS = Math.abs(player.getDeltaMovement().z);
        float Motion = (float) (MoveX_ABS + MoveY_ABS + MoveZ_ABS) * 3f;
        float Motion_BaseDamage = (float) (MoveX_ABS + MoveY_ABS + MoveZ_ABS) * 0.5f + 1;
        return BaseDamage(player) * Motion_BaseDamage + Motion;
    }

    public static void swingArm(ServerPlayer player, HumanoidArm arm) {
        InteractionHand hand = arm == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.swing(hand);
        // クライアント側へアニメーション再生の指示を送信
        player.connection.send(new ClientboundAnimatePacket(player, arm == HumanoidArm.LEFT ? 3 : 0));
    }

    private static final Map<UUID, Boolean> swingRight = new HashMap<>(); // プレイヤーごとの振り向きを記憶するマップ

    public static void SwingArm(ServerPlayer player) {
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD)
                || Objects.requireNonNull(WeaponTypeUtils.getWeaponName(player)).contains("dual_claw")) {
            UUID playerId = player.getUUID();
            boolean isSwingRight = swingRight.getOrDefault(playerId, true); // 初期値は右手振り

            if (isSwingRight) {
                player.connection.send(new ClientboundAnimatePacket(player, 0)); // 右手振り
            } else {
                player.connection.send(new ClientboundAnimatePacket(player, 3)); // 左手振り
            }

            swingRight.put(playerId, !isSwingRight); // 振り向きを反転
        } else {
            swingArm(player, player.getMainArm());
        }
    }

    public static void cleanup(UUID playerId) {
        swingRight.remove(playerId);
    }

    public static Boolean SkillTargetEntity(LivingEntity entity, LivingEntity owner) {
        if (entity == owner) { // entity が owner 自身の場合
            return false; // PvP ゲームルールに関係なく false を返す
        } else if (!ServerConfig.PvP.get() && entity instanceof Player) {
            return false;
        } else if (entity.getType().getCategory() == MobCategory.CREATURE
                || entity.getType().getCategory() == MobCategory.AMBIENT) {
            return ServerConfig.attackNeutralMobs.get();
        } else {
            return true;
        }
    }

    public static Vec3 rotateLookVec(Entity entity, double pitch, double yaw) {
        float RotX = (float) (entity.getXRot() + pitch);
        float RotY = (float) (entity.getYRot() + yaw);
        float f = RotX * ((float) Math.PI / 180F);
        float f1 = -RotY * ((float) Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }
}
