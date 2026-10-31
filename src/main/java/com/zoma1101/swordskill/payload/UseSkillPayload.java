package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;

import com.zoma1101.swordskill.server.handler.CounterManager;
import com.zoma1101.swordskill.server.handler.SkillClashManager;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.swordskills.SkillChains;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
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

public record UseSkillPayload(int skillId, int chargeTicks) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<UseSkillPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "use_skill"));

    public static final ResourceLocation SKILL_SLOT_DISPLAY_LAYER = ResourceLocation
            .fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_display");

    /**
     * スキルIDとチャージ tick のみを送る。
     *
     * <p>実行時間（finalTick）は以前クライアントから受け取っていたが、値は常にサーバー側の
     * {@link SkillData#getFinalTick()} と同じため、サーバーで解決するようにした。
     * ペイロードが軽くなるうえ、クライアントが実行時間を偽装できなくなる。</p>
     */
    public static final StreamCodec<FriendlyByteBuf, UseSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            UseSkillPayload::skillId,
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
            // 鍔迫り合い中は新しいスキルを受け付けない
            if (SkillClashManager.isClashing(player.getUUID())) {
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

            // 爪スキルは両手に爪を持たない限り使用できない（サーバー側でも検証）
            if (skill.getAvailableWeaponTypes().contains(SkillData.WeaponType.CLAW)
                    && !WeaponTypeUtils.getWeaponType(player).contains(SkillData.WeaponType.CLAW)) {
                LOGGER.debug("Player {} tried to use claw skill {} without dual claw. Ignored.",
                        player.getName().getString(), msg.skillId());
                return;
            }

            // チャージ不可スキルはチャージ時間を無視する
            boolean chargeable = skill.isChargeable();
            int chargeTicks = chargeable ? msg.chargeTicks() : 0;

            boolean derived = SkillChains.isDerived(msg.skillId());
            // 派生先はチャージによる派生（chargeTicks > 0）時のみ受け付ける
            if (derived && chargeTicks == 0) {
                LOGGER.debug("Player {} tried to use derived skill {} without charge. Ignored.",
                        player.getName().getString(), msg.skillId());
                return;
            }

            if (SkillExecutionManager.isSkillActive(player)) {
                int runningSkillId = SkillExecutionManager.getRunningSkillId(player);
                SkillData runningSkill = SwordSkillRegistry.SKILLS.get(runningSkillId);

                boolean isCombo = false;

                if (!isCombo) {
                    LOGGER.debug("Player {} tried to use skill {} while skill {} is active. Ignored.",
                            player.getName().getString(), msg.skillId(), runningSkillId);
                    return;
                }
            }

            // 解放チェックは「解放機能が有効なときだけ」評価する。
            // 以前は isSkillUnlocked を先に評価していたため、解放を無効にしていても
            // 発動のたびにプレイヤーデータを読み込んでいた。
            if (player.gameMode.isCreative() || !UnlockedSkill.get()
                    || isSkillUnlocked(player, msg.skillId())) {
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

                // 派生時はチャイム音を鳴らす
                if (derived) {
                    float pitch = 1.2F + 0.1F * SkillChains.depthOf(msg.skillId());
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, pitch);
                }

                // 実行時間はサーバー側の定義から決める（クライアントの申告値は使わない）
                int finalTick = skill.getFinalTick();
                // カウンタースキルはチャージ中に受けたダメージに応じて威力を上乗せする
                float powerMultiplier = chargeable ? DataDrivenSkill.chargeMultiplier(chargeTicks) : 1.0F;
                if (skill.isCounter()) {
                    powerMultiplier *= CounterManager.consumeMultiplier(player.getUUID());
                }
                if (finalTick <= 0) {
                    executeSkill(player, skill, powerMultiplier);
                } else {
                    SkillExecutionManager.startSkillExecution(player, msg.skillId(), finalTick,
                            powerMultiplier);
                }
            } else {
                LOGGER.warn("Player {} attempted to use locked skill ID {}.", player.getName().getString(),
                        msg.skillId());
            }
        });
    }

    private static void executeSkill(ServerPlayer player, SkillData skill, float powerMultiplier) {
        DataDrivenSkill.clearPlayerState(player.getUUID());
        DataDrivenSkill.setChargeMultiplier(player.getUUID(), powerMultiplier);
        try {
            ISkill skillInstance = SkillExecutionManager.skillInstance(skill);
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