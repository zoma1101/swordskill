package com.zoma1101.swordskill.swordskills.skill.spear;

import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.toClient.PlayAnimationPacket;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class FallingStar implements ISkill {
    private static boolean isAttacked = false;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        if (FinalTick == 1) {
            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 4.0;

            // プレイヤーを吹き飛ばす
            Vec3 moveVec = lookVec.scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 35;
            MobEffectInstance levitationEffect = new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 1);
            player.addEffect(levitationEffect);
        }
        if (FinalTick == 10) {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayAnimationPacket(SkillID,"start"));

            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 4.0;

            // プレイヤーを吹き飛ばす
            Vec3 moveVec = lookVec.scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 35;
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 100));
        }


        if (!isAttacked) {
            // 周囲のエンティティを取得
            AABB boundingBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> SkillTargetEntity(entity,player)); // LivingEntity のみ取得

            // 接触判定と吹き飛ばし
            if (!entities.isEmpty()) {
                for (LivingEntity entity : entities) {
                    if (player.distanceTo(entity) < 1.5) {
                        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayAnimationPacket(SkillID,"finish"));
                        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.7, 0).add(lookVec.scale(1)); // 目の前2ブロック
                        double damage = RushDamage(player)*3f;
                        double knockbackForce = BaseKnowBack(player)*0.5;
                        Vector3f size = new Vector3f(0.5f, 0.5f, 6f);
                        int duration = 12;
                        Vec3 Rotation = new Vec3(0,0,40);
                        String skill_particle = Spia_Particle();
                        SimpleSkillSound(level,spawnPos);
                        Vec3 Move = new Vec3(0,0,0.1);
                        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Move);

                        Vec3 reverseLookVec = lookVec.reverse().scale(3);
                        player.setDeltaMovement(player.getDeltaMovement().add(reverseLookVec));
                        player.hurtMarked = true;
                        isAttacked = true;
                        break;
                    }
                }
            }

        }
    }
}