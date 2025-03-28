package com.zoma1101.swordskill.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue hudPosX;
    public static final ForgeConfigSpec.IntValue hudPosY;
    public static final ForgeConfigSpec.IntValue hudScale;

    public static final int HUD_SCALE_MAX = 64;

    static {
        BUILDER.push("HUD Settings");
        hudPosX = BUILDER.defineInRange("hudPosX", 0, 0, 1920);
        hudPosY = BUILDER.defineInRange("hudPosY", 0, 0, 1080);
        hudScale = BUILDER.defineInRange("hudScale", 24, 1, HUD_SCALE_MAX);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}