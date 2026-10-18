package com.zoma1101.swordskill.client.handler;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
import com.zoma1101.swordskill.payload.SkillHitEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * スキル命中時の演出を担当する。サーバーから届いた {@link SkillHitEffectPayload} をもとに、
 * 火花のバースト・光輪（拡がるリング）・斬痕・画面フラッシュを出す。
 * 色はスキルのトレイル色と同じ。
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SkillHitEffectHandler {
    private static final int DEFAULT_COLOR = 0x54D8FF;
    private static final int RING_TICKS = 7;
    private static final int DECAL_TICKS = 11;
    private static final int FLASH_TICKS = 5;
    private static final float MAX_FLASH = 0.32F;

    private static final RenderType DECAL_RENDER_TYPE = RenderType.create(
            "swordskill_slash_decal",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final List<RingEffect> RINGS = new ArrayList<>();
    private static final List<Decal> DECALS = new ArrayList<>();
    private static final Map<Integer, Long> LAST_HIT_TICK = new HashMap<>();
    private static int flashTicks;
    private static float flashStrength;
    private static ClientLevel observedLevel;

    private SkillHitEffectHandler() {
    }

    public static void onHitEffect(SkillHitEffectPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int color = WeaponTrailHandler.saturateColor(payload.color() & 0xFFFFFF);
        float power = Mth.clamp(payload.power(), 0.5F, 4.0F);
        Vec3 position = payload.position();
        Vec3 direction = safeNormalize(payload.direction(), new Vec3(0.0D, 0.0D, 1.0D));

        if (ClientConfig.hitSparks.get() || ClientConfig.sparkStreaks.get()) {
            spawnBurst(level, position, direction, color, power);
        }

        // 同一tickや連続ヒット（ラッシュの繰り返し等）では、光輪・斬痕・フラッシュを使い回す
        long gameTime = level.getGameTime();
        Long previous = LAST_HIT_TICK.get(payload.targetId());
        if (previous != null && gameTime - previous < 3L) {
            return;
        }
        LAST_HIT_TICK.put(payload.targetId(), gameTime);

        if (ClientConfig.hitRing.get()) {
            RINGS.add(new RingEffect(position, direction, color, RING_TICKS, RING_TICKS));
        }
        if (ClientConfig.slashDecals.get()) {
            DECALS.add(createDecal(position, direction, color, power));
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientConfig.hitFlash.get() && minecraft.player != null
                && minecraft.player.getId() == payload.attackerId()) {
            flashStrength = Math.min(1.0F, 0.45F + power * 0.18F);
            flashTicks = FLASH_TICKS;
        }
    }

    /** 画面フラッシュの強さ（0〜1）。GUI レイヤーから参照する。 */
    public static float getFlashIntensity() {
        return flashTicks <= 0 ? 0.0F : flashStrength * (flashTicks / (float) FLASH_TICKS) * MAX_FLASH;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (observedLevel != level) {
            RINGS.clear();
            DECALS.clear();
            LAST_HIT_TICK.clear();
            flashTicks = 0;
            observedLevel = level;
        }
        if (level == null) {
            return;
        }
        if (flashTicks > 0) {
            flashTicks--;
        }

        Iterator<RingEffect> ringIterator = RINGS.iterator();
        while (ringIterator.hasNext()) {
            RingEffect ring = ringIterator.next();
            if (ClientConfig.hitRing.get()) {
                spawnRingParticles(level, ring);
            }
            if (--ring.remaining <= 0) {
                ringIterator.remove();
            }
        }

        Iterator<Decal> decalIterator = DECALS.iterator();
        while (decalIterator.hasNext()) {
            Decal decal = decalIterator.next();
            if (--decal.remaining <= 0) {
                decalIterator.remove();
            }
        }
    }

    private static void spawnBurst(ClientLevel level, Vec3 position, Vec3 normal, int color, float power) {
        double density = ClientConfig.sparkDensity.get();
        if (density <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        if (ClientConfig.hitSparks.get()) {
            int count = Mth.clamp((int) Math.round((4.0D + power * 3.5D) * density), 1, 40);
            for (int index = 0; index < count; index++) {
                Vec3 scatter = randomUnit(random).scale(0.55D + random.nextDouble() * 1.05D);
                Vec3 velocity = scatter.add(normal.scale(-(0.25D + random.nextDouble() * 0.55D)));
                Vec3 spawn = position.add(normal.scale(0.02D + random.nextDouble() * 0.12D))
                        .add(randomUnit(random).scale(random.nextDouble() * 0.12D));
                float scale = Mth.clamp((0.7F + random.nextFloat() * 0.7F) * (0.85F + power * 0.12F), 0.01F, 4.0F);
                level.addParticle(new SkillSparkParticleOptions(color, scale),
                        spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
            }
        }
        if (ClientConfig.sparkStreaks.get()) {
            int count = Mth.clamp((int) Math.round((2.0D + power * 1.8D) * density), 1, 14);
            for (int index = 0; index < count; index++) {
                Vec3 scatter = randomUnit(random).scale(0.75D + random.nextDouble() * 1.15D);
                Vec3 velocity = scatter.add(normal.scale(-(0.20D + random.nextDouble() * 0.50D)));
                Vec3 spawn = position.add(normal.scale(0.04D + random.nextDouble() * 0.14D))
                        .add(randomUnit(random).scale(random.nextDouble() * 0.10D));
                level.addParticle(new SparkStreakParticleOptions(color, 0.8F + random.nextFloat() * 0.6F),
                        spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
            }
        }
    }

    private static void spawnRingParticles(ClientLevel level, RingEffect ring) {
        double density = ClientConfig.sparkDensity.get();
        if (density <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        float progress = 1.0F - ring.remaining / (float) ring.total;
        double radius = 0.16D + progress * (0.75D + ring.total * 0.04D);
        Vec3 reference = Math.abs(ring.normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = safeNormalize(ring.normal.cross(reference), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 up = safeNormalize(side.cross(ring.normal), new Vec3(0.0D, 1.0D, 0.0D));
        int count = Mth.clamp((int) Math.round(5.0D * density), 2, 16);
        double angleOffset = random.nextDouble() * Math.PI * 2.0D;
        for (int spark = 0; spark < count; spark++) {
            double angle = angleOffset + Math.PI * 2.0D * spark / count;
            Vec3 radial = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 position = ring.position
                    .add(radial.scale(radius + (random.nextDouble() - 0.5D) * 0.10D))
                    .add(ring.normal.scale(-0.02D));
            Vec3 velocity = radial.scale(0.22D + random.nextDouble() * 0.24D)
                    .add(ring.normal.scale(-(0.08D + random.nextDouble() * 0.12D)));
            level.addParticle(new SkillSparkParticleOptions(ring.color, 0.7F + random.nextFloat() * 0.5F),
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static Decal createDecal(Vec3 position, Vec3 normal, int color, float power) {
        RandomSource random = RandomSource.create();
        Vec3 reference = Math.abs(normal.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = safeNormalize(normal.cross(reference), new Vec3(1.0D, 0.0D, 0.0D));
        Vec3 up = safeNormalize(side.cross(normal), new Vec3(0.0D, 1.0D, 0.0D));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        Vec3 longAxis = safeNormalize(side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle))),
                side);
        Vec3 shortAxis = safeNormalize(normal.cross(longAxis), up);
        float length = (float) (0.75D + power * 0.22D);
        float width = (float) (0.10D + power * 0.035D);
        return new Decal(position, normal, longAxis, shortAxis, color, length, width, DECAL_TICKS, DECAL_TICKS);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        if (DECALS.isEmpty() || !ClientConfig.slashDecals.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(DECAL_RENDER_TYPE);
        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        Matrix4f inverseMatrix = new Matrix4f(matrix).invert();
        Vec3 cameraPosition = event.getCamera().getPosition();
        for (Decal decal : DECALS) {
            float life = decal.remaining / (float) decal.total;
            if (life <= 0.0F) {
                continue;
            }
            renderSlashShape(vertices, matrix, inverseMatrix, cameraPosition, decal, life * life);
        }
        buffers.endBatch(DECAL_RENDER_TYPE);
    }

    private static void renderSlashShape(VertexConsumer vertices, Matrix4f matrix, Matrix4f inverseMatrix,
            Vec3 cameraPosition, Decal decal, float alpha) {
        int core = mixWithWhite(decal.color, 0.55F);
        int segments = 6;
        Vec3 previousLeft = null;
        Vec3 previousRight = null;
        for (int index = 0; index <= segments; index++) {
            float t = index / (float) segments;
            Vec3 center = decal.position.add(decal.longAxis.scale((t - 0.5F) * decal.length));
            double halfWidth = decal.width * Math.sin(Math.PI * t);
            Vec3 left = center.subtract(decal.shortAxis.scale(halfWidth));
            Vec3 right = center.add(decal.shortAxis.scale(halfWidth));
            if (index > 0) {
                addVertex(vertices, matrix, inverseMatrix, cameraPosition, previousLeft, decal.color, alpha * 0.85F);
                addVertex(vertices, matrix, inverseMatrix, cameraPosition, previousRight, core, alpha * 0.95F);
                addVertex(vertices, matrix, inverseMatrix, cameraPosition, right, core, alpha * 0.95F);
                addVertex(vertices, matrix, inverseMatrix, cameraPosition, left, decal.color, alpha * 0.85F);
            }
            previousLeft = left;
            previousRight = right;
        }
    }

    private static void addVertex(VertexConsumer vertices, Matrix4f matrix, Matrix4f inverseMatrix,
            Vec3 cameraPosition, Vec3 worldPosition, int color, float alpha) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int vertexAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Vec3 cameraRelative = worldPosition.subtract(cameraPosition);
        Vector3f localPosition = inverseMatrix.transformPosition(
                new Vector3f((float) cameraRelative.x, (float) cameraRelative.y, (float) cameraRelative.z));
        vertices.addVertex(matrix, localPosition.x(), localPosition.y(), localPosition.z())
                .setColor(red, green, blue, vertexAlpha);
    }

    private static Vec3 randomUnit(RandomSource random) {
        Vec3 vector = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return safeNormalize(vector, new Vec3(0.0D, 1.0D, 0.0D));
    }

    private static Vec3 safeNormalize(Vec3 vector, Vec3 fallback) {
        double lengthSqr = vector.lengthSqr();
        return lengthSqr < 1.0E-8D ? fallback : vector.scale(1.0D / Math.sqrt(lengthSqr));
    }

    private static int mixWithWhite(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return ((int) (red + (255 - red) * amount) << 16)
                | ((int) (green + (255 - green) * amount) << 8)
                | (int) (blue + (255 - blue) * amount);
    }

    private static final class RingEffect {
        final Vec3 position;
        final Vec3 normal;
        final int color;
        final int total;
        int remaining;

        RingEffect(Vec3 position, Vec3 normal, int color, int total, int remaining) {
            this.position = position;
            this.normal = normal;
            this.color = color;
            this.total = total;
            this.remaining = remaining;
        }
    }

    private static final class Decal {
        final Vec3 position;
        final Vec3 normal;
        final Vec3 longAxis;
        final Vec3 shortAxis;
        final int color;
        final float length;
        final float width;
        final int total;
        int remaining;

        Decal(Vec3 position, Vec3 normal, Vec3 longAxis, Vec3 shortAxis, int color,
                float length, float width, int total, int remaining) {
            this.position = position;
            this.normal = normal;
            this.longAxis = longAxis;
            this.shortAxis = shortAxis;
            this.color = color;
            this.length = length;
            this.width = width;
            this.total = total;
            this.remaining = remaining;
        }
    }
}
