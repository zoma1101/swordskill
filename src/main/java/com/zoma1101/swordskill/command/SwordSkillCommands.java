package com.zoma1101.swordskill.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.payload.SyncUnlockedSkillsPayload;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = SwordSkill.MOD_ID)
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
                                .executes(context -> unlockSkill(context, EntityArgument.getPlayers(context, "targets"), StringArgumentType.getString(context, "skill_name")))
                        )
                )
        );

        dispatcher.register(Commands.literal("swordskill")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("level")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> levelGet(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(skillNameArgument()
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                                        .executes(context -> levelSet(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "skill_name"),
                                                                IntegerArgumentType.getInteger(context, "level")))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(skillNameArgument()
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                                        .executes(context -> levelAdd(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "skill_name"),
                                                                IntegerArgumentType.getInteger(context, "amount"))))))))
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> skillNameArgument() {
        return Commands.argument("skill_name", StringArgumentType.string()).suggests(SUGGEST_SKILLS);
    }

    private static int levelGet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        int maxLevel = SkillLevelManager.maxLevel();
        for (ServerPlayer player : targets) {
            Map<Integer, Integer> levels = SkillLevelManager.getStoredLevels(player);
            StringBuilder builder = new StringBuilder();
            levels.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        SkillData skill = SwordSkillRegistry.SKILLS.get(entry.getKey());
                        if (skill == null) {
                            return;
                        }
                        if (builder.length() > 0) {
                            builder.append(", ");
                        }
                        builder.append(skill.getName()).append(" Lv.").append(entry.getValue())
                                .append('/').append(maxLevel);
                    });
            String summary = builder.length() == 0 ? "-" : builder.toString();
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.level.get",
                    player.getName().getString(), summary), false);
        }
        return targets.size();
    }

    private static int levelSet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String skillName, int level) {
        SkillData targetSkill = findSkill(skillName);
        if (targetSkill == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.unlock.failed.not_found", skillName));
            return 0;
        }
        int applied = 0;
        for (ServerPlayer player : targets) {
            int result = SkillLevelManager.setLevel(player, targetSkill.getId(), level);
            PacketDistributor.sendToPlayer(player, SkillLevelManager.buildSyncPayload(player));
            applied++;
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.level.set",
                    player.getName().getString(), skillName, result,
                    formatMultiplier(SkillLevelManager.multiplierForLevel(result))), true);
        }
        return applied;
    }

    private static int levelAdd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String skillName, int amount) {
        SkillData targetSkill = findSkill(skillName);
        if (targetSkill == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.unlock.failed.not_found", skillName));
            return 0;
        }
        int applied = 0;
        for (ServerPlayer player : targets) {
            int result = SkillLevelManager.addLevel(player, targetSkill.getId(), amount);
            PacketDistributor.sendToPlayer(player, SkillLevelManager.buildSyncPayload(player));
            applied++;
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.level.set",
                    player.getName().getString(), skillName, result,
                    formatMultiplier(SkillLevelManager.multiplierForLevel(result))), true);
        }
        return applied;
    }

    private static SkillData findSkill(String skillName) {
        return SwordSkillRegistry.SKILLS.values().stream()
                .filter(skill -> skill.getName().equals(skillName))
                .findFirst()
                .orElse(null);
    }

    private static String formatMultiplier(double multiplier) {
        return String.format(java.util.Locale.ROOT, "%.2f", multiplier);
    }

    // スキル名の入力候補を提供する
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SKILLS = (context, builder) -> SharedSuggestionProvider.suggest(
            SwordSkillRegistry.SKILLS.values().stream().map(SkillData::getName),
            builder
    );

    private static int unlockSkill(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, String skillName) {
        // 名前からスキルデータを検索
        SkillData targetSkill = SwordSkillRegistry.SKILLS.values().stream()
                .filter(skill -> skill.getName().equals(skillName))
                .findFirst()
                .orElse(null);

        if (targetSkill == null) {
            // 変更: 翻訳キーを使用 (引数: スキル名)
            context.getSource().sendFailure(Component.translatable("commands.swordskill.unlock.failed.not_found", skillName));
            return 0;
        }

        int skillId = targetSkill.getId();
        int successCount = 0;

        for (ServerPlayer player : targets) {
            try {
                // --- データ保存処理 ---
                JsonObject playerData = DataManager.loadPlayerData(player);
                JsonArray unlockedSkillsArray;

                // 既存データの読み込みと形式チェック
                if (playerData.has("unlockedskill")) {
                    JsonElement element = playerData.get("unlockedskill");
                    if (element.isJsonArray()) {
                        unlockedSkillsArray = element.getAsJsonArray();
                    } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                        unlockedSkillsArray = new JsonArray();
                        unlockedSkillsArray.add(element.getAsInt());
                    } else {
                        unlockedSkillsArray = new JsonArray();
                    }
                } else {
                    unlockedSkillsArray = new JsonArray();
                }

                // 重複チェック
                boolean alreadyUnlocked = false;
                for (JsonElement element : unlockedSkillsArray) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && element.getAsInt() == skillId) {
                        alreadyUnlocked = true;
                        break;
                    }
                }

                if (!alreadyUnlocked) {
                    unlockedSkillsArray.add(skillId);
                    playerData.add("unlockedskill", unlockedSkillsArray);
                    DataManager.savePlayerData(player, playerData);

                    // --- クライアント同期 ---
                    int[] unlockedSkills = SkillDataFetcher.getUnlockedSkills(player);
                    List<Integer> skillList = Arrays.stream(unlockedSkills).boxed().collect(Collectors.toList());
                    PacketDistributor.sendToPlayer(player, new SyncUnlockedSkillsPayload(skillList));

                    successCount++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (successCount > 0) {
            // 変更: 翻訳キーを使用 (引数: スキル名, プレイヤー数)
            int finalSuccessCount = successCount;
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.unlock.success", skillName, finalSuccessCount), true);
        } else {
            // 変更: 翻訳キーを使用
            context.getSource().sendFailure(Component.translatable("commands.swordskill.unlock.failed.no_update"));
        }

        return successCount;
    }
}