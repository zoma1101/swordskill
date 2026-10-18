package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 突きスキルの攻撃時に、突きのビームを装飾する衝撃波を発光する粒子のリングとして表現する。
 * リングは前方へ流れながら広がり、粒子は外側と前方へ散っていく。色はスキルのトレイル色と同じ。
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ShockwaveHandler {
    private static final int DEFAULT_COLOR = 0x54D8FF;
    private static final double WAVE_TRAVEL_SPEED = 1.7D;
    private static final int RING_SPARKS_PER_TICK = 3;
    private static final int BURST_SPARKS_PER_TICK = 7;
    private static final int STREAK_SPARKS_PER_TICK = 3;
    private static final float BURST_PROGRESS = 0.35F;

    private static final List<ShockwaveEffect> EFFECTS = new ArrayList<>();
    private static ClientLevel observedLevel;

    private ShockwaveHandler() {
    }

    public static void onShockwave(int entityId, int ticks, String color) {
        if (ticks <= 0) {
            return;
        }
        EFFECTS.removeIf(effect -> effect.entityId == entityId);
        EFFECTS.add(new ShockwaveEffect(entityId, ticks, parseColor(color)));
    }

    private static int parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return DEFAULT_COLOR;
        }
        String hex = color.startsWith("#") ? color.substring(1) : color;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            return DEFAULT_COLOR;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (observedLevel != level) {
            EFFECTS.clear();
            observedLevel = level;
        }
        if (level == null || EFFECTS.isEmpty()) {
            return;
        }
        Iterator<ShockwaveEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            ShockwaveEffect effect = iterator.next();
            Entity entity = level.getEntity(effect.entityId);
            if (entity instanceof LivingEntity living && !living.isInvisible()) {
                spawnShockwave(level, living, effect);
            }
            if (--effect.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private static void spawnShockwave(ClientLevel level, LivingEntity entity, ShockwaveEffect effect) {
        float progress = Mth.clamp(1.0F - effect.remainingTicks / (float) effect.totalTicks, 0.0F, 1.0F);
        RandomSource random = entity.getRandom();
        Vec3 origin = entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D);
        Vec3 axis = safeNormalize(entity.getViewVector(1.0F), new Vec3(0.0D, 0.0D, 1.0D));
        Vec3 reference = Math.abs(axis.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = safeNormalize(axis.cross(reference), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 up = safeNormalize(side.cross(axis), new Vec3(0.0D, 1.0D, 0.0D));

        // ビームに沿って前進しながら広がる、発光粒子のリング
        double phase = (progress * WAVE_TRAVEL_SPEED) % 1.0D;
        double radius = 0.35D + phase * 1.90D;
        double distance = 0.75D + phase * 2.10D;
        double angleOffset = random.nextDouble() * Math.PI * 2.0D;
        for (int spark = 0; spark < RING_SPARKS_PER_TICK; spark++) {
            double angle = angleOffset + (Math.PI * 2.0D) * spark / RING_SPARKS_PER_TICK;
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 position = origin.add(axis.scale(distance + (random.nextDouble() - 0.5D) * 0.55D))
                    .add(radial.scale(radius + (random.nextDouble() - 0.5D) * 0.35D));
            Vec3 velocity = radial.scale(0.30D + random.nextDouble() * 0.28D)
                    .add(axis.scale(0.04D + random.nextDouble() * 0.10D));
            addSpark(level, position, velocity, effect.color, 0.8F + random.nextFloat() * 0.5F);
        }

        // 突き出し位置で弾ける衝撃のバースト
        if (progress < BURST_PROGRESS) {
            for (int spark = 0; spark < BURST_SPARKS_PER_TICK; spark++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                Vec3 position = origin.add(axis.scale(0.45D + random.nextDouble() * 0.55D))
                        .add(radial.scale(0.20D + random.nextDouble() * 0.40D));
                Vec3 velocity = radial.scale(0.48D + random.nextDouble() * 0.45D)
                        .add(axis.scale(0.22D + random.nextDouble() * 0.20D));
                addSpark(level, position, velocity, effect.color, 1.0F + random.nextFloat() * 0.6F);
            }
            if (ClientConfig.sparkStreaks.get()) {
                for (int spark = 0; spark < STREAK_SPARKS_PER_TICK; spark++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
                    Vec3 position = origin.add(axis.scale(0.45D + random.nextDouble() * 0.55D))
                            .add(radial.scale(0.18D + random.nextDouble() * 0.40D));
                    Vec3 velocity = radial.scale(0.70D + random.nextDouble() * 0.70D)
                            .add(axis.scale(0.25D + random.nextDouble() * 0.35D));
                    addStreak(level, position, velocity, effect.color, 0.9F + random.nextFloat() * 0.6F);
                }
            }
        }
    }

    private static void addSpark(ClientLevel level, Vec3 position, Vec3 velocity, int color, float scale) {
        level.addParticle(new SkillSparkParticleOptions(color, scale),
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    private static void addStreak(ClientLevel level, Vec3 position, Vec3 velocity, int color, float scale) {
        level.addParticle(new SparkStreakParticleOptions(color, scale),
                position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        double lengthSqr = vector.lengthSqr();
        return lengthSqr < 1.0E-8D ? fallback : vector.scale(1.0D / Math.sqrt(lengthSqr));
    }

    private static final class ShockwaveEffect {
        final int entityId;
        final int totalTicks;
        final int color;
        int remainingTicks;

        ShockwaveEffect(int entityId, int totalTicks, int color) {
            this.entityId = entityId;
            this.totalTicks = totalTicks;
            this.color = color;
            this.remainingTicks = totalTicks;
        }
    }
}
