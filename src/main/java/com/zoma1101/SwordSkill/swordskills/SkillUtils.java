package com.zoma1101.SwordSkill.swordskills;

import com.zoma1101.SwordSkill.config.ServerConfig;
import com.zoma1101.SwordSkill.data.WeaponTypeUtils;
import com.zoma1101.SwordSkill.entity.SwordSkill_Entities;
import com.zoma1101.SwordSkill.entity.custom.AttackEffectEntity;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.*;

public class SkillUtils {
    public static void spawnAttackEffect(Level level, Vec3 position, Vec3 rotation, Vector3f size, ServerPlayer owner, double damage, double knockback, int duration, String skill_particle) {
        if (!level.isClientSide) {
            AttackEffectEntity effect = new AttackEffectEntity(SwordSkill_Entities.ATTACK_EFFECT.get(), level);
            effect.setPos(position.x, position.y, position.z); // 位置を設定
            effect.setRotation(new Vec3(owner.getXRot(), -owner.getYRot(), 0).add(rotation));
            effect.setOwner(owner); // 所有者を設定
            effect.setDamage(damage); // ダメージ設定
            effect.setKnockbackStrength(knockback); // ノックバック設定
            effect.setEffectRadius(size); // 範囲設定
            effect.setSkillParticle(skill_particle);
            effect.setDuration(duration); // 持続時間設定
            level.addFreshEntity(effect); // エンティティを追加
            SwingArm(owner);
        }
    }

    public static double BaseDamage(ServerPlayer player){
        if (WeaponTypeUtils.getWeaponType(player) == SkillData.WeaponType.DUALSWORD){
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            double mainHandDamage = mainHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().findFirst().get().getAmount();
            double offHandDamage = offHandItem.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().findFirst().get().getAmount();
            double totalDamageHalf = (mainHandDamage + offHandDamage) / 2;
            double attackDamageMinusMainHand = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue() - mainHandDamage;
            return (totalDamageHalf + attackDamageMinusMainHand)* ServerConfig.damageMultiplier.get() ;

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


    public static Vec3 rotateLookVec(Player player,double Yaw) {
        Vec3 lookVec = player.getLookAngle();

        float angle = (float) Math.toRadians(Yaw);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        Matrix3f rotationMatrix = new Matrix3f(
                cos, 0, sin,
                0, 1, 0,
                -sin, 0, cos
        );

        Vector3f lookVec3f = new Vector3f((float) lookVec.x, (float) lookVec.y, (float) lookVec.z);
        rotationMatrix.transform(lookVec3f); // 修正

        return new Vec3(lookVec3f.x, lookVec3f.y, lookVec3f.z);
    }

    public static void swingArm(ServerPlayer player, HumanoidArm arm) {
        // EquipmentSlot を InteractionHand に変換
        InteractionHand hand = arm == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        // 腕を振るアニメーションを再生
        player.swing(hand);
        // クライアント側へアニメーション再生の指示を送信
        player.connection.send(new ClientboundAnimatePacket(player, arm == HumanoidArm.LEFT ? 3 : 0));
    }

    private static Map<UUID, Boolean> swingRight = new HashMap<>(); // プレイヤーごとの振り向きを記憶するマップ
    private static void SwingArm(ServerPlayer player) {
        if (WeaponTypeUtils.getWeaponType(player) == SkillData.WeaponType.DUALSWORD) {
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

    public static Boolean SkillTargetEntity(LivingEntity entity, Player owner) {
        if (entity == owner) { // entity が owner 自身の場合
            return false; // PvP ゲームルールに関係なく false を返す
        } else if (!ServerConfig.PvP.get() && entity instanceof Player) {
            return false;
        } else if (!ServerConfig.attackNeutralMobs.get() && entity instanceof Animal) {
            return false;
        }
        return true;
    }

}