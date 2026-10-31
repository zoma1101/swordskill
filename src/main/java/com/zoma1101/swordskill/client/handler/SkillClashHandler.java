package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.AnimationUtils;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
import com.zoma1101.swordskill.payload.SkillClashPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * 鍔迫り合い（ブレードロック）と飛翔斬撃の相殺をクライアント側で演出する。
 * ロック中は中央のゲージを {@code Hud_Registry} が描画し、ここでは火花とアニメーションを扱う。
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SkillClashHandler {
    private static final int SAFETY_EXTRA_TICKS = 40;
    private static final int DEFAULT_COLOR_A = 0x54D8FF;
    private static final int DEFAULT_COLOR_B = 0xFFC46B;
    private static final int ORANGE_SPARK = 0xFFAA33;
    private static final int HOT_ORANGE = 0xFFF2CC;

    private static int clashId = -1;
    private static int playerAId = -1;
    private static int playerBId = -1;
    private static int colorA = DEFAULT_COLOR_A;
    private static int colorB = DEFAULT_COLOR_B;
    private static int totalTicks = 1;
    private static long endGameTime;
    private static Vec3 center = Vec3.ZERO;
    /** 鍔迫り合いのポーズを再生中のエンティティ。終了通知が届かなかった場合の後始末用。 */
    private static final Set<Integer> posedEntities = new HashSet<>();
    private static ClientLevel observedLevel;

    private SkillClashHandler() {
    }

    public static void onPayload(SkillClashPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        switch (payload.phase()) {
            case SkillClashPayload.PHASE_LOCK_START -> start(level, payload);
            case SkillClashPayload.PHASE_LOCK_END -> end(level, payload);
            case SkillClashPayload.PHASE_SPARK -> burst(level,
                    new Vec3(payload.x(), payload.y(), payload.z()), payload.colorA(), payload.colorB(), 1.4F);
            default -> {
            }
        }
    }

    private static void start(ClientLevel level, SkillClashPayload payload) {
        if (payload.playerA() == 0) {
            return;
        }
        if (clashId == payload.clashId()) {
            return;
        }
        // 直前のロックが残っていたら後始末してから開始する
        clear(level, false);

        clashId = payload.clashId();
        playerAId = payload.playerA();
        playerBId = payload.playerB();
        colorA = payload.colorA();
        colorB = payload.colorB();
        totalTicks = Math.max(1, payload.lockTicks());
        endGameTime = level.getGameTime() + totalTicks;
        center = new Vec3(payload.x(), payload.y(), payload.z());
        observedLevel = level;

        burst(level, center, colorA, colorB, 1.6F);
        playAnimation(level, playerAId);
        playAnimation(level, playerBId);
    }

    private static void end(ClientLevel level, SkillClashPayload payload) {
        // 別の鍔迫り合いを表示中でも、終了した組のポーズは必ず解除する
        // （近距離で 2 組が同時にロックした場合に姿勢が残るのを防ぐ）
        if (payload.playerA() != 0) {
            stopAnimation(level, payload.playerA());
            stopAnimation(level, payload.playerB());
        }
        if (clashId != payload.clashId()) {
            return;
        }
        Vec3 position = new Vec3(payload.x(), payload.y(), payload.z());
        burst(level, position, colorA, colorB, 2.0F);
        clear(level, false);
    }

    private static void playAnimation(ClientLevel level, int entityId) {
        String animation = ClientConfig.clashAnimation.get();
        if (animation == null || animation.isBlank()) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            AnimationUtils.playClashAnimation(player, animation);
            posedEntities.add(entityId);
        }
    }

    private static void clear(ClientLevel level, boolean stopAnimation) {
        if (stopAnimation && level != null && !posedEntities.isEmpty()) {
            for (int entityId : Set.copyOf(posedEntities)) {
                stopAnimation(level, entityId);
            }
        }
        // 別の階層へ移動したら、ID が再利用されて別人のポーズを止めないよう記録だけ捨てる
        if (level == null) {
            posedEntities.clear();
        }
        clashId = -1;
        playerAId = -1;
        playerBId = -1;
        totalTicks = 1;
        endGameTime = 0L;
        center = Vec3.ZERO;
    }

    private static void stopAnimation(ClientLevel level, int entityId) {
        // 自分がポーズを当てた相手だけを止める（clashAnimation が空のときに
        // 関係ないスキルのアニメーションを消さないため）
        if (!posedEntities.remove(entityId)) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            AnimationUtils.stopClashAnimation(player);
        }
    }

    public static boolean isActive() {
        return clashId >= 0;
    }

    /** 0 = 開始直後、1 = 終了。HUD のゲージ用。 */
    public static float getProgress() {
        if (!isActive()) {
            return 0.0F;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 0.0F;
        }
        long remaining = endGameTime - level.getGameTime();
        return Mth.clamp(1.0F - remaining / (float) totalTicks, 0.0F, 1.0F);
    }

    public static int getColorA() {
        return colorA;
    }

    public static int getColorB() {
        return colorB;
    }

    public static int getRemainingTicks() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !isActive()) {
            return 0;
        }
        return (int) Math.max(0L, endGameTime - level.getGameTime());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            if (observedLevel != null) {
                clear(null, false);
                observedLevel = null;
            }
            return;
        }
        if (observedLevel != level) {
            clear(null, false);
            observedLevel = level;
            return;
        }
        if (!isActive()) {
            return;
        }
        if (level.getGameTime() > endGameTime + SAFETY_EXTRA_TICKS) {
            // 終了通知が届かなかった場合の保険
            clear(level, true);
            return;
        }
        if (ClientConfig.clashSparks.get()) {
            spawnLockSparks(level);
        }
    }

    private static void spawnLockSparks(ClientLevel level) {
        double density = ClientConfig.sparkDensity.get();
        if (density <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        int count = Mth.clamp((int) Math.round(3.0D * density), 1, 8);
        for (int index = 0; index < count; index++) {
            Vec3 scatter = randomUnit(random).scale(0.25D + random.nextDouble() * 0.55D);
            Vec3 spawn = center.add(scatter.scale(0.35D));
            int color = sparkColor(random, colorA, colorB);
            float scale = 0.8F + random.nextFloat() * 0.8F;
            // 速度方向へ伸びる spark_streak の火花。低速度でも筋として見える長さで飛ばす。
            level.addParticle(new SparkStreakParticleOptions(color, scale),
                    spawn.x, spawn.y, spawn.z,
                    scatter.x * 1.15D, 0.18D + random.nextDouble() * 0.45D, scatter.z * 1.15D);
        }
    }

    private static void burst(ClientLevel level, Vec3 position, int primary, int secondary, float power) {
        if (!ClientConfig.clashSparks.get()) {
            return;
        }
        double density = ClientConfig.sparkDensity.get();
        if (density <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        int count = Mth.clamp((int) Math.round(18.0D * density * power), 6, 56);
        for (int index = 0; index < count; index++) {
            Vec3 direction = randomUnit(random);
            Vec3 velocity = direction.scale(0.55D + random.nextDouble() * 1.45D);
            Vec3 spawn = position.add(randomUnit(random).scale(random.nextDouble() * 0.25D));
            int color = sparkColor(random, primary, secondary);
            float scale = 0.9F + random.nextFloat() * 0.9F;
            level.addParticle(new SparkStreakParticleOptions(color, scale),
                    spawn.x, spawn.y, spawn.z, velocity.x, velocity.y, velocity.z);
        }
    }

    private static int sparkColor(RandomSource random, int colorA, int colorB) {
        float r = random.nextFloat();
        if (r < 0.35F) return colorA;
        if (r < 0.65F) return colorB;
        if (r < 0.85F) return ORANGE_SPARK;
        return HOT_ORANGE;
    }

    private static Vec3 randomUnit(RandomSource random) {
        Vec3 vector = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        double lengthSqr = vector.lengthSqr();
        return lengthSqr < 1.0E-8D ? new Vec3(0.0D, 1.0D, 0.0D)
                : vector.scale(1.0D / Math.sqrt(lengthSqr));
    }
}
