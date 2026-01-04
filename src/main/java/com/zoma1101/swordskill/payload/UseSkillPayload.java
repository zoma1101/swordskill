package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static com.zoma1101.swordskill.data.SkillDataFetcher.isSkillUnlocked;


public record UseSkillPayload(int skillId, int finalTick) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<UseSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "use_skill"));

    public static final StreamCodec<FriendlyByteBuf, UseSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            UseSkillPayload::skillId,
            ByteBufCodecs.INT,
            UseSkillPayload::finalTick,
            UseSkillPayload::new
    );

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

            if (SkillExecutionManager.isSkillActive(player)) {
                int runningSkillId = SkillExecutionManager.getRunningSkillId(player);
                SkillData runningSkill = SwordSkillRegistry.SKILLS.get(runningSkillId);
                SkillData requestedSkill = SwordSkillRegistry.SKILLS.get(msg.skillId());


                boolean isCombo = runningSkill != null && requestedSkill != null &&
                        runningSkill.getType() == SkillData.SkillType.TRANSFORM &&
                        requestedSkill.getType() == SkillData.SkillType.TRANSFORM_FINISH;

                if (!isCombo) {
                    LOGGER.debug("Player {} tried to use skill {} while skill {} is active. Ignored.", player.getName().getString(), msg.skillId(), runningSkillId);
                    return;
                }
            }

            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId());
            if (skill != null && (isSkillUnlocked(player, msg.skillId()) || player.gameMode.isCreative() || !UnlockedSkill.get())) {
                if (msg.finalTick() == 0) {
                    executeSkill(player, skill);
                } else {
                    SkillExecutionManager.startSkillExecution(player, msg.skillId(), msg.finalTick());
                }
            } else if (skill == null) {
                LOGGER.warn("Skill with ID {} not found for player {}.", msg.skillId(), player.getName().getString());
            } else {
                LOGGER.warn("Player {} attempted to use locked skill ID {}.", player.getName().getString(), msg.skillId());
            }
        });
    }

    private static void executeSkill(ServerPlayer player, SkillData skill) {
        try {
            ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
            skillInstance.execute(player.level(), player, 0, skill.getId());
            LOGGER.info("Executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(), player.getName().getString(), e);
        }
    }
}