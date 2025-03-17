package com.zoma1101.SwordSkill.swordskills.skill.dagger;

import com.zoma1101.SwordSkill.effects.EffectRegistry;
import com.zoma1101.SwordSkill.swordskills.ISkill;
import com.zoma1101.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

import static com.zoma1101.SwordSkill.client.handler.ClientForgeHandler.setCooldowns;
import static com.zoma1101.SwordSkill.server.handler.SkillExecutionManager.skillExecutions;
import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class RapidBite implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1 && player.onGround()) {
            player.setDeltaMovement(new Vec3(0, 1.5, 0));
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 150));
            player.invulnerableTime = 35;
        } else if (FinalTick >= 3) {
            if (!player.onGround()) {
            // 周囲のエンティティを取得
            AABB boundingBox = player.getBoundingBox().inflate(8.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, boundingBox, entity -> SkillTargetEntity(entity, player)); // LivingEntity のみ取得
            // 接触判定と吹き飛ばし
            if (!entities.isEmpty()) {
                for (LivingEntity entity : entities) {
                    if (player.distanceTo(entity) < 3) {
                        Vec3 AttackRotation = player.position().subtract(entity.position()).normalize();
                        Vec3 SpawnPos = entity.position().add(AttackRotation.scale(2));
                        float yaw = (float) ((Math.toDegrees(Math.atan2(AttackRotation.z, AttackRotation.x))));
                        float pitch = (float) Math.toDegrees(Math.asin(AttackRotation.y));
                        performSlash(level, player, Spia_Particle_AxeGreen(),pitch,yaw,SpawnPos);
                        SimpleSkillSound(level, player.position());
                        player.setDeltaMovement(AttackRotation.scale(1.25).add(0,0.5,0));
                        player.hurtMarked = true;
                        int SkillCooldown = SwordSkillRegistry.SKILLS.get(SkillID).getCooldown();
                        setCooldowns(SkillID, SkillCooldown/5);
                        skillExecutions.remove(player.getUUID());
                        break;
                    }
                }
            }}
            else {
                skillExecutions.remove(player.getUUID());
            }
        }

    }

    private void performSlash(Level level, ServerPlayer player, String Texture, float pitch, float yaw, Vec3 SpawnPos) {

        Vec3 Rotation = new Vec3(pitch-player.getXRot(),90+yaw-player.getYRot(),30);
        double damage = RushDamage(player) * 3.5f;
        double knockbackForce = BaseKnowBack(player)* (float) 0.5;
        Vector3f size = new Vector3f(0.35f,0.35f,6f);
        int duration = 12;
        spawnAttackEffect(level, SpawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

}