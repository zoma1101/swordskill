package com.zoma1101.swordskill.config;


import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.TranslatableEnum;

public class ClientConfig {
    public enum TrailRenderMode implements TranslatableEnum {
        /** 通常の半透明。現状の安定版（デフォルト）。 */
        TRANSLUCENT,
        /** 加算合成（エミッシブ）。空でも光って見えるが、ブルームで白飛びしやすい。 */
        ADDITIVE,
        /** エミッシブ（自己発光）シェーダー＋通常の半透明。ADDITIVE よりブルームが弱い。 */
        EMISSIVE;

        @Override
        public Component getTranslatedName() {
            return Component.translatable("swordskill.configuration.trailRenderMode." + name());
        }
    }

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
    public static final ModConfigSpec.BooleanValue clashSparks;
    public static final ModConfigSpec.ConfigValue<String> clashAnimation;
    public static final ModConfigSpec.DoubleValue clawTrailBrightness;
    public static final ModConfigSpec.DoubleValue trailGlow;
    public static final ModConfigSpec.DoubleValue trailOpacity;
    public static final ModConfigSpec.EnumValue<TrailRenderMode> trailRenderMode;
    public static final ModConfigSpec.DoubleValue trailAdditiveIntensity;

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
        clashSparks = BUILDER.comment("Emit sparks while a blade lock (鍔迫り合い) is active.")
                .define("clashSparks", true);
        clashAnimation = BUILDER.comment(
                "Player-animation name played on both players during a blade lock. "
                        + "Leave empty to keep the current skill pose. The name is resolved as "
                        + "swordskill:<name> unless a namespace is given.")
                .define("clashAnimation", "clash_lock");
        BUILDER.pop();

        BUILDER.push("Trail Settings");
        clawTrailBrightness = BUILDER.comment(
                "Brightness multiplier for the triple claw weapon trail (0.0-1.0). "
                        + "Lower values reduce the excessive glow of claw skills.")
                .defineInRange("clawTrailBrightness", 0.4, 0.0, 1.0);
        trailGlow = BUILDER.comment(
                "Glow strength of the weapon trail (0.0-2.0). "
                        + "Higher values make the white core brighter and wider.")
                .defineInRange("trailGlow", 1.0, 0.0, 2.0);
        trailOpacity = BUILDER.comment(
                "Overall opacity of the weapon trail (0.2-1.0). "
                        + "Lower values make the trail more transparent.")
                .defineInRange("trailOpacity", 1.0, 0.2, 1.0);
        trailRenderMode = BUILDER.comment(
                "How the weapon trail is blended.",
                "TRANSLUCENT: normal alpha blending (stable default).",
                "ADDITIVE: additive glow. Visible even over the sky, but bloom-heavy shader packs may blow it out.",
                "EMISSIVE: self-lit (ignores the light map) with normal alpha blending. Shader packs may treat it as an emissive surface.")
                .defineEnum("trailRenderMode", TrailRenderMode.TRANSLUCENT);
        trailAdditiveIntensity = BUILDER.comment(
                "Brightness multiplier applied only in ADDITIVE mode (0.05-2.0). " +
                        "Lower it if the trail blows out.")
                .defineInRange("trailAdditiveIntensity", 1.0, 0.05, 2.0);
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