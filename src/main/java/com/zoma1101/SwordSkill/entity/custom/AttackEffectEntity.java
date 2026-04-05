package com.zoma1101.swordskill.entity.custom;

import com.zoma1101.swordskill.swordskills.SkillTag;
import com.zoma1101.swordskill.swordskills.SkillTexture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.SkillTargetEntity;

public class AttackEffectEntity extends Entity {

    private final Set<Integer> damagedEntityIds = new HashSet<>();

    private static final EntityDataAccessor<Float> ROTATION_Z = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_X = SynchedEntityData
            .defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Y = SynchedEntityData
            .defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EFFECT_RADIUS_Z = SynchedEntityData
            .defineId(AttackEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> SKILL_PARTICLE = SynchedEntityData
            .defineId(AttackEffectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKILL_TAGS = SynchedEntityData
            .defineId(AttackEffectEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> FOLLOW_OWNER = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TRAIL_COLOR = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.INT);
    // クライアントで確実に速度を再現するための同期データ
    private static final EntityDataAccessor<Float> MOVEMENT_X = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOVEMENT_Y = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MOVEMENT_Z = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_X_ROT = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SYNC_Y_ROT = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_X_ROT = SynchedEntityData.defineId(AttackEffectEntity.class,
            EntityDataSerializers.FLOAT);

    private double damage = 4.0; // ダメージ量
    private double knockbackStrength = 0.5; // ノックバック強さ
    private int duration = 15; // 生存時間 (tick)
    private LivingEntity owner;
    private float relativeXRot = 0;
    private float relativeYRot = 0;
    private boolean rotationOffsetInitialized = false;

    public AttackEffectEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // 壁やプレイヤーの当たり判定で移動が止まるのを防ぐ
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ROTATION_Z, 0.0f);
        this.entityData.define(EFFECT_RADIUS_X, 2f);
        this.entityData.define(EFFECT_RADIUS_Y, 2f);
        this.entityData.define(EFFECT_RADIUS_Z, 2f);
        this.entityData.define(SKILL_PARTICLE, "skill_particle");
        this.entityData.define(SKILL_TAGS, "");
        this.entityData.define(TRAIL_COLOR, 0xFF33AAFF);
        this.entityData.define(FOLLOW_OWNER, false);
        this.entityData.define(OWNER_ID, -1); // -1 = オーナー未設定
        this.entityData.define(MOVEMENT_X, 0.0f);
        this.entityData.define(MOVEMENT_Y, 0.0f);
        this.entityData.define(MOVEMENT_Z, 0.0f);
        this.entityData.define(SYNC_X_ROT, 0.0f);
        this.entityData.define(SYNC_Y_ROT, 0.0f);
        this.entityData.define(VISUAL_X_ROT, 0.0f);
    }

    @Override
    public void setXRot(float xRot) {
        super.setXRot(xRot);
        if (this.entityData != null && this.level() != null && !this.level().isClientSide) {
            this.entityData.set(SYNC_X_ROT, xRot);
        }
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
        if (this.entityData != null && this.level() != null && !this.level().isClientSide) {
            this.entityData.set(SYNC_Y_ROT, yRot);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.tickCount <= 3) {
            // クライアント側で確実に初速度を同期する
            this.setDeltaMovement(
                    this.entityData.get(MOVEMENT_X),
                    this.entityData.get(MOVEMENT_Y),
                    this.entityData.get(MOVEMENT_Z));
        }

        if (isFollowOwner() && owner != null) {
            if (!rotationOffsetInitialized) {
                relativeXRot = getXRot() - owner.getXRot();
                relativeYRot = getYRot() - owner.getYRot();
                rotationOffsetInitialized = true;
            }
            this.setPos(owner.getX(), owner.getY() + owner.getEyeHeight() * 0.5, owner.getZ());
            // 視点に合わせて回転も更新する（オフセットを維持）
            this.setXRot(owner.getXRot() + relativeXRot);
            this.setYRot(owner.getYRot() + relativeYRot);
            this.xRotO = this.getXRot(); // 補間の跳びを防ぐ
            this.yRotO = this.getYRot();
        }

        Movement();
        if (this.level().isClientSide) {
            spawnAttributeParticles();
        }
        if (!this.level().isClientSide) {
            // 移動速度がある（飛ぶ斬撃など）場合は毎tick判定を行う
            // 速度がない（通常の突き・叩きつけなど）場合は最初の1tick目のみ判定を行う
            boolean isMoving = this.getDeltaMovement().lengthSqr() > 0.0001;
            if (isMoving || this.tickCount <= 1) {
                if (hasTag(SkillTag.RAY) || SkillTexture.Spia_ParticleType.contains(this.getSkillParticle())) {
                    applyRayDamage();
                } else {
                    applyDamageAndKnockback();
                }
            }
        }
        if (this.tickCount >= duration) {
            this.discard();
        }
    }

    private void applyDamageAndKnockback() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;

        AABB area = this.getBoundingBox()
                .inflate((getEffectRadius().x + getEffectRadius().y + getEffectRadius().z) / 5);
        List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> (SkillTargetEntity(entity, owner)));

        for (LivingEntity entity : entities) {
            if (!damagedEntityIds.contains(entity.getId())) {
                KnowBack(entity);
                ApplyDamage(entity);
                damagedEntityIds.add(entity.getId());
            }
        }
    }

    private void applyRayDamage() {
        if (!(this.level() instanceof ServerLevel serverLevel))
            return;
        Vec3 direction = this.getLookAngle();

        double rayLength = getEffectRadius().z * 0.5;

        // 視点方向の Ray の始点と終点を計算
        Vec3 rayStart = this.position().add(direction.scale(rayLength));
        Vec3 rayEnd = this.position().add(direction.scale(-rayLength));

        if (!Objects.equals(this.getSkillParticle(), FlashingPenetrator_Texture())
                || this.getDeltaMovement() != Vec3.ZERO) {
            Vec3 playerPos = owner != null ? owner.position() : this.position();
            // 終点がエンティティとowner間の距離より遠い場合はownerの位置を終点にする
            // フラッシングペネトレーターは除く
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

        float radius = getEffectRadius().x; // 横幅を判定に反映させる
        Vec3 finalRayEnd = rayEnd;
        List<LivingEntity> hitEntities = entitiesInRange.stream()
                .filter(entity -> isEntityInRay(entity, rayStart, finalRayEnd, radius))
                .toList();

        for (LivingEntity entity : hitEntities) {
            if (!damagedEntityIds.contains(entity.getId())) {
                ApplyDamage(entity);
                if (owner != null) {
                    Vec3 knockbackDir = (this.position().subtract(owner.position())).normalize().scale(knockbackStrength);
                    entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
                }
                damagedEntityIds.add(entity.getId());
            }
        }
    }

    private boolean isEntityInRay(LivingEntity entity, Vec3 rayStart, Vec3 rayEnd, float radius) {
        // エンティティの判定ボックスをスキルの横幅分だけ膨らませてから線分交差判定を行う
        AABB entityAABB = entity.getBoundingBox().inflate(radius * 0.5f);
        return entityAABB.intersects(rayStart, rayEnd);
    }

    private void ApplyDamage(LivingEntity entity) {
        float DamagePer = 1;

        // タグベースの判定
        List<SkillTag> tags = getSkillTags();

        if (tags.contains(SkillTag.HOLY) && entity.getMobType() == MobType.UNDEAD) {
            DamagePer = 3f;
        }
        if (tags.contains(SkillTag.HOLY_ENCHANT) && entity.getMobType() == MobType.UNDEAD) {
            DamagePer = 1.5f;
        }
        if (tags.contains(SkillTag.DARK) && entity.getMobType() != MobType.UNDEAD) {
            DamagePer = 2.5f;
        }
        if (tags.contains(SkillTag.BLOOD)) {
            if (owner != null) {
                owner.heal((float) (damage * 0.25f));
            }
            if (entity.getMaxHealth() / 2 >= entity.getHealth()) {
                DamagePer *= 1.25f;
            }
        }
        if (tags.contains(SkillTag.EXECUTION) && entity.getMaxHealth() / 2 >= entity.getHealth()) {
            DamagePer = 2.5f;
        }
        if (tags.contains(SkillTag.MAGIC)) {
            entity.hurt(this.damageSources().magic(), (float) (damage * 0.5f));
            entity.invulnerableTime = 0;
        }
        if (tags.contains(SkillTag.SLOWNESS)) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        }
 
        // 属性エンチャント効果
        if (tags.contains(SkillTag.FIRE)) {
            entity.setSecondsOnFire(5);
        }
        if (tags.contains(SkillTag.WATER)) {
            entity.clearFire();
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        if (tags.contains(SkillTag.WIND)) {
            entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10, 2));
            if (owner != null) {
                Vec3 push = entity.position().subtract(owner.position()).normalize().scale(1.5 * knockbackStrength);
                entity.push(push.x, 0.2, push.z);
            }
        }
        if (tags.contains(SkillTag.DARK)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
        }
        if (tags.contains(SkillTag.SONIC)) {
            entity.hurt(this.damageSources().sonicBoom(owner), (float) (damage));
            entity.invulnerableTime = 0;
            // 以降の hurt 処理をスキップするためにダメージ倍率を 0 にする
            DamagePer = 0;
        }

        // 互換性のための古いパーティクル名判定
        if (tags.isEmpty()) {
            if (Objects.equals(this.getSkillParticle(), YellowSkillTexture())
                    && entity.getMobType() == MobType.UNDEAD) {
                DamagePer = 3f;
            } else if (Objects.equals(this.getSkillParticle(), BlackSkillTexture())
                    && entity.getMobType() != MobType.UNDEAD) {
                DamagePer = 2.5f;
            } else if (Objects.equals(this.getSkillParticle(), RedSkillTexture())
                    && entity.getMobType() != MobType.UNDEAD) {
                DamagePer = 1.25f;
                if (owner != null) {
                    owner.heal((float) (damage * 0.25f));
                }
            } else if (Objects.equals(this.getSkillParticle(), AxeBloodSkillTexture())
                    && entity.getMaxHealth() / 2 >= entity.getHealth()) {
                DamagePer = 2.5f;
            } else if (Objects.equals(this.getSkillParticle(), AxeKingSkillTexture())
                    || Objects.equals(this.getSkillParticle(), GoldSkillTexture())) {
                entity.hurt(this.damageSources().magic(), (float) (damage * 0.5f));
                entity.invulnerableTime = 0;
            } else if (Mace_ParticleType.contains(this.getSkillParticle())) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            }
        }

        entity.hurt(this.damageSources().mobAttack(owner), (float) (damage * DamagePer));
        if (getDeltaMovement() == Vec3.ZERO) {
            entity.invulnerableTime = 0;
        } else {
            entity.invulnerableTime = 8;
        }
    }

    private void KnowBack(LivingEntity entity) {
        if (owner != null && entity.invulnerableTime == 0) {
            Vec3 knockbackDir = (entity.position().subtract(this.position())).normalize().scale(knockbackStrength);
            entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength, knockbackDir.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag compound) {
        if (compound.contains("TrailColor")) {
            this.setTrailColor(compound.getInt("TrailColor"));
        }
        if (compound.contains("EffectRadiusX")) {
            this.setEffectRadius(new Vector3f(
                compound.getFloat("EffectRadiusX"),
                compound.getFloat("EffectRadiusY"),
                compound.getFloat("EffectRadiusZ")
            ));
        }
        if (compound.contains("Damage")) {
            this.damage = compound.getDouble("Damage");
        }
        if (compound.contains("Knockback")) {
            this.knockbackStrength = compound.getDouble("Knockback");
        }
        if (compound.contains("Duration")) {
            this.duration = compound.getInt("Duration");
        }
        if (compound.contains("SkillParticle")) {
            this.setSkillParticle(compound.getString("SkillParticle"));
        }
        if (compound.contains("SkillTags")) {
            this.setSkillTags(SkillTag.fromString(compound.getString("SkillTags")));
        }
        if (compound.contains("FollowOwner")) {
            this.setFollowOwner(compound.getBoolean("FollowOwner"));
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.@NotNull CompoundTag compound) {
        compound.putInt("TrailColor", this.getTrailColor());
        Vector3f radius = this.getEffectRadius();
        compound.putFloat("EffectRadiusX", radius.x);
        compound.putFloat("EffectRadiusY", radius.y);
        compound.putFloat("EffectRadiusZ", radius.z);
        compound.putDouble("Damage", this.damage);
        compound.putDouble("Knockback", this.knockbackStrength);
        compound.putInt("Duration", this.duration);
        compound.putString("SkillParticle", this.getSkillParticle());
        compound.putString("SkillTags", this.entityData.get(SKILL_TAGS));
        compound.putBoolean("FollowOwner", this.isFollowOwner());
    }

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

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setMovement(Vec3 movement) {
        Vec3 direction = Vec3.directionFromRotation(this.getRotationVector());
        Vec3 velocity = direction.scale(movement.z / duration);

        Vec3 upDirection = new Vec3(0, 1, 0).yRot((float) Math.toRadians(-this.getYRot()));
        Vec3 upVelocity = upDirection.scale(movement.y / duration);

        Vec3 rightDirection = direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 rightVelocity = rightDirection.scale(movement.x / duration);
        Vec3 resultVelocity = velocity.add(upVelocity).add(rightVelocity).scale(0.8f);
        setDeltaMovement(resultVelocity);

        // クライアントに初速度を同期するためEntityDataに保存
        this.entityData.set(MOVEMENT_X, (float) resultVelocity.x);
        this.entityData.set(MOVEMENT_Y, (float) resultVelocity.y);
        this.entityData.set(MOVEMENT_Z, (float) resultVelocity.z);
    }

    public void setSkillParticle(String skillParticle) {
        this.entityData.set(SKILL_PARTICLE, skillParticle);
    }

    public void setSkillTags(List<SkillTag> tags) {
        this.entityData.set(SKILL_TAGS, SkillTag.toString(tags));
    }

    public List<SkillTag> getSkillTags() {
        return SkillTag.fromString(this.entityData.get(SKILL_TAGS));
    }

    public boolean hasTag(SkillTag tag) {
        return getSkillTags().contains(tag);
    }

    public void setTrailColor(int color) {
        this.entityData.set(TRAIL_COLOR, color);
    }

    public int getTrailColor() {
        return this.entityData.get(TRAIL_COLOR);
    }

    public void setRotation(float rotationZ) {
        this.entityData.set(ROTATION_Z, rotationZ);
    }

    public void setFollowOwner(boolean follow) {
        this.entityData.set(FOLLOW_OWNER, follow);
    }

    public boolean isFollowOwner() {
        return this.entityData.get(FOLLOW_OWNER);
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
        this.entityData.set(OWNER_ID, owner != null ? owner.getId() : -1);
    }

    public int getOwnerId() {
        return this.entityData.get(OWNER_ID);
    }

    public LivingEntity getOwner() {
        if (this.owner == null) {
            int id = this.entityData.get(OWNER_ID);
            if (id != -1) {
                Entity found = this.level().getEntity(id);
                if (found instanceof LivingEntity living) {
                    this.owner = living;
                }
            }
        }
        return this.owner;
    }

    public float getRotation() {
        return this.entityData.get(ROTATION_Z);
    }

    public Vector3f getEffectRadius() {
        return new Vector3f(
                this.entityData.get(EFFECT_RADIUS_X),
                this.entityData.get(EFFECT_RADIUS_Y),
                this.entityData.get(EFFECT_RADIUS_Z));
    }

    public String getSkillParticle() {
        return this.entityData.get(SKILL_PARTICLE);
    }

    public float getSyncXRot() {
        return this.entityData.get(SYNC_X_ROT);
    }

    public float getSyncYRot() {
        return this.entityData.get(SYNC_Y_ROT);
    }

    public void setVisualXRot(float visualXRot) {
        this.entityData.set(VISUAL_X_ROT, visualXRot);
    }

    public float getVisualXRot() {
        return this.entityData.get(VISUAL_X_ROT);
    }

    private void spawnAttributeParticles() {
        List<SkillTag> tags = getSkillTags();
        if (tags.isEmpty()) return;

        Vector3f rVec = getEffectRadius();
        float radius = rVec.x;

        // 1. 基本的な発生位置の決定
        for (int i = 0; i < 3; i++) {
            Vec3 particlePos;

            if (tags.contains(SkillTag.RAY) || tags.contains(SkillTag.SHAPE_THRUST)) {
                // 直線状（突き・ビーム）: 視線方向の線上に配置
                float dist = (this.random.nextFloat() - 0.5f) * rVec.z * 1.2f;
                // 波動（SHAPE_THRUST）の場合は少し周囲にも散らす
                float spread = tags.contains(SkillTag.SHAPE_THRUST) ? 0.3f : 0.05f;
                particlePos = this.position().add(this.getLookAngle().scale(dist))
                        .add((this.random.nextDouble() - 0.5) * spread, (this.random.nextDouble() - 0.5) * spread, (this.random.nextDouble() - 0.5) * spread);
            } else {
                // 円弧状（斬撃）: 回転を考慮した円弧上に配置
                float angleDegrees = (this.random.nextFloat() - 0.5f) * 180.0f; // -90 to 90
                float angleRad = (float) Math.toRadians(angleDegrees);
                
                // ローカルのXY平面上での位置 (Rendererの3DCrescentに合わせる)
                float lx = (float) Math.sin(angleRad) * radius;
                float ly = (float) Math.cos(angleRad) * radius;
                float lz = (this.random.nextFloat() - 0.5f) * 0.1f; // 厚み
                
                // 回転行列の計算 (RendererのmulPose順序を再現)
                float pitch = (float) Math.toRadians(this.getXRot() + 90);
                float yaw = (float) Math.toRadians(-this.getYRot());
                float roll = (float) Math.toRadians(this.getRotation());
                
                Vector3f local = new Vector3f(lx, ly, lz);
                // Roll (Z)
                float cosR = (float)Math.cos(roll), sinR = (float)Math.sin(roll);
                float x1 = local.x * cosR - local.y * sinR;
                float y1 = local.x * sinR + local.y * cosR;
                local.set(x1, y1, local.z);
                // Pitch (X)
                float cosP = (float)Math.cos(pitch), sinP = (float)Math.sin(pitch);
                float y2 = local.y * cosP - local.z * sinP;
                float z2 = local.y * sinP + local.z * cosP;
                local.set(local.x, y2, z2);
                // Yaw (Y)
                float cosY = (float)Math.cos(yaw), sinY = (float)Math.sin(yaw);
                float x3 = local.x * cosY + local.z * sinY;
                float z3 = -local.x * sinY + local.z * cosY;
                
                particlePos = this.position().add(x3, local.y, z3);
            }

            // 2. 属性に応じたパーティクルの生成
            if (tags.contains(SkillTag.FIRE)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, 
                    particlePos.x, particlePos.y, particlePos.z, 0, 0.02, 0);
            }
            if (tags.contains(SkillTag.WATER)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH, 
                    particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
            if (tags.contains(SkillTag.WIND)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, 
                    particlePos.x, particlePos.y, particlePos.z, 
                    (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
            }
            if (tags.contains(SkillTag.HOLY)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT, 
                    particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
            if (tags.contains(SkillTag.DARK)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SQUID_INK, 
                    particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            }
            if (tags.contains(SkillTag.SONIC)) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, 
                    particlePos.x, particlePos.y, particlePos.z, 
                    (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
            }
//            if (tags.contains(SkillTag.BLOOD)) {
//                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.DAMAGE_INDICATOR, 
//                    particlePos.x, particlePos.y, particlePos.z, 0, 0.1, 0);
//            }
        }
    }

    private void Movement() {
        Vec3 vec3 = getDeltaMovement();
        double d2 = this.getX() + vec3.x;
        double d0 = this.getY() + vec3.y;
        double d1 = this.getZ() + vec3.z;
        this.setXRot(getSyncXRot()); // クライアントでも同期された回転を反映
        this.setYRot(getSyncYRot());

        // 召喚直後に初期角度の0.0等から補間（lerp）が走ってしまい向きがズレるラグを防ぐ
        if (this.level().isClientSide && this.tickCount <= 2) {
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
        }

        Vec3 movement = new Vec3(d2, d0, d1);
        this.setDeltaMovement(vec3.scale(0.99));
        this.move(MoverType.SELF, vec3);
        if (!this.position().equals(movement) && getDeltaMovement() != Vec3.ZERO) {
            this.discard();
        }
    }

}
