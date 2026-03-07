package com.zoma1101.swordskill.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider; // 追加
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.toClient.UnlockedSkillsResponsePacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import com.zoma1101.swordskill.network.toClient.SyncTrailConfigPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class SwordSkillCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("swordskillunlock")
                .requires(source -> source.hasPermission(2)) // OP権限レベル2以上
                .then(Commands.argument("targets", EntityArgument.players()) // ターゲット指定 (複数可)
                        .then(Commands.argument("skill_name", StringArgumentType.string()) // スキル名指定
                                .suggests(SUGGEST_SKILLS) // 入力補完
                                .executes(context -> unlockSkill(context, EntityArgument.getPlayers(context, "targets"),
                                        StringArgumentType.getString(context, "skill_name"))))));

        dispatcher.register(Commands.literal("swordskilltrail")
                .then(Commands.literal("on")
                        .executes(context -> toggleTrail(context, true)))
                .then(Commands.literal("off")
                        .executes(context -> toggleTrail(context, false))));
    }

    // スキル名の入力候補を提供する
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SKILLS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
                SwordSkillRegistry.SKILLS.values().stream().map(SkillData::getName),
                builder);
    };

    private static int unlockSkill(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String skillName) {
        // 名前からスキルデータを検索
        SkillData targetSkill = SwordSkillRegistry.SKILLS.values().stream()
                .filter(skill -> skill.getName().equals(skillName))
                .findFirst()
                .orElse(null);

        if (targetSkill == null) {
            context.getSource()
                    .sendFailure(Component.translatable("commands.swordskill.unlock.failed.not_found", skillName));
            return 0;
        }

        int skillId = targetSkill.getId();
        AtomicInteger successCount = new AtomicInteger(0); // ラムダ式内でカウントするためにAtomicIntegerを使用

        for (ServerPlayer player : targets) {
            // ★修正: DataManagerを使わずCapabilityを使用
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                // まだ習得していない場合のみ処理
                if (!skills.isSkillUnlocked(skillId)) {
                    skills.unlockSkill(skillId);

                    // --- クライアント同期 ---
                    // 解放済みスキルリストを取得して配列に変換
                    int[] skillArray = skills.getUnlockedSkills().stream().mapToInt(i -> i).toArray();

                    // クライアントへ同期パケットを送信
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new UnlockedSkillsResponsePacket(skillArray));

                    successCount.incrementAndGet();
                }
            });
        }

        if (successCount.get() > 0) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("commands.swordskill.unlock.success", skillName, successCount.get()),
                    true);
        } else {
            context.getSource().sendFailure(Component.translatable("commands.swordskill.unlock.failed.no_update"));
        }

        return successCount.get();
    }

    private static int toggleTrail(CommandContext<CommandSourceStack> context, boolean enable) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                skills.setTrailEnabled(enable);
                // 同期パケットの送信
                NetworkHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new SyncTrailConfigPacket(enable));

                String status = enable ? "ON" : "OFF";
                context.getSource().sendSuccess(() -> Component.literal("Sword trail is now " + status), true);
            });
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
    }
}
