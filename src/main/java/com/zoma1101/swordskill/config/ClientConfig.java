package com.zoma1101.swordskill.config;


import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SwordSkillEffect_System;
    public static final ModConfigSpec.IntValue hudPosX;
    public static final ModConfigSpec.IntValue hudPosY;
    public static final ModConfigSpec.IntValue hudScale;
    public static final ModConfigSpec.BooleanValue hudShowSkillLevel;

    public static final ModConfigSpec.BooleanValue trailSparks;
    public static final ModConfigSpec.BooleanValue sparkStreaks;
    public static final ModConfigSpec.BooleanValue hitSparks;
    public static final ModConfigSpec.BooleanValue hitRing;
    public static final ModConfigSpec.BooleanValue hitFlash;
    public static final ModConfigSpec.BooleanValue slashDecals;
    public static final ModConfigSpec.DoubleValue sparkDensity;

    public static final ModConfigSpec.BooleanValue guiDarkTheme;

    public static final int HUD_SCALE_MAX = 64;

    static {
        BUILDER.push("SwordSkillEffect Settings");
        SwordSkillEffect_System = BUILDER.comment("Please adjust according to the Shader").defineInRange("SwordSkillEffect_System", 0, 0, 3);

        BUILDER.push("Spark Settings");
        trailSparks = BUILDER.comment("Emit coloured sparks along the weapon while a sword skill trail is active.")
                .define("trailSparks", true);
        sparkStreaks = BUILDER.comment("Add fast, streaking sparks that follow the trail colour along the blade and impacts.")
                .define("sparkStreaks", true);
        hitSparks = BUILDER.comment("Emit a burst of coloured sparks when a sword skill hits an entity.")
                .define("hitSparks", true);
        hitRing = BUILDER.comment("Show an expanding light ring at the point of impact.")
                .define("hitRing", true);
        hitFlash = BUILDER.comment("Show a short screen flash when you land a sword skill hit.")
                .define("hitFlash", true);
        slashDecals = BUILDER.comment("Briefly leave a glowing slash mark where an enemy was cut.")
                .define("slashDecals", true);
        sparkDensity = BUILDER.comment("Multiplier for the amount of sparks. 1.0 is the default.")
                .defineInRange("sparkDensity", 1.0, 0.0, 3.0);
        BUILDER.pop();

        BUILDER.push("HUD Settings");
        hudPosX = BUILDER.defineInRange("hudPosX", 0, -1920, 1920);
        hudPosY = BUILDER.defineInRange("hudPosY", 0, -1080, 1080);
        hudScale = BUILDER.defineInRange("hudScale", 24, 1, HUD_SCALE_MAX);
        hudShowSkillLevel = BUILDER.comment("Show the selected skill's level and damage multiplier under the HUD SP bar.")
                .define("hudShowSkillLevel", true);
        BUILDER.pop();

        BUILDER.push("GUI Settings");
        guiDarkTheme = BUILDER.comment("Use the dark theme for the sword skill GUI. Set to false for the light theme.")
                .define("guiDarkTheme", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}