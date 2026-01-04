package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
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
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, LivingEntity owner, double damage, double knockback, int duration, String skill_particle,Vec3 Movement) {
        if (!level.isClientSide) {
            AttackEffectEntity effect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(), level);
            effect.setPos(position.x, position.y, position.z); // 位置を設定
            effect.setRotation((float) rotation.z);
            effect.setXRot((float) (owner.getXRot() + rotation.x));
            effect.setYRot((float) (owner.getYRot() + rotation.y));
            effect.setOwner(owner); // 所有者を設定
            effect.setDamage(damage); // ダメージ設定
            effect.setKnockbackStrength(knockback); // ノックバック設定
            effect.setEffectRadius(size); // 範囲設定
            effect.setSkillParticle(skill_particle);
            effect.setDuration(duration); // 持続時間設定
            effect.setMovement(Movement);
            level.addFreshEntity(effect); // エンティティを追加
            SwingArm((ServerPlayer) owner);
        }
    }


    public static double BaseDamage(ServerPlayer player){
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD)){
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
                double mainHandDamage =  5; //mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().findFirst().get().getAmount();
                double offHandDamage =  5; //offHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().findFirst().get().getAmount();
                double totalDamageHalf = (mainHandDamage + offHandDamage) / 2;
                double attackDamageMinusMainHand = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue() - mainHandDamage;
                return (totalDamageHalf + attackDamageMinusMainHand) * ServerConfig.damageMultiplier.get();
        } else {
            return Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue() * ServerConfig.damageMultiplier.get();
        }
    }

    public static float BaseKnowBack(ServerPlayer player){
        return (float) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_KNOCKBACK)).getValue()+1;
    }

    public static double RushDamage(ServerPlayer player){
        double MoveX_ABS = Math.abs(player.getDeltaMovement().x);
        double MoveY_ABS = Math.abs(player.getDeltaMovement().y);
        double MoveZ_ABS = Math.abs(player.getDeltaMovement().z);
        float Motion = (float) (MoveX_ABS+MoveY_ABS+MoveZ_ABS)*3f;
        float Motion_BaseDamage = (float) (MoveX_ABS+MoveY_ABS+MoveZ_ABS)*0.5f+1;
        return BaseDamage(player)*Motion_BaseDamage+Motion;
    }

    public static void swingArm(ServerPlayer player, HumanoidArm arm) {
        InteractionHand hand = arm == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        player.swing(hand);
        // クライアント側へアニメーション再生の指示を送信
        player.connection.send(new ClientboundAnimatePacket(player, arm == HumanoidArm.LEFT ? 3 : 0));
    }

    private static final Map<UUID, Boolean> swingRight = new HashMap<>(); // プレイヤーごとの振り向きを記憶するマップ
    public static void SwingArm(ServerPlayer player) {
        if (Objects.requireNonNull(WeaponTypeUtils.getWeaponType(player)).contains(SkillData.WeaponType.DUALSWORD) || Objects.requireNonNull(WeaponTypeUtils.getWeaponName(player)).contains("dual_claw")){
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

    public static Boolean SkillTargetEntity(LivingEntity entity, LivingEntity owner) {
        if (entity == owner) { // entity が owner 自身の場合
            return false; // PvP ゲームルールに関係なく false を返す
        } else if (!ServerConfig.PvP.get() && entity instanceof Player) {
            return false;
        } else if (entity.getType().getCategory() == MobCategory.CREATURE  || entity.getType().getCategory() == MobCategory.AMBIENT){
            return ServerConfig.attackNeutralMobs.get();
        } else {
            return true;
        }
    }

    public static Vec3 rotateLookVec(Entity entity, double pitch , double yaw) {
        float RotX = (float) (entity.getXRot()+ pitch);
        float RotY = (float) (entity.getYRot()+ yaw);
        float f = RotX * ((float)Math.PI / 180F);
        float f1 = -RotY * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }
}