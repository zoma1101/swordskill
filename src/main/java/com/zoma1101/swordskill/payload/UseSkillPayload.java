package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.server.handler.SkillChainTracker;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.swordskills.SkillChains;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static com.zoma1101.swordskill.data.SkillDataFetcher.isSkillUnlocked;

public record UseSkillPayload(int skillId, int finalTick, int chargeTicks) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<UseSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "use_skill"));

    public static final ResourceLocation SKILL_SLOT_DISPLAY_LAYER = ResourceLocation
            .fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_display");

    public static final StreamCodec<FriendlyByteBuf, UseSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            UseSkillPayload::skillId,
            ByteBufCodecs.INT,
            UseSkillPayload::finalTick,
            ByteBufCodecs.VAR_INT,
            UseSkillPayload::chargeTicks,
            UseSkillPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UseSkillPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("Received UseSkillPayload from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }
            if (msg.chargeTicks() < 0 || msg.chargeTicks() > DataDrivenSkill.MAX_CHARGE_TICKS) {
                LOGGER.warn("Player {} sent invalid skill charge duration {}.",
                        player.getName().getString(), msg.chargeTicks());
                return;
            }

            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId());
            if (skill == null) {
                LOGGER.warn("Skill with ID {} not found for player {}.", msg.skillId(), player.getName().getString());
                return;
            }

            boolean derived = SkillChains.isDerived(msg.skillId());
            // 派生先は元スキルからのコンボ時のみ受け付ける（チャージによる派生は chargeTicks > 0 で許可）
            if (derived && msg.chargeTicks() == 0 && !SkillChainTracker.canAdvance(player, msg.skillId())) {
                LOGGER.debug("Player {} tried to use derived skill {} outside of a combo window. Ignored.",
                        player.getName().getString(), msg.skillId());
                return;
            }

            if (SkillExecutionManager.isSkillActive(player)) {
                int runningSkillId = SkillExecutionManager.getRunningSkillId(player);
                SkillData runningSkill = SwordSkillRegistry.SKILLS.get(runningSkillId);

                boolean isCombo = runningSkill != null
                        && runningSkill.getType() == SkillData.SkillType.TRANSFORM
                        && SkillChains.next(runningSkillId) == msg.skillId();

                if (!isCombo) {
                    LOGGER.debug("Player {} tried to use skill {} while skill {} is active. Ignored.",
                            player.getName().getString(), msg.skillId(), runningSkillId);
                    return;
                }
            }

            if (isSkillUnlocked(player, msg.skillId()) || player.gameMode.isCreative()
                    || !UnlockedSkill.get()) {
                // SP チェック
                if (!player.gameMode.isCreative()) {
                    double currentSP = player.getPersistentData().getDouble("SS_CurrentSP");
                    double consumption = skill.getSpCost();
                    if (currentSP < consumption) {
                        LOGGER.debug("Player {} does not have enough SP to use skill {}. ({} < {})",
                                player.getName().getString(), skill.getName(), currentSP, consumption);
                        return;
                    }
                    // SP 消費
                    player.getPersistentData().putDouble("SS_CurrentSP", currentSP - consumption);
                    // 同期パケット送信
                    PacketDistributor.sendToPlayer(player, new SyncSPPayload(currentSP - consumption,
                            player.getAttributeValue(SwordSkillAttribute.MAX_SP)));
                }

                // コンボ成立を記録し、派生時はチャイム音を鳴らす
                SkillChainTracker.record(player, msg.skillId());
                if (derived) {
                    float pitch = 1.2F + 0.1F * SkillChains.depthOf(msg.skillId());
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, pitch);
                }

                if (msg.finalTick() == 0) {
                    executeSkill(player, skill, msg.chargeTicks());
                } else {
                    SkillExecutionManager.startSkillExecution(player, msg.skillId(), msg.finalTick(),
                            DataDrivenSkill.chargeMultiplier(msg.chargeTicks()));
                }
            } else {
                LOGGER.warn("Player {} attempted to use locked skill ID {}.", player.getName().getString(),
                        msg.skillId());
            }
        });
    }

    private static void executeSkill(ServerPlayer player, SkillData skill, int chargeTicks) {
        DataDrivenSkill.clearPlayerState(player.getUUID());
        DataDrivenSkill.setChargeMultiplier(player.getUUID(), DataDrivenSkill.chargeMultiplier(chargeTicks));
        try {
            ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
            skillInstance.execute(player.level(), player, 0, skill.getId());
            LOGGER.info("Executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(),
                    player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(),
                    player.getName().getString(), e);
        } finally {
            DataDrivenSkill.clearPlayerState(player.getUUID());
        }
    }
}