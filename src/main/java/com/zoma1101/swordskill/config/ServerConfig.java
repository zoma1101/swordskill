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

        BUILDER.push("SP Settings");
        defaultMaxSp = BUILDER.comment("Default maximum SP value").defineInRange("defaultMaxSP", 100.0, 1.0, 10000.0);
        defaultSpRegen = BUILDER.comment("Default SP regeneration value per second").defineInRange("defaultSPRegen",
                1.0, 0.0, 1000.0);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

}