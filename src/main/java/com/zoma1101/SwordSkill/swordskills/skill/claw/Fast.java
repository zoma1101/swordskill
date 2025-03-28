package com.zoma1101.swordskill.swordskills.skill.claw;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

import static com.zoma1101.swordskill.AnimationUtils.PlayerAnimation;
import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.GreenSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Fast implements ISkill {
    private static boolean isAttacked = false;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        if (FinalTick == 1) {
            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 5;
            // プレイヤーを吹き飛ばす
            Vec3 moveVec = lookVec.scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, -2, moveVec.z);
            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 10;
        }

        if (!isAttacked) {
            PlayerAnimation(SkillID,"move");
            // 周囲のエンティティを取得
            AABB boundingBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> SkillTargetEntity(entity,player)); // LivingEntity のみ取得

            // 接触判定と吹き飛ばし
            if (!entities.isEmpty()) {
                for (LivingEntity entity : entities) {
                    if (player.distanceTo(entity) < 1.5) {
                        PlayerAnimation(SkillID,"finish");
                        performNeil(level,player);
                        Vec3 reverseLookVec = lookVec.reverse().scale(1.5);
                        player.setDeltaMovement(player.getDeltaMovement().add(reverseLookVec));
                        player.hurtMarked = true;
                        isAttacked = true;
                        break;
                    }
                }
            }
        }
    }

    private void performNeil(Level level, ServerPlayer player){
        performSlash(level, player, 0);
        performSlash(level, player, 1);
        performSlash(level, player, 2);
        SimpleSkillSound(level,player.position());
    }


    private void performSlash(Level level, ServerPlayer player, int slashIndex) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = RushDamage(player) * 1.2f;
        double knockbackForce = BaseKnowBack(player)* (float) 0.95;
        Vector3f size = new Vector3f(4.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = GreenSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> upVec.scale(0.5).add(lookVec.scale(1.25));
            case 1 -> upVec.scale(0).add(lookVec.scale(1.25));
            case 2 -> upVec.scale(-0.5).add(lookVec.scale(1.25));
            default -> new Vec3(0, 0, 0);
        };
        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(6, 5, 3);
            case 1 -> new Vec3(-6, 5, 3);
            case 2 -> new Vec3(-12, 5, 3);
            default -> new Vec3(0, 0, 0);
        };
    }

}