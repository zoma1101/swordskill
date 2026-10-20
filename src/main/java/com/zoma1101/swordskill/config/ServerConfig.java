package com.zoma1101.swordskill.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue damageMultiplier;
    public static final ModConfigSpec.DoubleValue cooldownMultiplier;
    public static final ModConfigSpec.DoubleValue defaultMaxSp;
    public static final ModConfigSpec.DoubleValue defaultSpRegen;
    public static final ModConfigSpec.BooleanValue PvP;
    public static final ModConfigSpec.BooleanValue AUTOWEAPON_SETTING;
    public static final ModConfigSpec.BooleanValue UnlockedSkill;
    public static final ModConfigSpec.BooleanValue attackNeutralMobs;
    public static final ModConfigSpec.IntValue hitstopTicks;
    public static final ModConfigSpec.DoubleValue hitstopShakeStrength;
    public static final ModConfigSpec.DoubleValue hitstopFovPunch;
    public static final ModConfigSpec.IntValue skillLevelMax;
    public static final ModConfigSpec.DoubleValue skillLevelMinMultiplier;
    public static final ModConfigSpec.DoubleValue skillLevelMaxMultiplier;
    public static final ModConfigSpec.IntValue proficiencyPerLevelUp;
    public static final ModConfigSpec.IntValue proficiencyPerKill;
    public static final ModConfigSpec.BooleanValue proficiencyFromMonstersOnly;

    public static final ModConfigSpec.BooleanValue levelSystemEnabled;
    public static final ModConfigSpec.IntValue playerLevelMax;
    public static final ModConfigSpec.DoubleValue levelXpBase;
    public static final ModConfigSpec.DoubleValue levelXpExponent;
    public static final ModConfigSpec.IntValue statPointsPerLevel;
    public static final ModConfigSpec.DoubleValue levelSpPerLevel;
    public static final ModConfigSpec.DoubleValue levelSpBonusCap;
    public static final ModConfigSpec.IntValue levelXpPerKill;
    public static final ModConfigSpec.BooleanValue levelXpFromMonstersOnly;

    public static final ModConfigSpec.DoubleValue strDamagePerPoint;
    public static final ModConfigSpec.DoubleValue strMaxBonus;
    public static final ModConfigSpec.DoubleValue agilityMoveSpeedPerPoint;
    public static final ModConfigSpec.DoubleValue agilityMaxMoveSpeedBonus;
    public static final ModConfigSpec.BooleanValue levelDiffEnabled;
    public static final ModConfigSpec.DoubleValue levelDiffPerPoint;
    public static final ModConfigSpec.IntValue levelDiffCap;

    public static final ModConfigSpec.BooleanValue smithingEnabled;
    public static final ModConfigSpec.DoubleValue forgeBaseDamage;
    public static final ModConfigSpec.DoubleValue forgeMaterialPowerScale;
    public static final ModConfigSpec.DoubleValue forgeQualityDamageBonus;
    public static final ModConfigSpec.DoubleValue forgeEnhanceDamageBonus;
    public static final ModConfigSpec.IntValue forgeEnhanceMax;
    public static final ModConfigSpec.DoubleValue strPerWeight;
    public static final ModConfigSpec.DoubleValue overweightAttackSpeedPenalty;
    public static final ModConfigSpec.DoubleValue overweightSkillSpeedMultiplier;
    public static final ModConfigSpec.IntValue creationMaterialCost;
    public static final ModConfigSpec.DoubleValue enhancementCostRatio;
    public static final ModConfigSpec.DoubleValue qualityProficiencyPerPoint;
    public static final ModConfigSpec.IntValue qualityMax;
    public static final ModConfigSpec.DoubleValue reinforcementPowerRatio;

    public static final ModConfigSpec.DoubleValue critBaseChance;
    public static final ModConfigSpec.DoubleValue critBaseMultiplier;
    public static final ModConfigSpec.IntValue shortCooldownTicks;
    public static final ModConfigSpec.DoubleValue memoryDamageBonusPerHit;
    public static final ModConfigSpec.DoubleValue memoryDamageBonusCap;
    public static final ModConfigSpec.DoubleValue memoryCritDamagePerHit;
    public static final ModConfigSpec.DoubleValue memoryCritDamageCap;
    public static final ModConfigSpec.DoubleValue memoryCooldownReductionPerHit;
    public static final ModConfigSpec.DoubleValue memoryCooldownReductionCap;

    static {
        BUILDER.push("Damage Settings");
        damageMultiplier = BUILDER.comment("Sword skill damage multiplier").defineInRange("damageMultiplier", 1.0, 0.1,
                1000.0);
        cooldownMultiplier = BUILDER.comment("Sword skill cool down multiplier").defineInRange("CoolDownMultiplier",
                1.0, 0.01, 10.0);
        AUTOWEAPON_SETTING = BUILDER.comment("The available weapons are set automatically to some extent.")
                .define("AutoWeaponSetting", false);
        PvP = BUILDER.comment("Allow PVP with sword skills").define("pvp", false);
        UnlockedSkill = BUILDER.comment("Enable Sword Skill Unlock").define("UnlockedSkill", true);
        attackNeutralMobs = BUILDER.comment("Include neutral mobs as attack targets").define("attackNeutralMobs", true);
        BUILDER.pop();

        BUILDER.push("Hitstop Settings");
        hitstopTicks = BUILDER.comment(
                "Default hit-stop duration (ticks) applied when a sword skill hit lands. 0 disables hit-stop. "
                        + "A skill can override this with the hitstop_ticks field in its skilldata.")
                .defineInRange("hitstopTicks", 3, 0, 40);
        hitstopShakeStrength = BUILDER.comment("Camera shake strength during hit-stop. 0 disables the screen shake.")
                .defineInRange("hitstopShakeStrength", 1.0, 0.0, 4.0);
        hitstopFovPunch = BUILDER.comment(
                "FOV punch (short zoom-in) strength shown to the attacker during hit-stop. 0 disables the zoom.")
                .defineInRange("hitstopFovPunch", 1.0, 0.0, 4.0);
        BUILDER.pop();

        BUILDER.push("Skill Level Settings");
        skillLevelMax = BUILDER.comment(
                "Maximum sword-skill level. Levels run from 1 to this value (per skill, per player).")
                .defineInRange("skillLevelMax", 10, 1, 100);
        skillLevelMinMultiplier = BUILDER.comment(
                "Damage multiplier applied to a level-1 skill. The multiplier grows linearly up to "
                        + "skillLevelMaxMultiplier at the maximum level.")
                .defineInRange("skillLevelMinMultiplier", 1.0, 0.01, 100.0);
        skillLevelMaxMultiplier = BUILDER.comment(
                "Damage multiplier applied to a skill at the maximum level.")
                .defineInRange("skillLevelMaxMultiplier", 3.0, 0.01, 100.0);
        BUILDER.pop();

        BUILDER.push("Proficiency Settings");
        proficiencyPerLevelUp = BUILDER.comment(
                "Proficiency needed per level-up point. Every time a category (weapon type) gains this much "
                        + "proficiency, the player can raise one skill of that category by one level.")
                .defineInRange("proficiencyPerLevelUp", 100, 1, 1000000);
        proficiencyPerKill = BUILDER.comment(
                "Proficiency gained in the running skill's category when a monster is defeated with a sword skill. "
                        + "0 disables gaining proficiency from kills.")
                .defineInRange("proficiencyPerKill", 1, 0, 10000);
        proficiencyFromMonstersOnly = BUILDER.comment(
                "Only hostile monsters (MobCategory.MONSTER) grant proficiency when defeated. "
                        + "Set to false to also gain proficiency from animals and other mobs.")
                .define("proficiencyFromMonstersOnly", true);
        BUILDER.pop();

        BUILDER.push("Player Level Settings");
        levelSystemEnabled = BUILDER.comment(
                "Enable the player level system. Level grants stat points (STR/AGI), maximum SP and "
                        + "progression gates. Level itself never adds raw HP or damage; STR/AGI do.",
                "Disabled by default for the first release; the status screen is also disabled.")
                .define("levelSystemEnabled", false);
        playerLevelMax = BUILDER.comment("Maximum player level.")
                .defineInRange("playerLevelMax", 100, 1, 10000);
        levelXpBase = BUILDER.comment(
                "XP required to advance from level 1 to level 2. The cost of leaving level L is "
                        + "levelXpBase * L^levelXpExponent.")
                .defineInRange("levelXpBase", 100.0, 1.0, 10000000.0);
        levelXpExponent = BUILDER.comment("Exponent of the XP curve. 1.0 makes every level cost levelXpBase * L.")
                .defineInRange("levelXpExponent", 1.25, 1.0, 3.0);
        statPointsPerLevel = BUILDER.comment("Stat points gained per level-up, to spend on STR / AGI.")
                .defineInRange("statPointsPerLevel", 3, 0, 100);
        levelSpPerLevel = BUILDER.comment("Maximum SP granted per level starting from level 2.")
                .defineInRange("levelSpPerLevel", 2.0, 0.0, 1000.0);
        levelSpBonusCap = BUILDER.comment("Upper limit of the maximum-SP bonus granted by level.")
                .defineInRange("levelSpBonusCap", 200.0, 0.0, 100000.0);
        levelXpPerKill = BUILDER.comment(
                "Player-level XP gained when a monster is defeated. 0 disables gaining XP from kills.")
                .defineInRange("levelXpPerKill", 25, 0, 1000000);
        levelXpFromMonstersOnly = BUILDER.comment(
                "Only hostile monsters (MobCategory.MONSTER) grant player-level XP. "
                        + "Set to false to also gain XP from animals and other mobs.")
                .define("levelXpFromMonstersOnly", true);
        BUILDER.pop();

        BUILDER.push("Stat Settings");
        strDamagePerPoint = BUILDER.comment(
                "Sword-skill damage bonus per point of STR (+0.005 = +0.5% per point). "
                        + "STR is folded into the custom sword-skill formula, not into the vanilla attack attribute.")
                .defineInRange("strDamagePerPoint", 0.005, 0.0, 1.0);
        strMaxBonus = BUILDER.comment("Upper limit of the STR damage bonus (0.5 = +50%).")
                .defineInRange("strMaxBonus", 0.5, 0.0, 100.0);
        agilityMoveSpeedPerPoint = BUILDER.comment(
                "Movement speed bonus per point of AGI (0.003 = +0.3% per point).")
                .defineInRange("agilityMoveSpeedPerPoint", 0.003, 0.0, 1.0);
        agilityMaxMoveSpeedBonus = BUILDER.comment("Upper limit of the AGI movement speed bonus.")
                .defineInRange("agilityMaxMoveSpeedBonus", 0.3, 0.0, 10.0);
        levelDiffEnabled = BUILDER.comment(
                "Enable the SAO-style level-difference term on damage dealt. It only applies to entities "
                        + "that carry an enemy level, so other mods' mobs are never affected.")
                .define("levelDiffEnabled", true);
        levelDiffPerPoint = BUILDER.comment(
                "Damage dealt modifier per point of level difference (0.02 = 2% per level).")
                .defineInRange("levelDiffPerPoint", 0.02, 0.0, 1.0);
        levelDiffCap = BUILDER.comment("Maximum absolute level difference taken into account.")
                .defineInRange("levelDiffCap", 25, 0, 1000);
        BUILDER.pop();

        BUILDER.push("Smithing Settings");
        smithingEnabled = BUILDER.comment("Enable the smithing (forged weapon) system.")
                .define("smithingEnabled", false);
        forgeBaseDamage = BUILDER.comment("Base attack damage of a forged weapon before material and part scaling.")
                .defineInRange("forgeBaseDamage", 1.0, 0.0, 1000.0);
        forgeMaterialPowerScale = BUILDER.comment(
                "How strongly the material power turns into attack damage (material power * this value).")
                .defineInRange("forgeMaterialPowerScale", 3.0, 0.0, 1000.0);
        forgeQualityDamageBonus = BUILDER.comment(
                "Damage bonus at maximum quality (0.5 = +50% attack damage at quality 100).")
                .defineInRange("forgeQualityDamageBonus", 0.5, 0.0, 100.0);
        forgeEnhanceDamageBonus = BUILDER.comment("Attack damage bonus per enhancement level (0.05 = +5%).")
                .defineInRange("forgeEnhanceDamageBonus", 0.05, 0.0, 10.0);
        forgeEnhanceMax = BUILDER.comment("Maximum enhancement level of a forged weapon.")
                .defineInRange("forgeEnhanceMax", 10, 0, 1000);
        strPerWeight = BUILDER.comment(
                "STR required per unit of weapon weight. The requirement is weight * this value rounded up. "
                        + "0.1 keeps the requirement low (about 1-3 for wood..iron and 3-5 for the top tiers).")
                .defineInRange("strPerWeight", 0.1, 0.0, 100.0);
        overweightAttackSpeedPenalty = BUILDER.comment(
                "Attack speed modifier applied when STR is at least half of the requirement but below it "
                        + "(-0.3 = -30% attack speed).")
                .defineInRange("overweightAttackSpeedPenalty", -0.3, -1.0, 0.0);
        overweightSkillSpeedMultiplier = BUILDER.comment(
                "Sword-skill execution speed multiplier while overweight (0.7 = skills run 30% slower).")
                .defineInRange("overweightSkillSpeedMultiplier", 0.7, 0.05, 1.0);
        creationMaterialCost = BUILDER.comment(
                "Materials consumed to create a weapon. The enhancement cost is enhancementCostRatio of this value.")
                .defineInRange("creationMaterialCost", 9, 1, 64);
        enhancementCostRatio = BUILDER.comment("Ratio of the creation materials consumed per enhancement level.")
                .defineInRange("enhancementCostRatio", 0.1, 0.0, 1.0);
        qualityProficiencyPerPoint = BUILDER.comment(
                "Proficiency (smithing skill) per point of quality. quality = proficiency / this value, capped.")
                .defineInRange("qualityProficiencyPerPoint", 20.0, 0.01, 100000.0);
        qualityMax = BUILDER.comment("Maximum quality of a forged weapon.")
                .defineInRange("qualityMax", 100, 1, 100000);
        reinforcementPowerRatio = BUILDER.comment(
                "Material power added per unit of power of a material placed in a reinforcement slot "
                        + "(0.15 = +15% of the material power).")
                .defineInRange("reinforcementPowerRatio", 0.15, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("Weapon Memory Settings");
        critBaseChance = BUILDER.comment("Critical hit chance of a forged weapon (0.05 = 5%).")
                .defineInRange("critBaseChance", 0.05, 0.0, 1.0);
        critBaseMultiplier = BUILDER.comment("Damage multiplier of a critical hit before weapon-memory growth.")
                .defineInRange("critBaseMultiplier", 1.5, 1.0, 100.0);
        shortCooldownTicks = BUILDER.comment(
                "Skills whose cooldown is at or below this value count as short-cooldown skills in weapon memory.")
                .defineInRange("shortCooldownTicks", 60, 1, 100000);
        memoryDamageBonusPerHit = BUILDER.comment(
                "Damage bonus for the matching attack tendency per hit (0.002 = +0.2% per thrust/sweep/flying hit).")
                .defineInRange("memoryDamageBonusPerHit", 0.002, 0.0, 1.0);
        memoryDamageBonusCap = BUILDER.comment("Upper limit of a tendency damage bonus (0.5 = +50%).")
                .defineInRange("memoryDamageBonusCap", 0.5, 0.0, 100.0);
        memoryCritDamagePerHit = BUILDER.comment("Critical damage bonus per critical hit (0.005 = +0.5%).")
                .defineInRange("memoryCritDamagePerHit", 0.005, 0.0, 1.0);
        memoryCritDamageCap = BUILDER.comment("Upper limit of the critical damage bonus (1.0 = +100%).")
                .defineInRange("memoryCritDamageCap", 1.0, 0.0, 100.0);
        memoryCooldownReductionPerHit = BUILDER.comment(
                "Cooldown reduction per short-cooldown hit (0.0005 = 0.05%).")
                .defineInRange("memoryCooldownReductionPerHit", 0.0005, 0.0, 1.0);
        memoryCooldownReductionCap = BUILDER.comment("Upper limit of the weapon-memory cooldown reduction (0.3 = 30%).")
                .defineInRange("memoryCooldownReductionCap", 0.3, 0.0, 0.95);
        BUILDER.pop();

        BUILDER.push("SP Settings");
        defaultMaxSp = BUILDER.comment("Default maximum SP value").defineInRange("defaultMaxSP", 100.0, 1.0, 10000.0);
        defaultSpRegen = BUILDER.comment("Default SP regeneration value per second").defineInRange("defaultSPRegen",
                1.0, 0.0, 1000.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

}