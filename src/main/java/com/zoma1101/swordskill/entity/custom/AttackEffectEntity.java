package com.zoma1101.swordskill.entity.custom;

import com.zoma1101.swordskill.swordskills.SkillTexture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.SkillTargetEntity;

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

    public AttackEffectEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROTATION_Z, 0.0f);
        builder.define(EFFECT_RADIUS_X, 2f);
        builder.define(EFFECT_RADIUS_Y, 2f);
        builder.define(EFFECT_RADIUS_Z, 2f);
        builder.define(SKILL_PARTICLE, "skill_particle");
    }

    private boolean hasAppliedDamage = false;

    @Override
    public void tick() {
        super.tick();
        Movement();
        if (!this.level().isClientSide) {
            if (!hasAppliedDamage) {
                if (SkillTexture.Spia_ParticleType.contains(this.getSkillParticle())) {
                    applyRayDamage();
                } else if (!SkillTexture.Spia_ParticleType.contains(this.getSkillParticle())) {
                    applyDamageAndKnockback();
                }
                hasAppliedDamage = this.getDeltaMovement().equals(Vec3.ZERO);
            }

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
            KnowBack(entity);
            ApplyDamage(entity);
        }
    }
    private void applyRayDamage() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 direction = this.getLookAngle();

        double rayLength = getEffectRadius().z * 0.5 ;

        // 視点方向の Ray の始点と終点を計算
        Vec3 rayStart = this.position().add(direction.scale(rayLength));
        Vec3 rayEnd = this.position().add(direction.scale(-rayLength));

        if (!Objects.equals(this.getSkillParticle(), FlashingPenetrator_Texture()) || this.getDeltaMovement() != Vec3.ZERO) {
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
            }
        }
    }
    private boolean isEntityInRay(LivingEntity entity, Vec3 rayStart, Vec3 rayEnd) {
        AABB entityAABB = entity.getBoundingBox();
        return entityAABB.intersects(rayStart, rayEnd);
    }
    private void ApplyDamage(LivingEntity entity) {
        float DamagePer = 1;

        if (Objects.equals(this.getSkillParticle(), YellowSkillTexture()) && entity.getTags().contains(EntityTypeTags.UNDEAD)) {
            DamagePer = 3f;
        }
        else if (Objects.equals(this.getSkillParticle(), BlackSkillTexture()) && entity.getTags().contains(EntityTypeTags.UNDEAD)) {
            DamagePer = 2.5f;
        }
        else if (Objects.equals(this.getSkillParticle(), RedSkillTexture()) && entity.getTags().contains(EntityTypeTags.UNDEAD)) {
            DamagePer = 1.25f;
            if (owner != null) { // owner が null でないかチェック
                owner.heal((float) (damage * 0.25f));
            }
        }
        else if (Objects.equals(this.getSkillParticle(), AxeBloodSkillTexture()) && entity.getMaxHealth() / 2 >= entity.getHealth()) {
            DamagePer = 2.5f;
        }
        else if (Objects.equals(this.getSkillParticle(), AxeKingSkillTexture())) {
            entity.hurt(this.damageSources().magic(), (float) (damage * 0.5f));
            entity.invulnerableTime = 0;
        }
        else if (Objects.equals(this.getSkillParticle(), GoldSkillTexture())) {
            entity.hurt(this.damageSources().magic(), (float) (damage * 0.25f));
            entity.invulnerableTime = 0;
        }
        else if (Mace_ParticleType.contains(this.getSkillParticle())) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100,1));
        }

        entity.hurt(this.damageSources().mobAttack(owner), (float) (damage * DamagePer));
        if (getDeltaMovement() == Vec3.ZERO){
            entity.invulnerableTime = 0;
        }
        else {
            entity.invulnerableTime = 8;
        }
    }
    private void KnowBack(LivingEntity entity){
        if (owner != null && entity.invulnerableTime == 0) {
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
    public void setMovement(Vec3 movement) {
        Vec3 direction = Vec3.directionFromRotation(this.getRotationVector());
        Vec3 velocity = direction.scale(movement.z / duration);

        // 視点から見て上方向への移動
        Vec3 upDirection = new Vec3(0, 1, 0).yRot((float) Math.toRadians(-this.getYRot()));
        Vec3 upVelocity = upDirection.scale(movement.y / duration);

        // 視点から見て右方向への移動
        Vec3 rightDirection = direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 rightVelocity = rightDirection.scale(movement.x / duration);
        // 全ての移動ベクトルを加算
        setDeltaMovement(velocity.add(upVelocity).add(rightVelocity).scale(0.8f));
    }
    public void setSkillParticle(String skillParticle) {this.entityData.set(SKILL_PARTICLE,skillParticle);
    }
    public void setRotation(float rotationZ) {
        this.entityData.set(ROTATION_Z, rotationZ);
    }
    public void setOwner(LivingEntity owner) { this.owner = owner; }

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

    private void Movement(){
        Vec3 vec3 = getDeltaMovement();
        double d2 = this.getX() + vec3.x;
        double d0 = this.getY() + vec3.y;
        double d1 = this.getZ() + vec3.z;
        Vec3 movement = new Vec3(d2,d0,d1);
        this.setDeltaMovement(vec3.scale(0.99));
        this.move(MoverType.SELF,vec3);
        if (!this.position().equals(movement) && getDeltaMovement() != Vec3.ZERO){
            this.discard();
        }//移動するソードスキルが壁に当たると消滅する。消滅時の処理を記述
    }

}
