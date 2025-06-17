package com.zoma1101.swordskill.config;


import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue damageMultiplier;
    public static final ModConfigSpec.DoubleValue cooldownMultiplier;
    public static final ModConfigSpec.BooleanValue PvP;
    public static final ModConfigSpec.BooleanValue AUTOWEAPON_SETTING;
    public static final ModConfigSpec.BooleanValue UnlockedSkill;
    public static final ModConfigSpec.BooleanValue attackNeutralMobs;


    static {
        BUILDER.push("Damage Settings");
        damageMultiplier = BUILDER.comment("Sword skill damage multiplier").defineInRange("damageMultiplier", 1.0, 0.1, 1000.0);
        cooldownMultiplier = BUILDER.comment("Sword skill cool down multiplier").defineInRange("CoolDownMultiplier", 1.0, 0.01, 10.0);
        AUTOWEAPON_SETTING = BUILDER.comment("The available weapons are set automatically to some extent.").define("AutoWeaponSetting", false);
        PvP = BUILDER.comment("Allow PVP with sword skills").define("pvp", false);
        UnlockedSkill = BUILDER.comment("Enable Sword Skill Unlock").define("UnlockedSkill", true);
        attackNeutralMobs = BUILDER.comment("Include neutral mobs as attack targets").define("attackNeutralMobs", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

}