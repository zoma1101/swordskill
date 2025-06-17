package com.zoma1101.swordskill.config;


import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SwordSkillEffect_System;
    public static final ModConfigSpec.IntValue hudPosX;
    public static final ModConfigSpec.IntValue hudPosY;
    public static final ModConfigSpec.IntValue hudScale;

    public static final int HUD_SCALE_MAX = 64;

    static {
        BUILDER.push("SwordSkillEffect Settings");
        SwordSkillEffect_System = BUILDER.comment("Please adjust according to the Shader").defineInRange("SwordSkillEffect_System", 0, 0, 3);

        BUILDER.push("HUD Settings");
        hudPosX = BUILDER.defineInRange("hudPosX", 0, -1920, 1920);
        hudPosY = BUILDER.defineInRange("hudPosY", 0, -1080, 1080);
        hudScale = BUILDER.defineInRange("hudScale", 24, 1, HUD_SCALE_MAX);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}