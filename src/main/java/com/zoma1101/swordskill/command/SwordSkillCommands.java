package com.zoma1101.swordskill.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import com.zoma1101.swordskill.payload.SyncUnlockedSkillsPayload;
import com.zoma1101.swordskill.proficiency.ProficiencyManager;
import com.zoma1101.swordskill.playerlevel.PlayerLevelManager;
import com.zoma1101.swordskill.playerlevel.EnemyLevel;
import com.zoma1101.swordskill.playerlevel.StatType;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
                .then(Commands.literal("mannequin")
                        .executes(context -> spawnMannequin(context, 1.0F, ""))
                        .then(Commands.literal("skill")
                                .then(skillNameArgument()
                                        .executes(context -> setMannequinSkill(context,
                                                StringArgumentType.getString(context, "skill_name")))))
                        .then(Commands.argument("clash_ratio", DoubleArgumentType.doubleArg(0.0, 4.0))
                                .executes(context -> spawnMannequin(context,
                                        (float) DoubleArgumentType.getDouble(context, "clash_ratio"), ""))
                                .then(skillNameArgument()
                                        .executes(context -> spawnMannequin(context,
                                                (float) DoubleArgumentType.getDouble(context, "clash_ratio"),
                                                StringArgumentType.getString(context, "skill_name"))))))
                .then(Commands.literal("proficiency")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> proficiencyGet(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(categoryArgument()
                                                .then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(0, 1000000))
                                                        .executes(context -> proficiencySet(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "category"),
                                                                IntegerArgumentType.getInteger(context,
                                                                        "amount")))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(categoryArgument()
                                                .then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(-1000000, 1000000))
                                                        .executes(context -> proficiencyAdd(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "category"),
                                                                IntegerArgumentType.getInteger(context,
                                                                        "amount"))))))))
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
                .then(Commands.literal("playerlevel")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> playerLevelGet(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 10000))
                                                .executes(context -> playerLevelSet(context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "level"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000))
                                                .executes(context -> playerLevelAdd(context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount",
                                                IntegerArgumentType.integer(0, 1000000000))
                                                .executes(context -> playerLevelXp(context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> statGet(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(statArgument()
                                                .then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(0, 1000000))
                                                        .executes(context -> statSet(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "stat"),
                                                                IntegerArgumentType.getInteger(context, "amount")))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(statArgument()
                                                .then(Commands.argument("amount",
                                                        IntegerArgumentType.integer(-1000000, 1000000))
                                                        .executes(context -> statAdd(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "stat"),
                                                                IntegerArgumentType.getInteger(context, "amount"))))))))
                .then(Commands.literal("enemylevel")
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(context -> enemyLevelGet(context,
                                                EntityArgument.getEntities(context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 10000))
                                                .executes(context -> enemyLevelSet(context,
                                                        EntityArgument.getEntities(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "level")))))))
                .then(Commands.literal("forge")
                        .requires(source -> com.zoma1101.swordskill.SwordSkillFeatures.SMITHING)
                        .then(Commands.literal("create")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("pattern", StringArgumentType.string())
                                                .suggests(SUGGEST_PATTERNS)
                                                .executes(context -> forgeCreate(context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        StringArgumentType.getString(context, "pattern"),
                                                        "iron"))
                                                .then(Commands.argument("material", StringArgumentType.string())
                                                        .suggests(SUGGEST_MATERIALS)
                                                        .executes(context -> forgeCreate(context,
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "pattern"),
                                                                StringArgumentType.getString(context, "material")))))))
                        .then(Commands.literal("memory")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> forgeMemory(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("enhance")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> forgeEnhance(context,
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("melt")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> forgeMelt(context,
                                                EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("defs")
                                .executes(SwordSkillCommands::forgeDefs))
        ));
    }

    // --- 鍛冶コマンド ---------------------------------------------------

    private static ResourceLocation smithingId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.equals("-") || value.equalsIgnoreCase("none")
                || value.equalsIgnoreCase("null")) {
            return null;
        }
        if (!value.contains(":")) {
            value = SwordSkill.MOD_ID + ":" + value;
        }
        return ResourceLocation.tryParse(value);
    }

    /**
     * 鍔迫り合いの実装テスト用マネキンを、実行者（コマンドブロックならその位置）の前に出す。
     *
     * @param clashRatio 押し込み力の倍率。スキル未設定なら 1.0 で相手と同等（鍔迫り合い）、
     *                   0 で鍔迫り合い無効。
     * @param skillName  常時使用させるソードスキル名。空文字・"none" で未設定。
     */
    private static int spawnMannequin(CommandContext<CommandSourceStack> context, float clashRatio,
            String skillName) {
        CommandSourceStack source = context.getSource();
        SkillData skill = resolveMannequinSkill(skillName);
        if (skill == null && !isNoSkill(skillName)) {
            source.sendFailure(Component.translatable("commands.swordskill.mannequin.unknown_skill", skillName));
            return 0;
        }
        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition();
        Vec3 look = source.getEntity() != null
                ? source.getEntity().getLookAngle()
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        SkillMannequinEntity mannequin = SwordSkill_Entities.SKILL_MANNEQUIN.get().create(level);
        if (mannequin == null) {
            source.sendFailure(Component.translatable("commands.swordskill.mannequin.failed"));
            return 0;
        }
        double x = position.x + horizontal.x * 2.0D;
        double z = position.z + horizontal.z * 2.0D;
        mannequin.moveTo(x, position.y, z, source.getRotation().y + 180.0F, 0.0F);
        mannequin.setClashRatio(clashRatio);
        mannequin.setSkillId(skill == null ? "" : skill.getName());
        mannequin.setPersistenceRequired();
        level.addFreshEntity(mannequin);
        source.sendSuccess(() -> Component.translatable("commands.swordskill.mannequin.spawned",
                String.format("%.2f", clashRatio)), false);
        return 1;
    }

    /**
     * 16 ブロック以内で最も近いマネキンの常時使用スキルを設定する。
     * {@code none} を渡すと解除。
     */
    private static int setMannequinSkill(CommandContext<CommandSourceStack> context, String skillName) {
        CommandSourceStack source = context.getSource();
        SkillData skill = resolveMannequinSkill(skillName);
        if (skill == null && !isNoSkill(skillName)) {
            source.sendFailure(Component.translatable("commands.swordskill.mannequin.unknown_skill", skillName));
            return 0;
        }
        Vec3 position = source.getPosition();
        AABB area = new AABB(position, position).inflate(16.0D);
        SkillMannequinEntity nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (SkillMannequinEntity mannequin : source.getLevel()
                .getEntitiesOfClass(SkillMannequinEntity.class, area)) {
            double distance = mannequin.position().distanceToSqr(position);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = mannequin;
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.translatable("commands.swordskill.mannequin.not_found"));
            return 0;
        }
        nearest.setSkillId(skill == null ? "" : skill.getName());
        if (skill == null) {
            source.sendSuccess(() -> Component.translatable(
                    "commands.swordskill.mannequin.skill_cleared"), false);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    "commands.swordskill.mannequin.skill_set", skill.getName()), false);
        }
        return 1;
    }

    private static SkillData resolveMannequinSkill(String skillName) {
        return isNoSkill(skillName) ? null : findSkill(skillName);
    }

    private static boolean isNoSkill(String skillName) {
        return skillName == null || skillName.isBlank() || skillName.equalsIgnoreCase("none");
    }

    private static int forgeCreate(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String patternArg, String materialArg) {
        com.zoma1101.swordskill.smithing.SmithingMaterial material = com.zoma1101.swordskill.smithing.SmithingDefinitions
                .material(smithingId(materialArg));
        com.zoma1101.swordskill.smithing.WeaponPattern pattern = com.zoma1101.swordskill.smithing.SmithingDefinitions
                .pattern(smithingId(patternArg));
        if (material == null || pattern == null) {
            context.getSource().sendFailure(Component.translatable("commands.swordskill.forge.unknown_def"));
            return 0;
        }
        int count = 0;
        for (ServerPlayer player : targets) {
            net.minecraft.world.item.ItemStack result = com.zoma1101.swordskill.smithing.SmithingForge.forge(player,
                    pattern, material, material, material, java.util.List.of());
            if (result.isEmpty()) {
                continue;
            }
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            count++;
        }
        final int forged = count;
        context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.forge.create", forged),
                true);
        return count;
    }

    private static String percent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value * 100.0D);
    }

    private static int forgeMemory(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer player : targets) {
            com.zoma1101.swordskill.smithing.ForgedWeaponData data = com.zoma1101.swordskill.smithing.ForgedWeaponData
                    .get(player.getMainHandItem());
            if (data == null) {
                context.getSource().sendFailure(Component.translatable("commands.swordskill.forge.not_forged",
                        player.getName()));
                continue;
            }
            com.zoma1101.swordskill.smithing.WeaponMemory memory = data.memory();
            com.zoma1101.swordskill.smithing.HiddenParams params = com.zoma1101.swordskill.smithing.SmithingCombat
                    .hiddenParams(memory);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.forge.memory",
                    player.getName(), memory.hits(), memory.thrustHits(), memory.sweepHits(), memory.flyingHits(),
                    memory.critHits(), memory.shortCooldownHits(), (int) memory.damageDealt(),
                    (int) memory.damageTaken(), memory.kills()), false);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.forge.hidden",
                    percent(params.thrustBonus()), percent(params.sweepBonus()), percent(params.flyingBonus()),
                    percent(params.criticalDamageBonus()), percent(params.cooldownReduction())), false);
            count++;
        }
        return count;
    }

    private static int forgeEnhance(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            int amount) {
        int count = 0;
        for (ServerPlayer player : targets) {
            net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
            int applied = 0;
            for (int i = 0; i < amount; i++) {
                if (!com.zoma1101.swordskill.smithing.SmithingForge.enhance(player, stack)) {
                    break;
                }
                applied++;
            }
            if (applied == 0) {
                context.getSource().sendFailure(Component.translatable("commands.swordskill.forge.enhance_failed",
                        player.getName()));
                continue;
            }
            final int appliedFinal = applied;
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.forge.enhance",
                    player.getName(), appliedFinal), true);
            count++;
        }
        return count;
    }

    private static int forgeMelt(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        int count = 0;
        for (ServerPlayer player : targets) {
            net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
            net.minecraft.world.item.ItemStack ingot = com.zoma1101.swordskill.smithing.SmithingForge.melt(stack);
            if (ingot.isEmpty()) {
                context.getSource().sendFailure(Component.translatable("commands.swordskill.forge.not_forged",
                        player.getName()));
                continue;
            }
            if (!player.getInventory().add(ingot)) {
                player.drop(ingot, false);
            }
            stack.shrink(1);
            count++;
        }
        final int melted = count;
        context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.forge.melt", melted), true);
        return count;
    }

    /** 素材・部品・武器の形の一覧をログ用に出す。 */
    private static int forgeDefs(CommandContext<CommandSourceStack> context) {
        String materials = com.zoma1101.swordskill.smithing.SmithingDefinitions.materials().stream()
                .map(material -> material.id() + " (tier " + material.tier() + ")")
                .sorted()
                .collect(Collectors.joining(", "));
        String parts = com.zoma1101.swordskill.smithing.SmithingDefinitions.parts().stream()
                .map(part -> part.id() + " [" + part.kind() + "]")
                .sorted()
                .collect(Collectors.joining(", "));
        String patterns = com.zoma1101.swordskill.smithing.SmithingDefinitions.patterns().stream()
                .map(pattern -> pattern.id() + " -> " + pattern.weapon())
                .sorted()
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal("materials: " + materials), false);
        context.getSource().sendSuccess(() -> Component.literal("parts: " + parts), false);
        context.getSource().sendSuccess(() -> Component.literal("patterns: " + patterns), false);
        return 1;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> statArgument() {
        return Commands.argument("stat", StringArgumentType.string()).suggests(SUGGEST_STATS);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> skillNameArgument() {
        return Commands.argument("skill_name", StringArgumentType.string()).suggests(SUGGEST_SKILLS);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> categoryArgument() {
        return Commands.argument("category", StringArgumentType.string()).suggests(SUGGEST_CATEGORIES);
    }

    // --- 熟練度コマンド -------------------------------------------------

    private static SkillData.WeaponType findCategory(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            SkillData.WeaponType category = SkillData.WeaponType
                    .valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
            return category == SkillData.WeaponType.NONE ? null : category;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String formatProficiencyList(ServerPlayer player) {
        StringBuilder builder = new StringBuilder();
        for (SkillData.WeaponType category : SkillData.WeaponType.values()) {
            if (category == SkillData.WeaponType.NONE) {
                continue;
            }
            int value = ProficiencyManager.getProficiency(player, category);
            if (value <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(category.name()).append(" ").append(value)
                    .append(" (+").append(ProficiencyManager.getAvailablePoints(player, category)).append(')');
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private static int proficiencyGet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            String summary = formatProficiencyList(player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.proficiency.get",
                    player.getName().getString(), summary), false);
        }
        return targets.size();
    }

    private static int proficiencySet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String categoryName, int amount) {
        SkillData.WeaponType category = findCategory(categoryName);
        if (category == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.proficiency.not_found", categoryName));
            return 0;
        }
        for (ServerPlayer player : targets) {
            int result = ProficiencyManager.setProficiency(player, category, amount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.proficiency.set",
                    player.getName().getString(), category.name(), result,
                    ProficiencyManager.getAvailablePoints(player, category)), true);
        }
        return targets.size();
    }

    private static int proficiencyAdd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String categoryName, int amount) {
        SkillData.WeaponType category = findCategory(categoryName);
        if (category == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.proficiency.not_found", categoryName));
            return 0;
        }
        for (ServerPlayer player : targets) {
            int result = ProficiencyManager.addProficiency(player, category, amount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.proficiency.set",
                    player.getName().getString(), category.name(), result,
                    ProficiencyManager.getAvailablePoints(player, category)), true);
        }
        return targets.size();
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

    // --- プレイヤーレベルコマンド ---------------------------------------

    private static int playerLevelGet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            int level = PlayerLevelManager.getLevel(player);
            long xp = PlayerLevelManager.getXp(player);
            long next = PlayerLevelManager.getXpToNext(player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.playerlevel.get",
                    player.getName().getString(), level, PlayerLevelManager.maxLevel(), xp, next), false);
        }
        return targets.size();
    }

    private static int playerLevelSet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            int level) {
        for (ServerPlayer player : targets) {
            int result = PlayerLevelManager.setLevel(player, level);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.playerlevel.set",
                    player.getName().getString(), result), true);
        }
        return targets.size();
    }

    private static int playerLevelAdd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            int amount) {
        for (ServerPlayer player : targets) {
            int result = PlayerLevelManager.addLevel(player, amount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.playerlevel.set",
                    player.getName().getString(), result), true);
        }
        return targets.size();
    }

    private static int playerLevelXp(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            int amount) {
        for (ServerPlayer player : targets) {
            int gained = PlayerLevelManager.addXp(player, amount);
            int level = PlayerLevelManager.getLevel(player);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.playerlevel.xp",
                    player.getName().getString(), amount, gained, level), true);
        }
        return targets.size();
    }

    // --- ステータス／敵レベルコマンド -----------------------------------

    private static int statGet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            String summary = "STR " + PlayerLevelManager.getStat(player, StatType.STR)
                    + ", AGI " + PlayerLevelManager.getStat(player, StatType.AGI)
                    + " (" + PlayerLevelManager.getAvailablePoints(player) + " free)";
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.stat.get",
                    player.getName().getString(), summary), false);
        }
        return targets.size();
    }

    private static int statSet(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String statName, int amount) {
        StatType type = StatType.byName(statName);
        if (type == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.stat.not_found", statName));
            return 0;
        }
        for (ServerPlayer player : targets) {
            int result = PlayerLevelManager.setStat(player, type, amount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.stat.set",
                    player.getName().getString(), type.name(), result), true);
        }
        return targets.size();
    }

    private static int statAdd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets,
            String statName, int amount) {
        StatType type = StatType.byName(statName);
        if (type == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.swordskill.stat.not_found", statName));
            return 0;
        }
        for (ServerPlayer player : targets) {
            int result = PlayerLevelManager.setStat(player, type,
                    PlayerLevelManager.getStat(player, type) + amount);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.stat.set",
                    player.getName().getString(), type.name(), result), true);
        }
        return targets.size();
    }

    private static int enemyLevelGet(CommandContext<CommandSourceStack> context,
            Collection<? extends net.minecraft.world.entity.Entity> targets) {
        for (net.minecraft.world.entity.Entity entity : targets) {
            int level = EnemyLevel.get(entity);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.enemylevel.get",
                    entity.getDisplayName().getString(), level), false);
        }
        return targets.size();
    }

    private static int enemyLevelSet(CommandContext<CommandSourceStack> context,
            Collection<? extends net.minecraft.world.entity.Entity> targets, int level) {
        for (net.minecraft.world.entity.Entity entity : targets) {
            EnemyLevel.set(entity, level);
            context.getSource().sendSuccess(() -> Component.translatable("commands.swordskill.enemylevel.set",
                    entity.getDisplayName().getString(), level), true);
        }
        return targets.size();
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

    // 熟練度カテゴリ（武器種）の入力候補を提供する
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CATEGORIES = (context, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(SkillData.WeaponType.values())
                    .filter(type -> type != SkillData.WeaponType.NONE)
                    .map(Enum::name),
            builder
    );

    // ステータス名の入力候補を提供する
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_STATS = (context, builder) -> SharedSuggestionProvider.suggest(
            Arrays.stream(StatType.values()).map(Enum::name),
            builder
    );

    // 鍛冶素材の入力候補
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MATERIALS = (context, builder) -> SharedSuggestionProvider.suggest(
            com.zoma1101.swordskill.smithing.SmithingDefinitions.materials().stream()
                    .map(material -> material.id().toString())
                    .sorted()
                    .collect(Collectors.toList()),
            builder
    );

    /** 武器の形の入力候補。 */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PATTERNS = (context, builder) -> SharedSuggestionProvider.suggest(
            com.zoma1101.swordskill.smithing.SmithingDefinitions.patterns().stream()
                    .map(pattern -> pattern.id().toString())
                    .sorted()
                    .collect(Collectors.toList()),
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
                    // 直接書き換えたのでキャッシュを捨てる
                    SkillDataFetcher.invalidate(player);

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