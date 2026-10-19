package com.zoma1101.swordskill.swordskills;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record DataSkillDefinition(
        int id,
        String name,
        int cooldown,
        double spCost,
        SkillData.SkillType type,
        java.util.List<SkillData.WeaponType> weapons,
        boolean hidden,
        int durationTicks,
        int transformLimitTicks,
        String animation,
        String color,
        Motion motion,
        List<MotionEvent> motions,
        List<Hitbox> hitboxes,
        List<ProjectileEvent> projectiles,
        List<SoundEvent> sounds,
        boolean legacyFallback,
        boolean replaceBuiltin,
        int rushStartTicks,
        String rushStartAnimation,
        String rushMoveAnimation,
        String rushFinishAnimation,
        RushCombo rushCombo,
        double trailLength,
        int shockwaveTicks,
        int hitstopTicks,
        SkillData.WeaponType proficiencyCategory,
        int unlockProficiency,
        boolean assignable,
        List<AnimationEvent> animations,
        List<PlayerEffectEvent> playerEffects,
        int invulnerabilityTicks,
        boolean endSkillOnGround) {
    public DataSkillDefinition {
        weapons = weapons == null ? List.of() : List.copyOf(weapons);
        motion = motion == null ? new Motion(0, 0, 0) : motion;
        motions = motions == null ? List.of() : List.copyOf(motions);
        hitboxes = hitboxes == null ? List.of() : List.copyOf(hitboxes);
        projectiles = projectiles == null ? List.of() : List.copyOf(projectiles);
        sounds = sounds == null ? List.of() : List.copyOf(sounds);
        animations = animations == null ? List.of() : List.copyOf(animations);
        playerEffects = playerEffects == null ? List.of() : List.copyOf(playerEffects);
        rushStartTicks = Math.max(0, rushStartTicks);
        rushStartAnimation = rushStartAnimation == null || rushStartAnimation.isBlank()
                ? "start" : rushStartAnimation;
        rushMoveAnimation = rushMoveAnimation == null || rushMoveAnimation.isBlank()
                ? "move" : rushMoveAnimation;
        rushFinishAnimation = rushFinishAnimation == null || rushFinishAnimation.isBlank()
                ? "finish" : rushFinishAnimation;
        trailLength = !Double.isFinite(trailLength) || trailLength <= 0.0 ? 1.5 : trailLength;
        shockwaveTicks = Math.max(0, shockwaveTicks);
        hitstopTicks = Math.max(-1, hitstopTicks);
        // null なら「先頭の武器種」が熟練度カテゴリとして使われる
        unlockProficiency = Math.max(0, unlockProficiency);
    }

    public record Motion(double forward, double strafe, double vertical) {
    }

    /**
     * 指定 tick の移動。{@code set} なら加算ではなく置き換え、{@code repeat} なら
     * その tick 以降ずっと適用する。{@code trigger} は TICK / GROUND。
     */
    public record MotionEvent(int tick, double forward, double strafe, double vertical,
            boolean set, boolean repeat, String trigger) {
        public MotionEvent {
            trigger = trigger == null ? "TICK" : trigger;
        }
    }

    public record RushCombo(
            Motion startMotion,
            double pitchLimitDegrees,
            boolean useChargeMultiplier,
            int invulnerabilityTicks,
            double searchRadius,
            double contactRadius,
            double contactMotionForward,
            int latestContactTick,
            int noContactTimeoutTick,
            int contactAnimationOffset,
            int finishContactOffset) {
    }

    public record Hitbox(
            int tick,
            double[] offset,
            double[] size,
            double damageMultiplier,
            String damageType,
            double knockback,
            double knockbackMultiplier,
            boolean repeatWhileRushing,
            List<Effect> effects,
            boolean hitboxEnabled,
            String sound,
            String trigger) {
        public Hitbox {
            offset = offset.clone();
            size = size.clone();
            damageType = damageType == null ? "BASE" : damageType;
            effects = effects == null ? List.of() : List.copyOf(effects);
            sound = sound == null ? "" : sound;
            trigger = trigger == null ? "TICK" : trigger;
        }
    }

    public record ProjectileEvent(
            int tick,
            double[] offset,
            double[] rotation,
            double playerPitchXMultiplier,
            double playerPitchZMultiplier,
            double renderPitch,
            boolean horizontal,
            double[] size,
            double damageMultiplier,
            String damageType,
            double knockback,
            double knockbackMultiplier,
            int durationTicks,
            String particle,
            double travelDistance,
            double[] movement,
            String sound,
            String trigger,
            double fallDamageScale,
            PathSpec path,
            String randomGroup,
            int randomIndex) {
        public ProjectileEvent {
            offset = offset.clone();
            rotation = rotation.clone();
            size = size.clone();
            movement = movement == null ? null : movement.clone();
            damageType = damageType == null ? "BASE" : damageType;
            particle = particle == null ? "skill_particle_sword/blue" : particle;
            sound = sound == null ? "" : sound;
            trigger = trigger == null ? "TICK" : trigger;
            randomGroup = randomGroup == null ? "" : randomGroup;
        }
    }

    /**
     * 移動しながら伸びる攻撃（突進の斬撃など）のための指定。
     *
     * @param startTick 始点を記録する tick
     * @param position  始点→現在位置のどこに出すか (0=現在位置, 0.5=中間, 1=始点)
     * @param rotation  進行方向から向きを決めるか
     * @param sizeScale 0 より大きければ size.z を「移動量の平均 × この値」にする
     */
    public record PathSpec(int startTick, double position, boolean rotation, double sizeScale) {
    }

    /** スキル中にクライアントへ送るアニメーション切り替え。 */
    public record AnimationEvent(int tick, String animation, String trigger, String randomGroup, int randomIndex) {
        public AnimationEvent {
            animation = animation == null ? "" : animation;
            trigger = trigger == null ? "TICK" : trigger;
            randomGroup = randomGroup == null ? "" : randomGroup;
        }
    }

    /** スキル中に自分へ付与するポーション効果（落下ダメージ無効など）。 */
    public record PlayerEffectEvent(int tick, String effect, int durationTicks, int amplifier,
            boolean showParticles, String trigger) {
        public PlayerEffectEvent {
            trigger = trigger == null ? "TICK" : trigger;
        }
    }

    public record SoundEvent(int tick, String sound, float volume, float pitch, String trigger) {
        public SoundEvent {
            sound = sound == null ? "" : sound;
            trigger = trigger == null ? "TICK" : trigger;
        }
    }

    public record Effect(
            String type,
            double multiplier,
            int durationTicks,
            int amplifier,
            String effectId) {
    }

    public record EffectContext(ServerPlayer player, Effect effect) {
    }
}
