package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.mixin.BlockDisplayAccessor;
import com.mojang.math.Transformation;
import com.zoma1101.swordskill.mixin.DisplayAccessor;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 着地衝撃（メイス・インパクトなど）の地面演出。
 *
 * <p>衝撃の中心付近の地表ブロックをクライアント専用のブロック表示として複製し、
 * 上下に揺らす。さらにブロック粉塵と短いカメラの上下動・ピッチ／ロールの揺れを重ねて、
 * 地面そのものが動いたように見せる。カメラ位置のオフセットは {@code CameraMixin} から
 * {@link #getCameraYOffset(double)} を参照する。</p>
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class GroundImpactHandler {
    private static final int IMPACT_TICKS = 15;
    private static final double IMPACT_RADIUS = 2.6D;
    private static final int BURST_DUST = 24;
    private static final int BURST_SPARKS = 8;
    private static final int RING_DUST = 5;
    private static final int DEFAULT_COLOR = 0x77DD77;
    private static final double BLOCK_RISE_HEIGHT = 0.9D;
    private static final int BLOCK_PULSE_TICKS = 6;
    private static final double BLOCK_DELAY_PER_DISTANCE = 1.5D;
    private static final double BLOCK_HORIZONTAL_NUDGE = 0.006D;
    private static final float BLOCK_SCALE = 1.08F;
    private static final float BLOCK_SINK = 0.0F;
    private static final RandomSource RANDOM = RandomSource.create();

    private static int remainingTicks;
    private static int totalTicks;
    private static int ageTicks;
    private static float shakeStrength;
    private static Vec3 center = Vec3.ZERO;
    private static BlockState groundState;
    private static ClientLevel observedLevel;
    private static final List<BlockVisual> BLOCK_VISUALS = new ArrayList<>();
    private static BlockPos pendingBlockBase;
    private static int nextVisualId = -1_000_000;

    private GroundImpactHandler() {
    }

    public static void onGroundImpact(double x, double y, double z, float strength, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ensureLevel(level);
        clearBlockVisuals();
        if (strength <= 0.0F) {
            return;
        }

        Vec3 position = new Vec3(x, y, z);
        double distance = minecraft.player == null ? 0.0D : minecraft.player.position().distanceTo(position);
        float attenuation = (float) Mth.clamp(1.0D - distance / 40.0D, 0.2D, 1.0D);
        float configured = ClientConfig.groundImpactShakeStrength.get().floatValue();
        float effective = Mth.clamp(strength * attenuation * configured, 0.0F, 3.0F);
        if (effective <= 0.0F) {
            return;
        }

        BlockPos base = findGround(level, position);
        BlockState state = base == null ? null : level.getBlockState(base);
        boolean visibleGround = state != null && !state.isAir()
                && state.getRenderShape() != RenderShape.INVISIBLE;

        if (remainingTicks <= 0 || effective >= shakeStrength) {
            shakeStrength = effective;
            totalTicks = IMPACT_TICKS;
            remainingTicks = IMPACT_TICKS;
            ageTicks = 0;
        }

        int tint = color & 0xFFFFFF;
        if (visibleGround) {
            VoxelShape shape = state.getCollisionShape(level, base);
            double top = shape.isEmpty() ? 1.0D : shape.max(Direction.Axis.Y);
            center = new Vec3(x, base.getY() + top + 0.03D, z);
            groundState = state;
            // エンティティ追加は次のクライアントtickまで遅延し、tick中のリスト変更を避ける。
            pendingBlockBase = base;
            // ブロック表示の天面より上から土煙を出す（表示ブロックに隠れないように）。
            Vec3 particleCenter = center.add(0.0D, 0.12D, 0.0D);
            spawnDustBurst(level, particleCenter, state);
            spawnSparkBurst(level, particleCenter, tint);
        } else {
            center = position;
            groundState = null;
            spawnSparkBurst(level, position, tint);
        }
    }

    /**
     * カメラの上下オフセット（ブロック単位）。{@code CameraMixin} の {@code setup} 終了時に加算される。
     */
    public static double getCameraYOffset(double partialTick) {
        if (remainingTicks <= 0 || shakeStrength <= 0.0F) {
            return 0.0D;
        }
        float age = ageTicks + (float) partialTick;
        float fade = remainingTicks / (float) totalTicks;
        return shakeStrength * 0.11F * fade * Mth.sin(age * 2.15F);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0 || shakeStrength <= 0.0F) {
            return;
        }
        float age = ageTicks + (float) event.getPartialTick();
        float fade = remainingTicks / (float) totalTicks;
        float amplitude = shakeStrength * 0.55F * fade;
        event.setPitch(event.getPitch() + Mth.sin(age * 2.15F) * amplitude);
        event.setRoll(event.getRoll() + Mth.cos(age * 1.65F) * amplitude * 0.65F);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clearBlockVisuals();
            return;
        }
        ensureLevel(level);
        if (pendingBlockBase != null) {
            BlockPos base = pendingBlockBase;
            pendingBlockBase = null;
            if (remainingTicks > 0) {
                spawnBlockVisuals(level, base);
            }
        }
        if (remainingTicks <= 0) {
            clearBlockVisuals();
            return;
        }

        updateBlockVisuals();
        float progress = 1.0F - remainingTicks / (float) totalTicks;
        if (groundState != null) {
            spawnDustRing(level, center, groundState, progress);
        }
        ageTicks++;
        remainingTicks--;
        if (remainingTicks <= 0) {
            clearBlockVisuals();
        }
    }

    private static void ensureLevel(ClientLevel level) {
        if (observedLevel == level) {
            return;
        }
        clearBlockVisuals();
        pendingBlockBase = null;
        remainingTicks = 0;
        totalTicks = 0;
        ageTicks = 0;
        shakeStrength = 0.0F;
        groundState = null;
        center = Vec3.ZERO;
        observedLevel = level;
    }

    /**
     * 衝撃中心から半径 3 ブロックの範囲にある地表ブロックを、クライアント専用のブロック表示エンティティとして複製する。
     * 表示ブロックを少し拡大して元ブロックを覆い、上方向に揺らす。
     */
    private static void spawnBlockVisuals(ClientLevel level, BlockPos base) {
        clearBlockVisuals();
        if (!ClientConfig.groundImpactBlockShake.get()) {
            return;
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 9) {
                    continue;
                }
                BlockPos surface = base.offset(dx, 0, dz);
                BlockState state = level.getBlockState(surface);
                if (!isShakeableSurface(level, surface, state)) {
                    continue;
                }

                Display.BlockDisplay display = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
                // サーバー側のエンティティIDと衝突しないよう、クライアント専用の負のIDを割り当てる。
                display.setId(allocateVisualId(level));
                ((BlockDisplayAccessor) display).swordskill$setBlockState(state);
                // 表示エンティティは地表ブロックの中に置かれるため、そのままでは真っ暗になる。
                // 直上の空気ブロックの明るさを上書きして、周囲の地面と同じ明るさで描画する。
                BlockPos lightPos = surface.above();
                ((DisplayAccessor) display).swordskill$setBrightnessOverride(
                        new Brightness(level.getBrightness(LightLayer.BLOCK, lightPos),
                                level.getBrightness(LightLayer.SKY, lightPos)));
                // 隣り合う表示ブロックの側面が同一平面にならないよう、ごく僅かに位置をずらす。
                double nudgeX = BLOCK_HORIZONTAL_NUDGE + dx * 0.0015D;
                double nudgeZ = BLOCK_HORIZONTAL_NUDGE + dz * 0.0015D;
                Vec3 anchor = new Vec3(surface.getX() + nudgeX,
                        surface.getY(),
                        surface.getZ() + nudgeZ);
                // 元ブロックを隠すため、少し拡大して沈み込ませる。
                ((DisplayAccessor) display).swordskill$setTransformation(
                        new Transformation(new Vector3f(0.0F, -BLOCK_SINK, 0.0F),
                                new Quaternionf(),
                                new Vector3f(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE),
                                new Quaternionf()));
                display.setPos(anchor);
                display.setOldPosAndRot();
                display.setNoGravity(true);
                display.setSilent(true);
                display.setInvulnerable(true);
                addDisplayToLevel(level, display);
                // add 直後の最初のフレームから描画できるよう、表示状態を一度初期化する。
                display.tick();
                double distance = Math.sqrt(dx * dx + dz * dz);
                int delay = (int) Math.round(distance * BLOCK_DELAY_PER_DISTANCE);
                BLOCK_VISUALS.add(new BlockVisual(display, anchor, delay));
            }
        }
    }

    /**
     * {@link ClientLevel#addFreshEntity} は {@link net.neoforged.neoforge.event.entity.EntityJoinLevelEvent} を
     * 発火し、一部の Mod によってキャンセルされることがある。キャンセルされると表示エンティティが
     * レベルに追加されず描画されないため、イベントを介さずセクション管理へ直接登録する。
     */
    private static void addDisplayToLevel(ClientLevel level, Display.BlockDisplay display) {
        try {
            Field field = ClientLevel.class.getDeclaredField("entityStorage");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            TransientEntitySectionManager<Entity> storage =
                    (TransientEntitySectionManager<Entity>) field.get(level);
            storage.addEntity(display);
            display.onAddedToLevel();
        } catch (Throwable throwable) {
            System.out.println("[GroundImpact] reflection add failed: " + throwable);
            level.addFreshEntity(display);
        }
    }

    private static int allocateVisualId(ClientLevel level) {
        int id;
        do {
            id = nextVisualId--;
        } while (level.getEntity(id) != null);
        return id;
    }

    private static boolean isShakeableSurface(ClientLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        if (!state.isCollisionShapeFullBlock(level, pos)) {
            return false;
        }
        return level.getBlockState(pos.above()).canBeReplaced();
    }

    private static void updateBlockVisuals() {
        if (BLOCK_VISUALS.isEmpty()) {
            return;
        }
        for (BlockVisual visual : BLOCK_VISUALS) {
            if (visual.display.isRemoved()) {
                continue;
            }
            int localAge = ageTicks - visual.delay;
            double lift = 0.0D;
            if (localAge >= 0 && localAge < BLOCK_PULSE_TICKS) {
                double t = localAge / (double) BLOCK_PULSE_TICKS;
                lift = BLOCK_RISE_HEIGHT * Math.sin(Math.PI * t);
            }
            visual.display.setPos(visual.anchor.x, visual.anchor.y + lift, visual.anchor.z);
        }
    }

    private static void clearBlockVisuals() {
        for (BlockVisual visual : BLOCK_VISUALS) {
            if (!visual.display.isRemoved()) {
                visual.display.discard();
            }
        }
        BLOCK_VISUALS.clear();
        pendingBlockBase = null;
    }

    private static BlockPos findGround(ClientLevel level, Vec3 position) {
        BlockPos pos = BlockPos.containing(position.x, position.y - 0.05D, position.z);
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getRenderShape() != RenderShape.INVISIBLE) {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }

    private static void spawnDustBurst(ClientLevel level, Vec3 origin, BlockState state) {
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int particle = 0; particle < BURST_DUST; particle++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double radius = 0.15D + RANDOM.nextDouble() * 1.35D;
            double outward = 0.08D + RANDOM.nextDouble() * 0.22D;
            level.addParticle(option,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + RANDOM.nextDouble() * 0.08D,
                    origin.z + Math.sin(angle) * radius,
                    Math.cos(angle) * outward,
                    0.08D + RANDOM.nextDouble() * 0.30D,
                    Math.sin(angle) * outward);
        }
    }

    private static void spawnDustRing(ClientLevel level, Vec3 origin, BlockState state, float progress) {
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        double radius = 0.35D + progress * IMPACT_RADIUS;
        for (int particle = 0; particle < RING_DUST; particle++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double speed = 0.05D + RANDOM.nextDouble() * 0.10D;
            level.addParticle(option,
                    origin.x + Math.cos(angle) * radius,
                    origin.y + RANDOM.nextDouble() * 0.06D,
                    origin.z + Math.sin(angle) * radius,
                    Math.cos(angle) * speed,
                    0.04D + RANDOM.nextDouble() * 0.10D,
                    Math.sin(angle) * speed);
        }
    }

    private static void spawnSparkBurst(ClientLevel level, Vec3 origin, int color) {
        int tint = color == 0 ? DEFAULT_COLOR : color;
        int density = Math.max(1, Math.round(BURST_SPARKS * ClientConfig.sparkDensity.get().floatValue()));
        for (int spark = 0; spark < density; spark++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
            double outward = 0.10D + RANDOM.nextDouble() * 0.25D;
            level.addParticle(new SkillSparkParticleOptions(tint, 0.8F + RANDOM.nextFloat() * 0.6F),
                    origin.x + Math.cos(angle) * 0.25D,
                    origin.y + 0.05D,
                    origin.z + Math.sin(angle) * 0.25D,
                    Math.cos(angle) * outward,
                    0.12D + RANDOM.nextDouble() * 0.35D,
                    Math.sin(angle) * outward);
        }
    }

    private record BlockVisual(Display.BlockDisplay display, Vec3 anchor, int delay) {
    }
}
