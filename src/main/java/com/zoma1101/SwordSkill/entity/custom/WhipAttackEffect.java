package com.zoma1101.SwordSkill.entity.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class WhipAttackEffect extends Entity {
    public WhipAttackEffect(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    private float Damage = 0;
    private double knockbackStrength = 0;
    private LivingEntity owner;
    private final LinkedList<Vec3> pastLocations = new LinkedList<>();
    private static final int MAX_PAST_LOCATIONS = 40; // 保存する過去の座標の数

    private static final EntityDataAccessor<Float> EFFECT_SIZE = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> SKILL_PARTICLE = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> PAST_LOCATION_X = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PAST_LOCATION_Y = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> PAST_LOCATION_Z = SynchedEntityData.defineId(WhipAttackEffect.class, EntityDataSerializers.FLOAT);

    @Override
    public void tick() {
        super.tick();
        Movement();
        pastLocations.add(this.position());
        if (pastLocations.size() > MAX_PAST_LOCATIONS){
            pastLocations.removeFirst();
        }

        if (this.tickCount >= 10){
            this.setXRot(getXRot());
            this.setYRot(getYRot()+10);
            setDeltaMovement(this.getLookAngle().scale(1));
        }

        if (this.tickCount >= this.entityData.get(DURATION)) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(EFFECT_SIZE, 0.0f);
        this.entityData.define(DURATION, 20);
        this.entityData.define(SKILL_PARTICLE, "textures/entity/whip_particle");
        this.entityData.define(PAST_LOCATION_X, 0.0f);
        this.entityData.define(PAST_LOCATION_Y, 0.0f);
        this.entityData.define(PAST_LOCATION_Z, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag p_20052_) {

    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag p_20139_) {

    }
    //get
    public float getSize(){
        return this.entityData.get(EFFECT_SIZE);
    }

    public Vec3 getmovePos(int index){
        if (pastLocations.size() > index){

            Vec3 Pos = this.position().subtract(pastLocations.get(
                    pastLocations.size()-(index+1)
                    )).reverse();

            this.entityData.set(PAST_LOCATION_X, (float) Pos.x);
            this.entityData.set(PAST_LOCATION_Y, (float) Pos.y);
            this.entityData.set(PAST_LOCATION_Z, (float) Pos.z);
            return getEffectRadius();
        } else {
            return Vec3.ZERO;
        }
    }

    private Vec3 getEffectRadius(){
        return new Vec3(
                this.entityData.get(PAST_LOCATION_X),
                this.entityData.get(PAST_LOCATION_Y),
                this.entityData.get(PAST_LOCATION_Z)
        );
    }


    //set
    public void setSkillParticle(String skillParticle){
        this.entityData.set(SKILL_PARTICLE, skillParticle);
    }
    public void setDamage(float Damage){
        this.Damage = Damage;
    }
    public void setKnockback(double knockback){
        this.knockbackStrength = knockback;
    }
    public void setDuration(int duration){
        this.entityData.set(DURATION, duration);
    }
    public void setOwner(LivingEntity owner){
        this.owner = owner;
    }
    public void setSize(float size){
        this.entityData.set(EFFECT_SIZE, size);
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
        }
    }

    private void ApplyDamage(LivingEntity entity){
        float DamagePer = 1;
        entity.hurt(this.damageSources().mobAttack(owner), (Damage * DamagePer));
        entity.invulnerableTime = 0;
    }

    private void ApplyKnockBack(LivingEntity entity){
        float KnockBackPer = 1;
        if (owner != null) {
            Vec3 knockbackDir = (entity.position().subtract(this.position())).normalize().scale(knockbackStrength * KnockBackPer);
            entity.setDeltaMovement(knockbackDir.x, 0.3 * knockbackStrength * KnockBackPer, knockbackDir.z);
        }
    }

}