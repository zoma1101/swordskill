package com.zoma1101.swordskill.swordskills.skill.axe;

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
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class GrandUpper implements ISkill {
    private static boolean isAttacked = false;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        if (FinalTick == 1) {
            isAttacked = false;
            // プレイヤーの向きベクトルを取得
            // 移動速度と距離を設定
            double moveSpeed = 3.0;

            // プレイヤーを吹き飛ばす
            Vec3 moveVec = lookVec.scale(moveSpeed);
            player.setDeltaMovement(moveVec.x, -2, moveVec.z);
            player.hasImpulse = true; // 重力の影響を受けないように設定

            // hurtMarked を true に設定
            player.hurtMarked = true;
            player.invulnerableTime = 10;
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
                        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
                        double damage = BaseDamage(player)*0.75f;
                        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
                        int duration = 12;
                        Vec3 Rotation = new Vec3(-6,20,95);
                        String skill_particle = AxeGreenSkillTexture();
                        SimpleSkillSound(level,spawnPos);
                        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, 0, duration,skill_particle,Vec3.ZERO);
                        entity.setDeltaMovement(0,4.5f,0);

                        MobEffectInstance levitationEffect = new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 1);
                        entity.addEffect(levitationEffect);
                        Vec3 reverseLookVec = lookVec.reverse().scale(2.5);
                        player.setDeltaMovement(player.getDeltaMovement().add(reverseLookVec));
                        player.hurtMarked = true;
                        isAttacked = true;
                        break;
                    }
                }
            }
            else {
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PlayAnimationPacket(SkillID,"move"));
            }
        }
    }
}
