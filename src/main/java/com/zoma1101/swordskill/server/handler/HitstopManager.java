package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.payload.HitstopPayload;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * ソードスキル命中時に攻撃者と対象を数tick停止させるヒットストップを管理する。
 * 停止中は対象の速度を毎tickゼロにし、停止が明けたtickでノックバックを付与する。
 * クライアントにはカメラ演出のための通知を送る(攻撃者の入力は止めない)。
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID)
public final class HitstopManager {
    private static final Map<UUID, Freeze> FREEZES = new HashMap<>();

    private HitstopManager() {
    }

    /**
     * Hit-stop を開始する。{@code knockback} は停止が明けたtickで対象に付与される。
     * ticks が 0 以下の場合は何もしない(呼び出し側で即時ノックバック済み)。
     */
    public static void apply(LivingEntity attacker, LivingEntity target, int ticks,
            @Nullable Vec3 knockback) {
        if (ticks <= 0 || attacker == null || target == null) {
            return;
        }
        addFreeze(target, ticks).pendingKnockback = knockback;
        if (!(attacker instanceof ServerPlayer)) {
            addFreeze(attacker, ticks);
        }
        if (attacker instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new HitstopPayload(attacker.getId(), target.getId(), ticks));
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                    new HitstopPayload(attacker.getId(), target.getId(), ticks));
        }
    }

    private static Freeze addFreeze(LivingEntity entity, int ticks) {
        Freeze existing = FREEZES.get(entity.getUUID());
        if (existing != null) {
            existing.ticks = Math.max(existing.ticks, ticks);
            return existing;
        }
        Freeze freeze = new Freeze(entity, ticks);
        FREEZES.put(entity.getUUID(), freeze);
        return freeze;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (FREEZES.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Freeze>> iterator = FREEZES.entrySet().iterator();
        while (iterator.hasNext()) {
            Freeze freeze = iterator.next().getValue();
            LivingEntity entity = freeze.entity;
            if (entity.isRemoved() || entity.level().isClientSide()) {
                iterator.remove();
                continue;
            }
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            // 停止中はダメージを受けたときの赤い外見を維持する
            entity.hurtTime = Math.max(entity.hurtTime, freeze.ticks + 1);
            entity.hurtMarked = true;
            if (entity.level() instanceof ServerLevel serverLevel) {
                ClientboundHurtAnimationPacket hurtPacket =
                        new ClientboundHurtAnimationPacket(entity.getId(), entity.getYRot());
                for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
                    if (sp.level() == serverLevel && sp.distanceToSqr(entity) < 4096.0D) {
                        sp.connection.send(hurtPacket);
                    }
                }
            }
            if (--freeze.ticks <= 0) {
                if (freeze.pendingKnockback != null) {
                    entity.setDeltaMovement(freeze.pendingKnockback);
                    entity.hurtMarked = true;
                }
                iterator.remove();
            }
        }
    }

    private static final class Freeze {
        final LivingEntity entity;
        int ticks;
        @Nullable
        Vec3 pendingKnockback;

        Freeze(LivingEntity entity, int ticks) {
            this.entity = entity;
            this.ticks = ticks;
        }
    }
}
