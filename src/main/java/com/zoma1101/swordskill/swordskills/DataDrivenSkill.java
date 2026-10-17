package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.payload.SkillShockwavePayload;
import com.zoma1101.swordskill.server.handler.HitstopManager;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DataDrivenSkill implements ISkill {
    public static final int MAX_CHARGE_TICKS = 40;
    private static final float MIN_CHARGE_MULTIPLIER = 0.75F;
    private static final float MAX_CHARGE_MULTIPLIER = 2.0F;
    private static final float MIN_CHARGE_MULTIPLIER_PER_LEVEL = 0.0F;
    private static final float MAX_CHARGE_MULTIPLIER_PER_LEVEL = 0.0F;
    private static final Map<UUID, Set<Integer>> FIRED_MOTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> FIRED_HITBOXES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> FIRED_PROJECTILES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> FIRED_SOUNDS = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> CHARGE_MULTIPLIERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> RUSH_CONTACT_TICKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> RUSH_PREVIOUS_POS = new ConcurrentHashMap<>();
    private static final Set<UUID> SHOCKWAVE_FIRED = ConcurrentHashMap.newKeySet();

    public static float chargeMultiplier(int chargeTicks) {
        return chargeMultiplier(chargeTicks, 0);
    }

    public static float chargeMultiplier(int chargeTicks, int skillLevel) {
        float chargeProgress = Math.max(0, Math.min(chargeTicks, MAX_CHARGE_TICKS))
                / (float) MAX_CHARGE_TICKS;
        int level = Math.max(0, skillLevel);
        float minimum = MIN_CHARGE_MULTIPLIER + MIN_CHARGE_MULTIPLIER_PER_LEVEL * level;
        float maximum = MAX_CHARGE_MULTIPLIER + MAX_CHARGE_MULTIPLIER_PER_LEVEL * level;
        return minimum + (maximum - minimum) * chargeProgress;
    }

    public static void setChargeMultiplier(UUID playerId, float multiplier) {
        if (Float.isFinite(multiplier) && multiplier >= 0.0F) {
            CHARGE_MULTIPLIERS.put(playerId, multiplier);
        }
    }

    private static float getChargeMultiplier(UUID playerId) {
        return CHARGE_MULTIPLIERS.getOrDefault(playerId, 1.0F);
    }

    @Override
    public void execute(Level level, ServerPlayer player, int tick, int skillId) {
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill == null || skill.getDataDefinition() == null) {
            return;
        }

        DataSkillDefinition definition = skill.getDataDefinition();
        if (definition.legacyFallback()) {
            SkillData baseSkill = SwordSkillRegistry.getBaseSkill(skillId);
            if (baseSkill == null) {
                throw new IllegalStateException("No built-in skill is available for legacy fallback: " + skillId);
            }
            try {
                baseSkill.getSkillClass().getDeclaredConstructor().newInstance()
                        .execute(level, player, tick, skillId);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to execute legacy skill " + baseSkill.getName(), exception);
            }
            return;
        }

        int shockwaveTicks = definition.shockwaveTicks();
        int hitstopTicks = definition.hitstopTicks() < 0
                ? ServerConfig.hitstopTicks.get() : definition.hitstopTicks();

        Set<Integer> firedMotions = FIRED_MOTIONS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (int index = 0; index < definition.motions().size(); index++) {
            DataSkillDefinition.MotionEvent motion = definition.motions().get(index);
            if ((motion.tick() == tick || (tick == 1 && motion.tick() == 0)) && firedMotions.add(index)) {
                applyMotion(player, new DataSkillDefinition.Motion(motion.forward(), motion.strafe(), motion.vertical()));
            }
        }
        if (definition.motion().forward() != 0 || definition.motion().strafe() != 0
                || definition.motion().vertical() != 0) {
            if (tick <= 1 && firedMotions.add(-1)) {
                applyMotion(player, definition.motion());
            }
        }

        DataSkillDefinition.RushCombo rushCombo = definition.rushCombo();
        int contactTick = RUSH_CONTACT_TICKS.getOrDefault(player.getUUID(), -1);
        if (rushCombo != null && definition.type() == SkillData.SkillType.RUSH) {
            Vec3 currentCenter = player.position().add(0, player.getBbHeight() * 0.5, 0);
            Vec3 previousCenter = RUSH_PREVIOUS_POS.getOrDefault(player.getUUID(), currentCenter);
            if (tick == 1) {
                RUSH_CONTACT_TICKS.put(player.getUUID(), -1);
                contactTick = -1;
                Vec3 rushLook = limitedLook(player.getLookAngle(), rushCombo.pitchLimitDegrees());
                player.setDeltaMovement(rushLook.scale(rushCombo.startMotion().forward())
                        .add(0, rushCombo.startMotion().vertical(), 0));
                player.hurtMarked = true;
                player.invulnerableTime = rushCombo.invulnerabilityTicks();
                previousCenter = currentCenter;
            }
            if (contactTick < 0 && tick <= rushCombo.latestContactTick()) {
                // 高速移動でも取りこぼさないよう、前tickからの線分で接触を判定する
                AABB searchArea = new AABB(previousCenter, currentCenter).inflate(rushCombo.searchRadius());
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, searchArea,
                        target -> SkillUtils.SkillTargetEntity(target, player))) {
                    AABB contactBox = entity.getBoundingBox().inflate(rushCombo.contactRadius());
                    boolean reached = contactBox.contains(currentCenter)
                            || contactBox.contains(previousCenter)
                            || contactBox.clip(previousCenter, currentCenter).isPresent();
                    if (reached) {
                        contactTick = tick;
                        RUSH_CONTACT_TICKS.put(player.getUUID(), contactTick);
                        player.setDeltaMovement(player.getLookAngle().scale(rushCombo.contactMotionForward()));
                        player.hurtMarked = true;
                        break;
                    }
                }
            }
            if (contactTick < 0 && tick >= rushCombo.noContactTimeoutTick()) {
                RUSH_PREVIOUS_POS.remove(player.getUUID());
                SkillExecutionManager.cancelRush(player);
                return;
            }
            if (contactTick < 0 && tick > rushCombo.latestContactTick()) {
                RUSH_PREVIOUS_POS.remove(player.getUUID());
                SkillExecutionManager.cancelRush(player);
                return;
            }
            if (contactTick >= 0 && tick == contactTick + rushCombo.contactAnimationOffset()) {
                SkillExecutionManager.changeRushPhase(player, "finish");
            }
            RUSH_PREVIOUS_POS.put(player.getUUID(), currentCenter);
        }

        Set<Integer> firedSounds = FIRED_SOUNDS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (int soundIndex = 0; soundIndex < definition.sounds().size(); soundIndex++) {
            DataSkillDefinition.SoundEvent sound = definition.sounds().get(soundIndex);
            boolean initialTick = tick == 1 && sound.tick() == 0;
            if ((sound.tick() == tick || initialTick) && firedSounds.add(soundIndex)) {
                playSound(level, player, sound);
            }
        }

        Set<Integer> firedHitboxes = FIRED_HITBOXES.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (int index = 0; index < definition.hitboxes().size(); index++) {
            DataSkillDefinition.Hitbox hitbox = definition.hitboxes().get(index);
            boolean contactTriggered = hitbox.trigger().equals("CONTACT") && contactTick >= 0;
            int triggerTick = contactTriggered ? contactTick + hitbox.tick() : hitbox.tick();
            boolean initialTick = hitbox.trigger().equals("TICK") && tick == 1 && hitbox.tick() == 0;
            boolean firstTrigger = ((hitbox.trigger().equals("TICK") && hitbox.tick() == tick)
                    || (contactTriggered && triggerTick == tick) || initialTick) && firedHitboxes.add(index);
            boolean repeatRushHitbox = hitbox.trigger().equals("TICK") && hitbox.repeatWhileRushing()
                    && definition.type() == SkillData.SkillType.RUSH && tick >= hitbox.tick();
            if (firstTrigger || repeatRushHitbox) {
                if (firstTrigger) {
                    playAttackSound(level, player, hitbox.sound());
                    triggerShockwave(player, shockwaveTicks, definition.color());
                }
                if (hitbox.hitboxEnabled()) {
                    if (applyHitbox(level, player, hitbox,
                            rushCombo == null || rushCombo.useChargeMultiplier(), hitstopTicks)
                            && hitbox.repeatWhileRushing()
                            && definition.type() == SkillData.SkillType.RUSH) {
                        SkillExecutionManager.finishRush(player);
                        return;
                    }
                }
            }
        }

        Set<Integer> firedProjectiles = FIRED_PROJECTILES.computeIfAbsent(player.getUUID(),
                ignored -> new HashSet<>());
        for (int projectileIndex = 0; projectileIndex < definition.projectiles().size(); projectileIndex++) {
            DataSkillDefinition.ProjectileEvent projectile = definition.projectiles().get(projectileIndex);
            boolean contactTriggered = projectile.trigger().equals("CONTACT") && contactTick >= 0;
            int triggerTick = contactTriggered ? contactTick + projectile.tick() : projectile.tick();
            boolean projectileInitialTick = projectile.trigger().equals("TICK") && tick == 1
                    && projectile.tick() == 0;
            if (((projectile.trigger().equals("TICK") && projectile.tick() == tick)
                    || (contactTriggered && triggerTick == tick) || projectileInitialTick)
                    && firedProjectiles.add(projectileIndex)) {
                spawnProjectile(level, player, projectile);
                triggerShockwave(player, shockwaveTicks, definition.color());
            }
        }

        if (rushCombo != null && contactTick >= 0
                && tick >= contactTick + rushCombo.finishContactOffset()) {
            SkillExecutionManager.finishRush(player);
            return;
        }

    }

    public static void clearPlayerState(UUID playerId) {
        FIRED_MOTIONS.remove(playerId);
        FIRED_HITBOXES.remove(playerId);
        FIRED_PROJECTILES.remove(playerId);
        FIRED_SOUNDS.remove(playerId);
        CHARGE_MULTIPLIERS.remove(playerId);
        RUSH_CONTACT_TICKS.remove(playerId);
        RUSH_PREVIOUS_POS.remove(playerId);
        SHOCKWAVE_FIRED.remove(playerId);
    }

    private static Vec3 limitedLook(Vec3 look, double pitchLimitDegrees) {
        if (pitchLimitDegrees >= 90) return look.normalize();
        double limitedY = Math.max(-Math.sin(Math.toRadians(pitchLimitDegrees)),
                Math.min(Math.sin(Math.toRadians(pitchLimitDegrees)), look.y));
        double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
        if (horizontalLength < 1.0e-8) return new Vec3(0, limitedY < 0 ? -1 : 1, 0);
        double horizontalScale = Math.cos(Math.asin(limitedY));
        return new Vec3(look.x / horizontalLength * horizontalScale, limitedY,
                look.z / horizontalLength * horizontalScale).normalize();
    }

    private static void spawnProjectile(Level level, ServerPlayer player,
            DataSkillDefinition.ProjectileEvent projectile) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double[] offset = projectile.offset();
        double[] rotation = projectile.rotation();
        double[] size = projectile.size();
        double playerPitch = player.getXRot();
        double pitch = projectile.horizontal() ? -playerPitch
                : rotation[0] + playerPitch * projectile.playerPitchXMultiplier();
        double roll = rotation[2] + playerPitch * projectile.playerPitchZMultiplier();
        Vec3 spawnPosition = player.position()
                .add(right.scale(offset[0]))
                .add(up.scale(offset[1]))
                .add(forward.scale(offset[2]));
        double baseDamage = projectile.damageType().equals("RUSH")
                ? SkillUtils.RushDamage(player) : SkillUtils.BaseDamage(player);
        float damage = (float) (baseDamage * projectile.damageMultiplier()
                * getChargeMultiplier(player.getUUID()));
        if (!projectile.sound().isEmpty()) {
            playAttackSound(level, player, projectile.sound());
        }
        Vec3 movement = projectile.movement() == null ? null
                : new Vec3(projectile.movement()[0], projectile.movement()[1], projectile.movement()[2]);
        double knockback = projectile.knockbackMultiplier() != -1
                ? SkillUtils.BaseKnowBack(player) * projectile.knockbackMultiplier()
                : projectile.knockback();
        if (movement == null) {
            SkillUtils.spawnFlyingSlash(level, spawnPosition,
                    new Vec3(pitch, rotation[1], roll),
                    new org.joml.Vector3f((float) size[0], (float) size[1], (float) size[2]),
                    player, damage, knockback,
                    projectile.durationTicks(), projectile.particle(), projectile.travelDistance(),
                    projectile.renderPitch());
        } else {
            SkillUtils.spawnFlyingSlash(level, spawnPosition,
                    new Vec3(pitch, rotation[1], roll),
                    new org.joml.Vector3f((float) size[0], (float) size[1], (float) size[2]),
                    player, damage, knockback, projectile.durationTicks(),
                    projectile.particle(), projectile.travelDistance(), movement, projectile.renderPitch());
        }
    }

    private static void applyMotion(ServerPlayer player, DataSkillDefinition.Motion motion) {
        if (motion.forward() == 0 && motion.strafe() == 0 && motion.vertical() == 0) {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.hurtMarked = true;
            return;
        }
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 addedMotion = forward.scale(motion.forward())
                .add(right.scale(motion.strafe()))
                .add(0, motion.vertical(), 0);
        if (addedMotion.lengthSqr() > 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(addedMotion));
            player.hurtMarked = true;
        }
    }

    private static boolean applyHitbox(Level level, ServerPlayer player, DataSkillDefinition.Hitbox hitbox,
            boolean useChargeMultiplier, int hitstopTicks) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double[] offset = hitbox.offset();
        double[] size = hitbox.size();
        Vec3 center = player.position()
                .add(right.scale(offset[0]))
                .add(up.scale(offset[1]))
                .add(forward.scale(offset[2]));
        double range = Math.max(size[0], Math.max(size[1], size[2])) + 2.0;
        AABB searchArea = new AABB(center, center).inflate(range);

        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> SkillUtils.SkillTargetEntity(entity, player))) {
            Vec3 relative = target.getBoundingBox().getCenter().subtract(center);
            double targetRadius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5;
            if (Math.abs(relative.dot(right)) > size[0] * 0.5 + targetRadius
                    || Math.abs(relative.dot(up)) > size[1] * 0.5 + targetRadius
                    || Math.abs(relative.dot(forward)) > size[2] * 0.5 + targetRadius) {
                continue;
            }
            hit |= applyHit(player, target, hitbox, useChargeMultiplier, hitstopTicks);
        }
        return hit;
    }

    private static boolean applyHit(ServerPlayer player, LivingEntity target, DataSkillDefinition.Hitbox hitbox,
            boolean useChargeMultiplier, int hitstopTicks) {
        float damageMultiplier = (float) hitbox.damageMultiplier();
        for (DataSkillDefinition.Effect effect : hitbox.effects()) {
            if (effect.type().equals("undead_bonus") && target.getType().is(EntityTypeTags.UNDEAD)) {
                damageMultiplier *= (float) effect.multiplier();
            }
            if (effect.type().equals("low_hp_bonus")
                    && target.getMaxHealth() / 2 >= target.getHealth()) {
                damageMultiplier *= (float) effect.multiplier();
            }
        }

        double baseDamage = hitbox.damageType().equals("RUSH")
                ? SkillUtils.RushDamage(player) : SkillUtils.BaseDamage(player);
        float damage = (float) (baseDamage * damageMultiplier
                * (useChargeMultiplier ? getChargeMultiplier(player.getUUID()) : 1.0F));
        target.invulnerableTime = 0;
        if (!target.hurt(player.damageSources().playerAttack(player), damage)) {
            return false;
        }

        double knockbackStrength = hitbox.knockbackMultiplier() != -1
                ? SkillUtils.BaseKnowBack(player) * hitbox.knockbackMultiplier()
                : hitbox.knockback();
        Vec3 knockback = null;
        if (knockbackStrength != 0) {
            Vec3 direction = target.position().subtract(player.position()).normalize();
            knockback = direction.scale(knockbackStrength)
                    .add(0.0D, 0.25D * knockbackStrength, 0.0D);
            if (hitstopTicks <= 0) {
                target.setDeltaMovement(target.getDeltaMovement().add(knockback));
                target.hurtMarked = true;
            }
        }

        for (DataSkillDefinition.Effect effect : hitbox.effects()) {
            applyEffect(player, target, effect, damage);
        }
        HitstopManager.apply(player, target, hitstopTicks, knockback);
        return true;
    }

    private static void triggerShockwave(ServerPlayer player, int shockwaveTicks, String color) {
        if (shockwaveTicks <= 0 || !SHOCKWAVE_FIRED.add(player.getUUID())) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new SkillShockwavePayload(player.getId(), shockwaveTicks, color == null ? "" : color));
    }

    private static void playAttackSound(Level level, ServerPlayer player, String soundId) {
        if (soundId.isEmpty()) {
            return;
        }
        ResourceLocation id = ResourceLocation.parse(soundId);
        var sound = BuiltInRegistries.SOUND_EVENT.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Unknown attack sound: " + soundId));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void playSound(Level level, ServerPlayer player, DataSkillDefinition.SoundEvent soundEvent) {
        ResourceLocation id = ResourceLocation.parse(soundEvent.sound());
        var sound = BuiltInRegistries.SOUND_EVENT.getOptional(id)
                .orElseThrow(() -> new IllegalStateException("Unknown skill sound: " + soundEvent.sound()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                sound, SoundSource.PLAYERS, soundEvent.volume(), soundEvent.pitch());
    }

    private static void applyEffect(ServerPlayer player, LivingEntity target, DataSkillDefinition.Effect effect,
            float damage) {
        switch (effect.type()) {
            case "undead_bonus", "low_hp_bonus" -> {
            }
            case "stun" -> {
                int duration = Math.max(1, effect.durationTicks());
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        duration, Math.max(0, effect.amplifier())));
            }
            case "magic_bonus" -> {
                // AttackEffectEntity相当: 追加魔法ダメージ (ダメージ×multiplier)
                float magicDamage = (float) (damage * effect.multiplier());
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().magic(), magicDamage);
                target.invulnerableTime = 0;
            }
            case "lifesteal" -> player.heal((float) (damage * effect.multiplier()));
            default -> {
                if (!SkillHitEffectRegistry.isRegistered(effect.type())) {
                    throw new IllegalStateException("Unregistered skill hit effect: " + effect.type());
                }
                SkillHitEffectRegistry.apply(effect, player, target);
            }
        }
    }
}
