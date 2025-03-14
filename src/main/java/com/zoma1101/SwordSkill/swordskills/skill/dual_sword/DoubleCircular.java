package com.zoma1101.SwordSkill.swordskills.skill.dual_sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import com.zoma1101.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

import static com.zoma1101.SwordSkill.server.handler.SkillExecutionManager.skillExecutions;
import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class DoubleCircular implements ISkill {
    private static boolean isAttacked = false;
    private static int NowTick = 0;
    private static int SkillFinalTick = 0;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            SkillFinalTick = SwordSkillRegistry.SKILLS.get(SkillID).getFinalTick();
            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 3.5;

            // プレイヤーを吹き飛ばす
            Vec3 moveVec = limitLookVec(player).scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, moveVec.y+0.5, moveVec.z);
            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 35;
        }

        if (!isAttacked) {
            // 周囲のエンティティを取得
            AABB boundingBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> SkillTargetEntity(entity,player)); // LivingEntity のみ取得

            // 接触判定と吹き飛ばし
            if (!entities.isEmpty()) {
                for (LivingEntity entity : entities) {
                    if (player.distanceTo(entity) < 1.5) {
                        NowTick = FinalTick;
                        isAttacked = true;
                        Vec3 reverseLookVec = player.getLookAngle().reverse().scale(1.25);
                        player.setDeltaMovement(player.getDeltaMovement().add(reverseLookVec));
                        // hurtMarked を true に設定
                        player.hurtMarked = true;
                        break;
                    }
                }
            }
        }
        else if (NowTick+6 <= SkillFinalTick) {
            if (FinalTick-NowTick == 1){
                performSlash(level,player,0,1f,1f);
            } else if (FinalTick-NowTick == 4){
                performThrust(level,player); //slashIndex:1
            } else if (FinalTick-NowTick == 8){
                performSlash(level,player,2,1f,2.5f);
                performSlash(level,player,3,1f,2.5f);
                skillExecutions.remove(player.getUUID());
            }
        }
    }

    private static Vec3 limitLookVec(ServerPlayer player) {
        Vec3 lookVec = player.getLookAngle();

        // y 成分を制限
        double maxY = Math.sin(Math.toRadians(15));
        double limitedY = Math.max(Math.min(lookVec.y, maxY), -maxY);

        // 制限された y 成分を使用して、新しい視線ベクトルを計算
        double xzLength = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
        double limitedX = xzLength * lookVec.x / xzLength * Math.cos(Math.asin(limitedY));
        double limitedZ = xzLength * lookVec.z / xzLength * Math.cos(Math.asin(limitedY));

        return new Vec3(limitedX, limitedY, limitedZ).normalize();
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,lookVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = RedSkillTexture();
        SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private void performThrust(Level level, ServerPlayer player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,lookVec, 1);
        double damage = BaseDamage(player) * (float) 1.5;
        double knockbackForce = BaseKnowBack(player)* 0.8f;
        Vector3f size = new Vector3f(0.75f, 0.75f, 7f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(1);
        String skill_particle = Spia_Particle_red();
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        SimpleSkillSound(level,spawnPos);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 0, -35); // 1回目の斬撃
            case 1 -> new Vec3(0, 0, 30); // 2回目の斬撃
            case 2 -> new Vec3(-15, 0, 45); // 2回目の斬撃
            case 3 -> new Vec3(-15, 0, 45); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> lookVec.scale(2);
            case 1 -> lookVec.scale(4);
            case 2 -> upVec.scale(0.2).add(lookVec.scale(2));
            case 3 -> upVec.scale(-0.2).add(lookVec.scale(2));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

}