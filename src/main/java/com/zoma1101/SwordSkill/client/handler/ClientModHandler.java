package com.zoma1101.swordskill.client.handler;
import net.minecraftforge.fml.common.Mod;


import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.particle.SkillSparkParticle;
import com.zoma1101.swordskill.client.particle.SparkStreakParticle;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.model.AttackEffectModel;
import com.zoma1101.swordskill.entity.renderer.AttackEffectRenderer;
import com.zoma1101.swordskill.entity.renderer.SkillMannequinRenderer;
import com.zoma1101.swordskill.particle.SkillParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;


@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybindings.INSTANCE.SwordSkill_Selector_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_QuickSelect_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_HUD_Setting);
            event.register(Keybindings.INSTANCE.SwordSkill_Status_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_0);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_1);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_2);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_3);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_4);
    }

    //レイヤー読み込み処理
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AttackEffectModel.LAYER_LOCATION,AttackEffectModel::createBodyLayer);
    }
    //レンダラー読み込み処理
    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(SwordSkill_Entities.ATTACK_EFFECT.get(), AttackEffectRenderer::new);
        event.registerEntityRenderer(SwordSkill_Entities.SKILL_MANNEQUIN.get(), SkillMannequinRenderer::new);
    }

    //スキルエフェクト用の光る粒子
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // RegistryObject 側は ParticleType<?> なので、各 ParticleOptions へ明示的にキャストする。
        event.registerSprite(SkillParticles.SKILL_SPARK.get(), (options, level, x, y, z, vx, vy, vz) ->
                new SkillSparkParticle(level, x, y, z, vx, vy, vz, (SkillSparkParticleOptions) options));
        event.registerSprite(SkillParticles.SPARK_STREAK.get(), (options, level, x, y, z, vx, vy, vz) ->
                new SparkStreakParticle(level, x, y, z, vx, vy, vz, (SparkStreakParticleOptions) options));
    }

    //鍛冶台・溶解炉の画面（1.20.1 では FMLClientSetupEvent で MenuScreens に直接登録する）
    @SubscribeEvent
    public static void registerMenuScreens(FMLClientSetupEvent event) {
        if (!com.zoma1101.swordskill.SwordSkillFeatures.SMITHING) {
            return;
        }
        MenuScreens.register(com.zoma1101.swordskill.smithing.SmithingRegistry.forgeMenu(),
                com.zoma1101.swordskill.client.gui.SmithingForgeScreen::new);
        MenuScreens.register(com.zoma1101.swordskill.smithing.SmithingRegistry.meltingMenu(),
                com.zoma1101.swordskill.client.gui.MeltingForgeScreen::new);
    }

    //鍛造武器の色（tintindex 0 = メインカラー / 1 = サブカラー）
    @SubscribeEvent
    public static void registerItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        if (!com.zoma1101.swordskill.SwordSkillFeatures.SMITHING) {
            return;
        }
        event.register((stack, tintIndex) -> {
            com.zoma1101.swordskill.smithing.ForgedWeaponData data =
                    com.zoma1101.swordskill.smithing.ForgedWeaponData.get(stack);
            if (data == null) {
                return 0xFFFFFF;
            }
            return tintIndex == 0 ? data.colorMain() : data.colorSub();
        }, com.zoma1101.swordskill.smithing.SmithingRegistry.FORGED_WEAPON.get(),
                com.zoma1101.swordskill.smithing.SmithingRegistry.FORGED_INGOT.get());
    }
}
