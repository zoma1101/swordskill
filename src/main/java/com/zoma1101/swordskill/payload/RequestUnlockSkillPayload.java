package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.proficiency.ProficiencyManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 熟練度条件を満たしていればスキルを解放するパケット
public record RequestUnlockSkillPayload(int skillId) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<RequestUnlockSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "request_unlock_skill"));

    public static final StreamCodec<FriendlyByteBuf, RequestUnlockSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RequestUnlockSkillPayload::skillId,
            RequestUnlockSkillPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // サーバーサイドハンドラー
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }

            SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
            if (skill == null || !skill.isAssignable()) {
                LOGGER.warn("Player {} tried to unlock invalid skill {}", player.getName().getString(), skillId);
                return;
            }

            // 熟練度による解放条件（クリエイティブは免除）
            int requiredProficiency = ProficiencyManager.unlockRequirementOf(skill);
            SkillData.WeaponType category = ProficiencyManager.categoryOf(skill);
            int currentProficiency = ProficiencyManager.getProficiency(player, category);
            if (!player.isCreative() && requiredProficiency > 0 && currentProficiency < requiredProficiency) {
                LOGGER.warn("Player {} tried to unlock skill {} but proficiency {} is below {}",
                        player.getName().getString(), skillId, currentProficiency, requiredProficiency);
                player.displayClientMessage(Component.translatable(
                        "message." + SwordSkill.MOD_ID + ".unlock.required_proficiency",
                        Component.translatable(SwordSkill.MOD_ID + ".weapon." + category.name().toLowerCase()),
                        requiredProficiency, currentProficiency), false);
                return;
            }

            boolean newlyUnlocked;
            try {
                newlyUnlocked = SkillDataFetcher.unlockSkill(player, skillId);
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to save unlocked skill {} for player {}",
                        skillId, player.getName().getString(), exception);
                return;
            }

            // 解放状態をクライアントへ同期（画面のボタン表示も更新される）
            int[] unlockedSkills = SkillDataFetcher.getUnlockedSkills(player);
            List<Integer> skillList = Arrays.stream(unlockedSkills).boxed().collect(Collectors.toList());
            PacketDistributor.sendToPlayer(player, new SyncUnlockedSkillsPayload(skillList));

            Component skillName = Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName());
            player.displayClientMessage(Component.translatable(newlyUnlocked
                    ? "message." + SwordSkill.MOD_ID + ".unlock.success"
                    : "message." + SwordSkill.MOD_ID + ".unlock.already", skillName), !newlyUnlocked);

            if (newlyUnlocked) {
                LOGGER.info("Unlocked skill ID {} ({}) for player {}",
                        skillId, skill.getName(), player.getName().getString());
            }
        });
    }
}
