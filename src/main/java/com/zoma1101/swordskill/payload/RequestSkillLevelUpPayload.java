package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.proficiency.ProficiencyManager;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 選択画面から「熟練度ポイントでスキルをレベルアップ」するリクエスト。
 * 実際に消費できるかはサーバー側で検証する。
 */
public record RequestSkillLevelUpPayload(int skillId) implements CustomPacketPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<RequestSkillLevelUpPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "request_skill_level_up"));

    public static final StreamCodec<FriendlyByteBuf, RequestSkillLevelUpPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RequestSkillLevelUpPayload::skillId,
            RequestSkillLevelUpPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
            ProficiencyManager.LevelUpResult result = ProficiencyManager.levelUpSkill(player, skill);
            if (result == ProficiencyManager.LevelUpResult.SUCCESS) {
                PacketDistributor.sendToPlayer(player, SkillLevelManager.buildSyncPayload(player));
                ProficiencyManager.syncToClient(player);
                int level = SkillLevelManager.getLevel(player, skillId);
                player.displayClientMessage(Component.translatable(
                        "message." + SwordSkill.MOD_ID + ".level_up.success",
                        Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName()),
                        level,
                        String.format(java.util.Locale.ROOT, "%.2f",
                                SkillLevelManager.multiplierForLevel(level))), true);
                LOGGER.debug("Levelled up skill {} to {} for player {}", skillId, level,
                        player.getName().getString());
                return;
            }
            LOGGER.warn("Rejected skill level-up request for skill {} from player {} ({})", skillId,
                    player.getName().getString(), result);
            String key = switch (result) {
                case NO_POINT -> "level_up.failed.no_point";
                case MAX_LEVEL -> "level_up.failed.max_level";
                case NOT_UNLOCKED -> "level_up.failed.not_unlocked";
                default -> "level_up.failed";
            };
            player.displayClientMessage(Component.translatable("message." + SwordSkill.MOD_ID + "." + key), true);
        });
    }
}
