package com.zoma1101.swordskill.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.payload.SyncUnlockedSkillsPayload;
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