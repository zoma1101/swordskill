package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.payload.SkillShockwavePayload;
import com.zoma1101.swordskill.server.handler.HitstopManager;
import com.zoma1101.swordskill.server.handler.SkillClashManager;
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
    private static final Map<UUID, Integer> RUSH_CONTACT_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> RUSH_PREVIOUS_POS = new ConcurrentHashMap<>();
    private static final Set<UUID> SHOCKWAVE_FIRED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Set<Integer>> FIRED_ANIMATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Integer>> FIRED_PLAYER_EFFECTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> SKILL_START_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> PEAK_Y = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> PREV_ON_GROUND = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> RANDOM_CHOICES = new ConcurrentHashMap<>();
    private static final java.util.Random RANDOM = new java.util.Random();

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

    /** 他クラス（鍔迫り合いの自動判定など）から現在のチャージ倍率を参照する。 */
    public static float chargeMultiplierOf(UUID playerId) {
        return getChargeMultiplier(playerId);
    }

    @Override
    public void execute(Level level, ServerPlayer player, int tick, int skillId) {
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill == null || skill.getDataDefinition() == null) {
            return;
        }

        DataSkillDefinition definition = skill.getDataDefinition();

        UUID playerId = player.getUUID();
        int shockwaveTicks = definition.shockwaveTicks();
        int hitstopTicks = definition.hitstopTicks() < 0
                ? ServerConfig.hitstopTicks.get() : definition.hitstopTicks();

        // --- スキル開始時の初期化と接地判定 ---
        // 初回 tick は実行環境によって 0 か 1 になるため、状態の有無で判定する
        if (!PREV_ON_GROUND.containsKey(playerId)) {
            SKILL_START_POS.remove(playerId);
            PEAK_Y.put(playerId, player.getY());
            PREV_ON_GROUND.put(playerId, true);
            rollRandomChoices(playerId, definition);
        }
        PEAK_Y.merge(playerId, player.getY(), Math::max);
        for (DataSkillDefinition.ProjectileEvent pathProjectile : definition.projectiles()) {
            DataSkillDefinition.PathSpec path = pathProjectile.path();
            if (path != null && path.startTick() == tick) {
                SKILL_START_POS.put(playerId, player.position());
                break;
            }
        }
        boolean previousOnGround = PREV_ON_GROUND.getOrDefault(playerId, true);
        boolean groundTriggered = player.onGround() && !previousOnGround;
        PREV_ON_GROUND.put(playerId, player.onGround());
        int invulnerability = definition.rushCombo() == null
                ? definition.invulnerabilityTicks()
                : Math.max(definition.invulnerabilityTicks(), definition.rushCombo().invulnerabilityTicks());
        if (tick == 1 && invulnerability > 0) {
            player.invulnerableTime = invulnerability;
        }

        Set<Integer> firedMotions = FIRED_MOTIONS.computeIfAbsent(playerId, ignored -> new HashSet<>());
        for (int index = 0; index < definition.motions().size(); index++) {
            DataSkillDefinition.MotionEvent motion = definition.motions().get(index);
            if (!motion.trigger().equals("TICK")) {
                continue;
            }
            boolean first = (motion.tick() == tick || (tick == 1 && motion.tick() == 0)) && firedMotions.add(index);
            if (first || (motion.repeat() && tick >= motion.tick() && tick > 0)) {
                applyMotion(player, new DataSkillDefinition.Motion(motion.forward(), motion.strafe(), motion.vertical()),
                        motion.set());
            }
        }
        if (definition.motion().forward() != 0 || definition.motion().strafe() != 0
                || definition.motion().vertical() != 0) {
            if (tick <= 1 && firedMotions.add(-1)) {
                applyMotion(player, definition.motion(), false);
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
                        RUSH_CONTACT_ENTITIES.put(player.getUUID(), entity.getId());
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

        // 接地・接触で動くモーション（突進の跳ね返りなど）
        for (int index = 0; index < definition.motions().size(); index++) {
            DataSkillDefinition.MotionEvent motion = definition.motions().get(index);
            if (motion.trigger().equals("TICK")) {
                continue;
            }
            boolean triggered = motion.trigger().equals("GROUND")
                    ? groundTriggered && tick >= motion.tick()
                    : contactTick >= 0 && tick == contactTick + motion.tick();
            if (triggered && firedMotions.add(index)) {
                applyMotion(player, new DataSkillDefinition.Motion(motion.forward(), motion.strafe(), motion.vertical()),
                        motion.set());
            }
        }

        Set<Integer> firedSounds = FIRED_SOUNDS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (int soundIndex = 0; soundIndex < definition.sounds().size(); soundIndex++) {
            DataSkillDefinition.SoundEvent sound = definition.sounds().get(soundIndex);
            boolean triggered = switch (sound.trigger()) {
                case "GROUND" -> groundTriggered && tick >= sound.tick();
                case "CONTACT" -> contactTick >= 0 && tick == contactTick + sound.tick();
                default -> sound.tick() == tick || (tick == 1 && sound.tick() == 0);
            };
            if (triggered && firedSounds.add(soundIndex)) {
                playSound(level, player, sound);
            }
        }

        Set<Integer> firedHitboxes = FIRED_HITBOXES.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        for (int index = 0; index < definition.hitboxes().size(); index++) {
            DataSkillDefinition.Hitbox hitbox = definition.hitboxes().get(index);
            boolean contactTriggered = hitbox.trigger().equals("CONTACT") && contactTick >= 0;
            int triggerTick = contactTriggered ? contactTick + hitbox.tick() : hitbox.tick();
            boolean initialTick = hitbox.trigger().equals("TICK") && tick == 1 && hitbox.tick() == 0;
            boolean groundTriggeredHitbox = hitbox.trigger().equals("GROUND")
                    && groundTriggered && tick >= hitbox.tick();
            boolean firstTrigger = ((hitbox.trigger().equals("TICK") && hitbox.tick() == tick)
                    || (contactTriggered && triggerTick == tick) || initialTick
                    || groundTriggeredHitbox) && firedHitboxes.add(index);
            boolean repeatRushHitbox = hitbox.trigger().equals("TICK") && hitbox.repeatWhileRushing()
                    && definition.type() == SkillData.SkillType.RUSH && tick >= hitbox.tick();
            if (firstTrigger || repeatRushHitbox) {
                if (firstTrigger) {
                    playAttackSound(level, player, hitbox.sound());
                    triggerShockwave(player, shockwaveTicks, definition.color());
                }
                if (hitbox.hitboxEnabled()) {
                    boolean hit = applyHitbox(level, player, hitbox,
                            rushCombo == null || rushCombo.useChargeMultiplier(), hitstopTicks,
                            definition.color());
                    // 鍔迫り合いが成立したら、このスキルの残りの判定はすべて打ち切る
                    if (SkillClashManager.isClashing(player.getUUID())) {
                        return;
                    }
                    if (hit && hitbox.repeatWhileRushing()
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
            if (!matchesRandom(playerId, projectile.randomGroup(), projectile.randomIndex())) {
                continue;
            }
            boolean contactTriggered = projectile.trigger().equals("CONTACT") && contactTick >= 0;
            int triggerTick = contactTriggered ? contactTick + projectile.tick() : projectile.tick();
            boolean projectileInitialTick = projectile.trigger().equals("TICK") && tick == 1
                    && projectile.tick() == 0;
            boolean groundTriggeredProjectile = projectile.trigger().equals("GROUND")
                    && groundTriggered && tick >= projectile.tick();
            if (((projectile.trigger().equals("TICK") && projectile.tick() == tick)
                    || (contactTriggered && triggerTick == tick) || projectileInitialTick
                    || groundTriggeredProjectile)
                    && firedProjectiles.add(projectileIndex)) {
                spawnProjectile(level, player, projectile, parseColor(definition.color()));
                triggerShockwave(player, shockwaveTicks, definition.color());
            }
        }

        Set<Integer> firedAnimations = FIRED_ANIMATIONS.computeIfAbsent(playerId, ignored -> new HashSet<>());
        for (int index = 0; index < definition.animations().size(); index++) {
            DataSkillDefinition.AnimationEvent animation = definition.animations().get(index);
            if (!matchesRandom(playerId, animation.randomGroup(), animation.randomIndex())) {
                continue;
            }
            boolean triggered = switch (animation.trigger()) {
                case "GROUND" -> groundTriggered && tick >= animation.tick();
                case "CONTACT" -> contactTick >= 0 && tick == contactTick + animation.tick();
                default -> animation.tick() == tick || (tick == 1 && animation.tick() == 0);
            };
            if (triggered && firedAnimations.add(index)) {
                playAnimation(player, skillId, animation.animation());
            }
        }

        Set<Integer> firedPlayerEffects = FIRED_PLAYER_EFFECTS.computeIfAbsent(playerId, ignored -> new HashSet<>());
        for (int index = 0; index < definition.playerEffects().size(); index++) {
            DataSkillDefinition.PlayerEffectEvent effect = definition.playerEffects().get(index);
            boolean triggered = switch (effect.trigger()) {
                case "GROUND" -> groundTriggered && tick >= effect.tick();
                case "CONTACT" -> contactTick >= 0 && tick == contactTick + effect.tick();
                default -> effect.tick() == tick || (tick == 1 && effect.tick() == 0);
            };
            if (triggered && firedPlayerEffects.add(index)) {
                applyPlayerEffect(player, effect);
            }
        }

        if (rushCombo != null && contactTick >= 0
                && tick >= contactTick + rushCombo.finishContactOffset()) {
            SkillExecutionManager.finishRush(player);
            return;
        }
        // 着地でスキル終了（棍棒の着地衝撃など）。RUSH なら finish アニメーションも再生する。
        if (definition.endSkillOnGround() && groundTriggered) {
            if (definition.type() == SkillData.SkillType.RUSH) {
                SkillExecutionManager.finishRush(player);
            } else {
                SkillExecutionManager.cancelRush(player);
            }
        }

    }

    /** アニメーション名が空なら「アニメーション解除」として skillId 0 を送る。 */
    private static void playAnimation(ServerPlayer player, int skillId, String animation) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new com.zoma1101.swordskill.payload.PlayAnimationPayload(player.getId(),
                        animation.isEmpty() ? 0 : skillId, animation));
    }

    private static void applyPlayerEffect(ServerPlayer player, DataSkillDefinition.PlayerEffectEvent effect) {
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder = BuiltInRegistries.MOB_EFFECT
                .getHolder(ResourceLocation.parse(effect.effect()))
                .orElseThrow(() -> new IllegalStateException("Unknown player effect: " + effect.effect()));
        player.addEffect(new MobEffectInstance(holder, effect.durationTicks(), effect.amplifier(),
                false, effect.showParticles()));
    }

    /** スキル開始時に random_group ごとの出目を 1 つ決めておく。 */
    private static void rollRandomChoices(UUID playerId, DataSkillDefinition definition) {
        Map<String, Integer> sizes = new java.util.HashMap<>();
        for (DataSkillDefinition.AnimationEvent animation : definition.animations()) {
            if (!animation.randomGroup().isEmpty()) {
                sizes.merge(animation.randomGroup(), animation.randomIndex() + 1, Math::max);
            }
        }
        for (DataSkillDefinition.ProjectileEvent projectile : definition.projectiles()) {
            if (!projectile.randomGroup().isEmpty()) {
                sizes.merge(projectile.randomGroup(), projectile.randomIndex() + 1, Math::max);
            }
        }
        if (sizes.isEmpty()) {
            RANDOM_CHOICES.remove(playerId);
            return;
        }
        Map<String, Integer> choices = new java.util.HashMap<>();
        for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
            choices.put(entry.getKey(), RANDOM.nextInt(Math.max(1, entry.getValue())));
        }
        RANDOM_CHOICES.put(playerId, choices);
    }

    private static boolean matchesRandom(UUID playerId, String group, int index) {
        if (group.isEmpty()) {
            return true;
        }
        Map<String, Integer> choices = RANDOM_CHOICES.get(playerId);
        return choices != null && choices.getOrDefault(group, -1) == index;
    }

    public static void clearPlayerState(UUID playerId) {
        FIRED_MOTIONS.remove(playerId);
        FIRED_HITBOXES.remove(playerId);
        FIRED_PROJECTILES.remove(playerId);
        FIRED_SOUNDS.remove(playerId);
        FIRED_ANIMATIONS.remove(playerId);
        FIRED_PLAYER_EFFECTS.remove(playerId);
        CHARGE_MULTIPLIERS.remove(playerId);
        RUSH_CONTACT_TICKS.remove(playerId);
        RUSH_CONTACT_ENTITIES.remove(playerId);
        RUSH_PREVIOUS_POS.remove(playerId);
        SHOCKWAVE_FIRED.remove(playerId);
        SKILL_START_POS.remove(playerId);
        PEAK_Y.remove(playerId);
        PREV_ON_GROUND.remove(playerId);
        RANDOM_CHOICES.remove(playerId);
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
            DataSkillDefinition.ProjectileEvent projectile, int skillColor) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(forward).normalize();
        double[] offset = projectile.offset();
        double[] rotation = projectile.rotation().clone();
        double[] size = projectile.size().clone();
        double playerPitch = player.getXRot();
        double yaw = rotation[1];
        double pitch = projectile.horizontal() ? -playerPitch
                : rotation[0] + playerPitch * projectile.playerPitchXMultiplier();
        double roll = rotation[2] + playerPitch * projectile.playerPitchZMultiplier();
        double damageMultiplier = projectile.damageMultiplier();
        Vec3 spawnPosition = player.position()
                .add(right.scale(offset[0]))
                .add(up.scale(offset[1]))
                .add(forward.scale(offset[2]));

        // 接触した相手に自動で狙いを付ける（突進の接触斬撃など）。
        // 旧実装と同じく「相手の足元から相手→本体方向へ offset[2] 離れた位置」から相手の中心を狙う。
        boolean aimedAtContact = false;
        if (projectile.aimAtContact() && projectile.trigger().equals("CONTACT")) {
            Integer targetId = RUSH_CONTACT_ENTITIES.get(player.getUUID());
            if (targetId != null && level.getEntity(targetId) instanceof LivingEntity target) {
                Vec3 targetCenter = target.getBoundingBox().getCenter();
                Vec3 away = new Vec3(player.getX() - targetCenter.x, 0, player.getZ() - targetCenter.z);
                away = away.lengthSqr() < 1.0E-6 ? forward.scale(-1) : away.normalize();
                spawnPosition = target.position().add(away.scale(offset[2])).add(0, offset[1], 0);
                Vec3 direction = targetCenter.subtract(spawnPosition);
                if (direction.lengthSqr() > 1.0E-6) {
                    direction = direction.normalize();
                    double yawDegrees = Math.toDegrees(Math.atan2(-direction.x, direction.z));
                    double pitchDegrees = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, direction.y))));
                    // AttackEffectEntity は owner の向きに rotation を加算するので、絶対角になるよう差し引く
                    pitch = -pitchDegrees - player.getXRot();
                    yaw = yawDegrees - player.getYRot();
                    roll = rotation[2];
                }
                aimedAtContact = true;
            }
        }

        // 突進しながら伸びる斬撃: 始点から現在位置までの線上に出し、進行方向へ向ける
        DataSkillDefinition.PathSpec path = projectile.path();
        if (path != null && !aimedAtContact) {
            Vec3 start = SKILL_START_POS.get(player.getUUID());
            if (start != null) {
                Vec3 travel = player.position().subtract(start);
                spawnPosition = start.add(travel.scale(path.position()))
                        .add(right.scale(offset[0]))
                        .add(up.scale(offset[1]))
                        .add(forward.scale(offset[2]));
                if (path.rotation() && travel.lengthSqr() > 1.0E-6) {
                    Vec3 direction = travel.normalize();
                    double yawDegrees = Math.toDegrees(Math.atan2(direction.z, direction.x));
                    double clampedY = Math.max(-1.0, Math.min(1.0, direction.y));
                    double pitchDegrees = Math.toDegrees(Math.asin(clampedY));
                    // AttackEffectEntity は owner の向きに rotation を加算するので、絶対角になるよう差し引く
                    pitch = -pitchDegrees - player.getXRot() + rotation[0];
                    yaw = yawDegrees - 90 + rotation[1] - player.getYRot();
                    roll = rotation[2];
                }
                if (path.sizeScale() > 0) {
                    double travelAverage = (Math.abs(travel.x) + Math.abs(travel.y) + Math.abs(travel.z)) / 3.0;
                    size[2] = Math.max(1.0, travelAverage * path.sizeScale());
                }
            }
        }
        // 落下距離に比例したダメージ（棍棒の着地衝撃など）
        if (projectile.fallDamageScale() > 0) {
            double peak = PEAK_Y.getOrDefault(player.getUUID(), player.getY());
            damageMultiplier *= Math.max(0, peak - player.getY()) * projectile.fallDamageScale();
        }

        double baseDamage = projectile.damageType().equals("RUSH")
                ? SkillUtils.RushDamage(player) : SkillUtils.BaseDamage(player);
        float damage = (float) (baseDamage * damageMultiplier
                * getChargeMultiplier(player.getUUID()));
        if (!projectile.sound().isEmpty()) {
            playAttackSound(level, player, projectile.sound());
        }
        Vec3 movement = projectile.movement() == null ? null
                : new Vec3(projectile.movement()[0], projectile.movement()[1], projectile.movement()[2]);
        double knockback = projectile.knockbackMultiplier() != -1
                ? SkillUtils.BaseKnowBack(player) * projectile.knockbackMultiplier()
                : projectile.knockback();
        AttackEffectEntity effect;
        if (movement == null) {
            effect = SkillUtils.spawnFlyingSlash(level, spawnPosition,
                    new Vec3(pitch, yaw, roll),
                    new org.joml.Vector3f((float) size[0], (float) size[1], (float) size[2]),
                    player, damage, knockback,
                    projectile.durationTicks(), projectile.particle(), projectile.travelDistance(),
                    projectile.renderPitch(), skillColor);
        } else {
            effect = SkillUtils.spawnFlyingSlash(level, spawnPosition,
                    new Vec3(pitch, yaw, roll),
                    new org.joml.Vector3f((float) size[0], (float) size[1], (float) size[2]),
                    player, damage, knockback, projectile.durationTicks(),
                    projectile.particle(), projectile.travelDistance(), movement, projectile.renderPitch(),
                    skillColor);
        }
        if (effect != null && projectile.singleTarget()) {
            effect.setSingleTarget(true);
        }
    }

    private static void applyMotion(ServerPlayer player, DataSkillDefinition.Motion motion, boolean set) {
        if (motion.forward() == 0 && motion.strafe() == 0 && motion.vertical() == 0) {
            player.setDeltaMovement(set ? Vec3.ZERO
                    : new Vec3(0, Math.min(0, player.getDeltaMovement().y), 0));
            player.hurtMarked = true;
            return;
        }
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 addedMotion = forward.scale(motion.forward())
                .add(right.scale(motion.strafe()))
                .add(0, motion.vertical(), 0);
        if (set) {
            player.setDeltaMovement(addedMotion);
            player.hurtMarked = true;
        } else if (addedMotion.lengthSqr() > 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(addedMotion));
            player.hurtMarked = true;
        }
    }

    private static boolean applyHitbox(Level level, ServerPlayer player, DataSkillDefinition.Hitbox hitbox,
            boolean useChargeMultiplier, int hitstopTicks, String color) {
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
            // スキル実行中のプレイヤーや訓練用マネキンに当たった場合は、まず衝突判定を行う
            // （ダメージ差が小さければ鍔迫り合い、大きければ火花＋強い側の一方的な勝ち）
            if (SkillClashManager.tryStartClash(player, target)) {
                return false;
            }
            hit |= applyHit(player, target, hitbox, useChargeMultiplier, hitstopTicks, color);
        }
        return hit;
    }

    /** ヒットボックスの形から攻撃の傾向を推定する（武器記憶用）。 */
    private static com.zoma1101.swordskill.smithing.WeaponMemoryCategory memoryCategory(
            DataSkillDefinition.Hitbox hitbox) {
        double[] size = hitbox.size();
        double right = size[0];
        double up = size[1];
        double forward = size[2];
        if (forward >= right * 1.5D && forward >= up * 1.5D) {
            return com.zoma1101.swordskill.smithing.WeaponMemoryCategory.THRUST;
        }
        if (right >= forward * 1.5D && right >= up * 1.5D) {
            return com.zoma1101.swordskill.smithing.WeaponMemoryCategory.SWEEP;
        }
        return com.zoma1101.swordskill.smithing.WeaponMemoryCategory.NORMAL;
    }

    private static boolean applyHit(ServerPlayer player, LivingEntity target, DataSkillDefinition.Hitbox hitbox,
            boolean useChargeMultiplier, int hitstopTicks, String color) {
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
        // 武器記憶: ヒットボックスの形から突き / 凪払いを判定して通知する
        com.zoma1101.swordskill.smithing.SmithingCombat.pushContext(
                memoryCategory(hitbox),
                com.zoma1101.swordskill.smithing.SmithingCombat.runningSkillCooldown(player));
        boolean hurt;
        try {
            hurt = target.hurt(player.damageSources().playerAttack(player), damage);
        } finally {
            com.zoma1101.swordskill.smithing.SmithingCombat.clearContext();
        }
        if (!hurt) {
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
        sendHitEffect(player, target, color, damageMultiplier);
        HitstopManager.apply(player, target, hitstopTicks, knockback);
        return true;
    }

    /**
     * 命中位置に火花バースト・光輪・斬痕・フラッシュを出すための通知を、対象を追跡している
     * クライアントと本人へ送る。位置は対象の攻撃者側の面、方向は攻撃者→対象の単位ベクトル。
     */
    public static void sendHitEffect(ServerPlayer player, LivingEntity target, String color, double power) {
        sendHitEffect(player, target, parseColor(color), power);
    }

    /** 色を整数で受け取る版。飛び道具（{@code AttackEffectEntity}）から呼ぶ。 */
    public static void sendHitEffect(ServerPlayer player, LivingEntity target, int color, double power) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 direction = center.subtract(player.position());
        direction = direction.lengthSqr() < 1.0E-6D ? player.getLookAngle().normalize() : direction.normalize();
        double radius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5D;
        Vec3 surface = center.subtract(direction.scale(radius + 0.05D));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new com.zoma1101.swordskill.payload.SkillHitEffectPayload(player.getId(), target.getId(),
                        color & 0xFFFFFF, (float) power, surface, direction));
    }

    /** "#RRGGBB" 形式のスキル色を整数へ変換する。失敗時は既定の青。 */
    public static int parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return 0x54D8FF;
        }
        String hex = color.startsWith("#") ? color.substring(1) : color;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return 0x54D8FF;
        }
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
            case "airborne_launch" -> {
                if (target instanceof LivingEntity living) {
                    double mult = Math.max(0.1D, effect.multiplier());
                    Vec3 away = target.position().subtract(player.position());
                    double horiz = Math.max(0.1D, away.horizontalDistance());
                    Vec3 launch = new Vec3(
                            (away.x / horiz) * 0.6D,
                            4.5D * mult,
                            (away.z / horiz) * 0.6D);
                    living.setDeltaMovement(launch);
                    // 位置を直接変更してクライアントに確実に伝える
                    living.teleportTo(living.getX(), living.getY() + 4.5D, living.getZ());
                    int dur = Math.max(1, effect.durationTicks());
                    int amp = effect.amplifier() > 0 ? effect.amplifier() : 1;
                    living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, dur, amp));
                    living.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE, dur + 40, 0));
                }
            }
            default -> {
                if (!SkillHitEffectRegistry.isRegistered(effect.type())) {
                    throw new IllegalStateException("Unregistered skill hit effect: " + effect.type());
                }
                SkillHitEffectRegistry.apply(effect, player, target);
            }
        }
    }
}
