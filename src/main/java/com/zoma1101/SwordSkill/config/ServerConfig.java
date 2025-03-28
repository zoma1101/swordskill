package com.zoma1101.swordskill.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue damageMultiplier;
    public static final ForgeConfigSpec.DoubleValue cooldownMultiplier;
    public static final ForgeConfigSpec.BooleanValue PvP;
    public static final ForgeConfigSpec.BooleanValue AUTOWEAPON_SETTING;
    public static final ForgeConfigSpec.BooleanValue attackNeutralMobs;


    static {
        BUILDER.push("Damage Settings");
        damageMultiplier = BUILDER.comment("Sword skill damage multiplier").defineInRange("damageMultiplier", 1.0, 0.1, 1000.0);
        cooldownMultiplier = BUILDER.comment("Sword skill cool down multiplier").defineInRange("CoolDownMultiplier", 1.0, 0.01, 10.0);
        AUTOWEAPON_SETTING = BUILDER.comment("The available weapons are set automatically to some extent.").define("auto weapon setting", false);
        PvP = BUILDER.comment("Allow PVP with sword skills").define("pvp", false);
        attackNeutralMobs = BUILDER.comment("Include neutral mobs as attack targets").define("attackNeutralMobs", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

}