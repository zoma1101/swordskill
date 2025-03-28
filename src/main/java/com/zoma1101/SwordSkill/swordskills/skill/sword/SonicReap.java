package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;
import static com.zoma1101.swordskill.server.handler.SkillExecutionManager.skillExecutions;
import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class SonicReap implements ISkill {
    private static boolean isAttacked = false;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        if (FinalTick == 1) {
            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 3.5;
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 160));
            // プレイヤーを吹き飛ばす
            Vec3 moveVec = lookVec.scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, moveVec.y+0.5, moveVec.z);
            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 35;
            MobEffectInstance levitationEffect = new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 1);
            player.addEffect(levitationEffect);
        }



        if (!isAttacked) {
            // 周囲のエンティティを取得
            AABB boundingBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> SkillTargetEntity(entity,player)); // LivingEntity のみ取得

            // 接触判定と吹き飛ばし
            if (!entities.isEmpty()) {
                for (LivingEntity entity : entities) {
                    if (player.distanceTo(entity) < 1.5) {
                        PlayerAnimation(SkillID,"finish");
                        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
                        double damage = RushDamage(player) * 2f;
                        double knockbackForce = BaseKnowBack(player);
                        Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
                        int duration = 12;
                        Vec3 Rotation = new Vec3(-20, 0, 35);

                        String skill_particle = NomalSkillTexture();
                        SimpleSkillSound(level, spawnPos);
                        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
                        Vec3 reverseLookVec = lookVec.reverse().scale(3);
                        player.setDeltaMovement(player.getDeltaMovement().add(reverseLookVec));
                        player.hurtMarked = true;
                        isAttacked = true;
                        break;
                    }
                }
            }
            else {
                PlayerAnimation(SkillID,"move");
            }
        }
        if (FinalTick > 5 && player.onGround()){
            skillExecutions.remove(player.getUUID());
            PlayerAnimation(0,"");
        }
    }
}