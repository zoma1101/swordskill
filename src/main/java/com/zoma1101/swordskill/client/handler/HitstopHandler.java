package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * サーバーから通知されたヒットストップをクライアントで演出する。
 * 攻撃者のキー入力は一切止めない(ダッシュ等を妨げない)代わりに、
 * カメラシェイクと、命中させた本人への一瞬のFOVパンチで打撃感を出す。
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class HitstopHandler {
    private static final RandomSource RANDOM = RandomSource.create();
    private static int remainingTicks;
    private static int totalTicks;
    private static int attackerId = -1;
    private static int targetId = -1;

    private HitstopHandler() {
    }

    public static void onHitstop(int attacker, int target, int ticks) {
        if (ticks <= 0) {
            return;
        }
        if (ticks > remainingTicks) {
            remainingTicks = ticks;
        }
        if (ticks > totalTicks) {
            totalTicks = ticks;
        }
        attackerId = attacker;
        targetId = target;
    }

    public static boolean isActive() {
        return remainingTicks > 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remainingTicks > 0 && --remainingTicks <= 0) {
            remainingTicks = 0;
            totalTicks = 0;
            attackerId = -1;
            targetId = -1;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingTicks <= 0) {
            return;
        }
        float strength = ServerConfig.hitstopShakeStrength.get().floatValue();
        if (strength <= 0.0F) {
            return;
        }
        float progress = totalTicks <= 0 ? 0.0F : remainingTicks / (float) totalTicks;
        float amplitude = strength * 0.7F * progress;
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * amplitude * 2.0F);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * amplitude * 2.0F);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * amplitude * 3.0F);
    }

    /**
     * 命中させた本人にだけ、一瞬のFOVパンチ(ズームイン)を入れて打撃の重みを伝える。
     * 入力は止めないので操作感には影響しない。
     */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (remainingTicks <= 0 || !event.usedConfiguredFov()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.getId() != attackerId) {
            return;
        }
        float strength = ServerConfig.hitstopFovPunch.get().floatValue();
        if (strength <= 0.0F) {
            return;
        }
        float progress = totalTicks <= 0 ? 0.0F : remainingTicks / (float) totalTicks;
        float punch = strength * 0.06F * progress * progress;
        event.setFOV(event.getFOV() * (1.0F - punch));
    }
}
