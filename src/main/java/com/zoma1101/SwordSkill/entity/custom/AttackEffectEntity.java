package com.zoma1101.SwordSkill.entity.custom;

import com.zoma1101.SwordSkill.swordskills.SkillTexture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import java.util.List;
import java.util.Objects;

import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.SkillTargetEntity;

public class AttackEffectEntity extends Entity {

    private static final EntityDataAccessor<Float> ROTATION_Z = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_X = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Y = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Z = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> SKILL_PARTICLE = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.STRING);

    private double damage = 4.0f; // ダメージ量
    private double knockbackStrength = 0.5; // ノックバック強さ
    private int duration = 15; // 生存時間 (tick)
    private LivingEntity owner;
    private Vec3 movement = new Vec3(0,0,2);

    public AttackEffectEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROTATION_Z, 0.0f);
        this.entityData.define(EFFECT_RADIUS_X,2f);
        this.entityData.define(EFFECT_RADIUS_Y,2f);
        this.entityData.define(EFFECT_RADIUS_Z,2f);
        this.entityData.define(SKILL_PARTICLE,"skill_particle");
    }
    private boolean hasAppliedDamage = false;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (!hasAppliedDamage) {
                if (SkillTexture.Spia_ParticleType.contains(this.getSkillParticle())) {
                    applyRayDamage();
                } else if (!SkillTexture.Spia_ParticleType.contains(this.getSkillParticle())) {
                    applyDamageAndKnockback();
                }
            }
            Movement();
        }
        if (this.tickCount >= duration) {
            this.discard();
        }
    }

    private void applyDamageAndKnockback() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        AABB area = this.getBoundingBox().inflate((getEffectRadius().x + getEffectRadius().y + getEffectRadius().z) / 5);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> (SkillTargetEntity(entity,owner)));

        for (LivingEntity entity : entities) {
            ApplyDamage(entity);
            KnowBack(entity);
        }
    }

    private void applyRayDamage() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 direction = this.getLookAngle();

        double rayLength = getEffectRadius().z * 0.5 ;

        // プレイヤーの位置ベクトルを取得


        // 視点方向の Ray の始点と終点を計算
        Vec3 rayStart = this.position().add(direction.scale(rayLength));
        Vec3 rayEnd = this.position().add(direction.scale(-rayLength));

        if (!Objects.equals(this.getSkillParticle(), FlashingPenetrator_Texture())) {
            Vec3 playerPos = owner != null ? owner.position() : this.position();
            // 終点がエンティティとowner間の距離より遠い場合はownerの位置を終点にする
            //フラッシングペネトレーターは除く
            double distanceToOwner = rayStart.distanceTo(playerPos);
            if (rayStart.distanceTo(rayEnd) > distanceToOwner) {
                rayEnd = playerPos;
            }
        }

        AABB expandedBoundingBox = this.getBoundingBox().inflate(rayLength); // 範囲を調整

        // 視点方向の Ray の判定
        List<LivingEntity> entitiesInRange = serverLevel.getEntitiesOfClass(LivingEntity.class,
                expandedBoundingBox,
                entity -> (SkillTargetEntity(entity, owner)));

        Vec3 finalRayEnd = rayEnd;
        List<LivingEntity> hitEntities = entitiesInRange.stream()
                .filter(entity -> isEntityInRay(entity, rayStart, finalRayEnd))
                .toList();

        for (LivingEntity entity : hitEntities) {
            ApplyDamage(entity);
            if (owner != null) {
                Vec3 knockbackDir = (this.position().add(owner.position().scale(-1))).scale(knockbackStrength);
                entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
                hasAppliedDamage = true;
            }
        }
    }

    private boolean isEntityInRay(LivingEntity entity, Vec3 rayStart, Vec3 rayEnd) {
        AABB entityAABB = entity.getBoundingBox();
        return entityAABB.intersects(rayStart, rayEnd);
    }

    private void ApplyDamage(LivingEntity entity) {
        float DamagePer = 1;

        if (Objects.equals(this.getSkillParticle(), YellowSkillTexture()) && entity.getMobType() == MobType.UNDEAD) {
            DamagePer = 3f;
        }
        if (Objects.equals(this.getSkillParticle(), RedSkillTexture()) && entity.getMobType() != MobType.UNDEAD) {
            DamagePer = 1.25f;
            if (owner != null) { // owner が null でないかチェック
                owner.heal((float) (damage * 0.25f));
            }
        }
        if (Objects.equals(this.getSkillParticle(), AxeBloodSkillTexture()) && entity.getMaxHealth() / 2 >= entity.getHealth()) {
            DamagePer = 2.5f;
        }
        if (Objects.equals(this.getSkillParticle(), AxeKingSkillTexture())) {
            entity.hurt(this.damageSources().magic(), (float) (damage * 0.5f));
            entity.invulnerableTime = 0;
        }

        entity.hurt(this.damageSources().mobAttack(owner), (float) (damage * DamagePer));
        entity.invulnerableTime = 0;
    }

    private void KnowBack(LivingEntity entity){
        if (owner != null) {
            //Vec3 knockbackDir = (this.position().subtract(owner.position())).scale(knockbackStrength);

            Vec3 knockbackDir = (entity.position().subtract(this.position())).normalize().scale(knockbackStrength);

            entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
        }
    }



    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag compound) {}

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setKnockbackStrength(double knockbackStrength) {
        this.knockbackStrength = knockbackStrength;
    }

    public void setEffectRadius(Vector3f effectRadius) {
        this.entityData.set(EFFECT_RADIUS_X, effectRadius.x);
        this.entityData.set(EFFECT_RADIUS_Y, effectRadius.y);
        this.entityData.set(EFFECT_RADIUS_Z, effectRadius.z);
    }

    public void setDuration(int duration) { this.duration = duration; }

    public void setMovement(Vec3 movement) { this.movement = movement; }

    public void setSkillParticle(String skillParticle) {this.entityData.set(SKILL_PARTICLE,skillParticle);
    }

    public void setRotation(float rotationZ) {
        this.entityData.set(ROTATION_Z, rotationZ);
    }

    public float getRotation() {
        return this.entityData.get(ROTATION_Z);
    }
    public Vector3f getEffectRadius(){
        return new Vector3f(
                this.entityData.get(EFFECT_RADIUS_X),
                this.entityData.get(EFFECT_RADIUS_Y),
                this.entityData.get(EFFECT_RADIUS_Z)
        );
    }
    public String getSkillParticle(){
        return this.entityData.get(SKILL_PARTICLE);
    }


    public void setOwner(LivingEntity owner) { this.owner = owner; }

    private void Movement() {
        if (movement != Vec3.ZERO) {
            Vec3 direction = Vec3.directionFromRotation(this.getRotationVector());
            Vec3 velocity = direction.scale(movement.z / duration);

            // 視点から見て上方向への移動
            Vec3 upDirection = new Vec3(0, 1, 0).yRot((float) Math.toRadians(-this.getYRot()));
            Vec3 upVelocity = upDirection.scale(movement.y / duration);

            // 視点から見て右方向への移動
            Vec3 rightDirection = direction.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 rightVelocity = rightDirection.scale(movement.x / duration);

            // 全ての移動ベクトルを加算
            Vec3 totalVelocity = velocity.add(upVelocity).add(rightVelocity);

            this.setPos(this.position().add(totalVelocity));
        }
        else {
            hasAppliedDamage = true;
        }
    }

}
