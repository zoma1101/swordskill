package com.zoma1101.SwordSkill.entity.custom;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.SkillTargetEntity;

public class AttackEffectEntity extends Entity {

    private static final EntityDataAccessor<Float> ROTATION_X = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_Y = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROTATION_Z = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_X = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Y = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Z = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> SKILL_PARTICLE = SynchedEntityData.defineId(AttackEffectEntity.class, EntityDataSerializers.STRING);

    private double damage = 4.0f; // ダメージ量
    private double knockbackStrength = 0.5; // ノックバック強さ
    private int duration = 15; // 生存時間 (tick)
    private ServerPlayer owner;

    public AttackEffectEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROTATION_X, 0.0f);
        this.entityData.define(ROTATION_Y, 50.0f);
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

        if (!this.level().isClientSide && !hasAppliedDamage) {
            if (Objects.equals(this.getSkillParticle(), Spia_Particle())) {
                applyRayDamage();
            } else if (Objects.equals(this.getSkillParticle(), Spia_Particle_AxeGreen())) {
                applyRayDamage();
            }
            else if (!Objects.equals(this.getSkillParticle(), Spia_Particle()) && !Objects.equals(this.getSkillParticle(), Spia_Particle_AxeGreen())) {
                applyDamageAndKnockback();
            }

            hasAppliedDamage = true;
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
            if (owner != null) { // owner が null でないかチェック
                Vec3 knockbackDir = (this.position().add(owner.position().scale(-1))).scale(knockbackStrength);
                entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
            }
        }
    }

    private void applyRayDamage() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Vec3 rotation = getRotation();
        Vec3 direction = new Vec3(Math.cos(Math.toRadians(rotation.y)) * Math.cos(Math.toRadians(rotation.x)),
                Math.sin(Math.toRadians(rotation.x)),
                Math.sin(Math.toRadians(rotation.y)) * Math.cos(Math.toRadians(rotation.x))).normalize();

        // 視点方向を Y 軸周りに 90 度回転
        Vec3 rotatedDirection = direction.yRot((float) Math.toRadians(90));

        // 逆方向のベクトルを計算
        Vec3 reverseRotatedDirection = rotatedDirection.scale(-1);

        double rayLength = getEffectRadius().z;
        double reverse_rayLength = rayLength;
        System.out.println("射程は"+rayLength);

        // プレイヤーの位置ベクトルを取得
        Vec3 playerPos = owner != null ? owner.position() : this.position(); // owner が null の場合はエンティティの位置を使用

        // 逆方向の Ray の長さを調整
        double distanceToPlayer = this.position().distanceTo(playerPos);
        if (reverse_rayLength > distanceToPlayer) {
            reverse_rayLength = distanceToPlayer;
        }

        // 視点方向の Ray
        Vec3 rayStart = this.position();
        Vec3 rayEnd = rayStart.add(rotatedDirection.scale(rayLength));

        // 逆方向の Ray
        Vec3 reverseRayStart = this.position();
        Vec3 reverseRayEnd = reverseRayStart.add(reverseRotatedDirection.scale(reverse_rayLength));

        AABB expandedBoundingBox = this.getBoundingBox().inflate(rayLength / 2);

        // 視点方向の Ray の判定
        List<LivingEntity> entitiesInRange = serverLevel.getEntitiesOfClass(LivingEntity.class,
                expandedBoundingBox,
                entity -> (SkillTargetEntity(entity,owner)));

        List<LivingEntity> hitEntities = entitiesInRange.stream()
                .filter(entity -> isEntityInRay(entity, rayStart, rayEnd))
                .collect(Collectors.toList());

        for (LivingEntity entity : hitEntities) {

            ApplyDamage(entity);
            if (owner != null) {
                Vec3 knockbackDir = (this.position().add(owner.position().scale(-1))).scale(knockbackStrength);
                entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
            }
        }

        // 逆方向の Ray の判定
        List<LivingEntity> reverseHitEntities = entitiesInRange.stream()
                .filter(entity -> isEntityInRay(entity, reverseRayStart, reverseRayEnd))
                .collect(Collectors.toList());

        for (LivingEntity entity : reverseHitEntities) {
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


    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {}

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

    public void setSkillParticle(String skillParticle) {this.entityData.set(SKILL_PARTICLE,skillParticle);
    }

    //基本Rot(entity.getXRot(), -entity.getYRot(), 0)
    public void setRotation(Vec3 rotation) {
        this.entityData.set(ROTATION_X, (float) rotation.x);
        this.entityData.set(ROTATION_Y, (float) rotation.y);
        this.entityData.set(ROTATION_Z, (float) rotation.z);
    }

    public Vec3 getRotation() {
        return new Vec3(
                this.entityData.get(ROTATION_X),
                this.entityData.get(ROTATION_Y),
                this.entityData.get(ROTATION_Z)
        );
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


    public void setOwner(ServerPlayer owner) { this.owner = owner; }
}
