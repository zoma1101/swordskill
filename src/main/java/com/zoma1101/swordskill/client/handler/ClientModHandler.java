package com.zoma1101.swordskill.client.handler;


import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.particle.SkillSparkParticle;
import com.zoma1101.swordskill.client.particle.SparkStreakParticle;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.model.AttackEffectModel;
import com.zoma1101.swordskill.entity.renderer.AttackEffectRenderer;
import com.zoma1101.swordskill.particle.SkillParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;


@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
    }

    //スキルエフェクト用の光る粒子
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSprite(SkillParticles.SKILL_SPARK.get(), (options, level, x, y, z, vx, vy, vz) ->
                new SkillSparkParticle(level, x, y, z, vx, vy, vz, options));
        event.registerSprite(SkillParticles.SPARK_STREAK.get(), (options, level, x, y, z, vx, vy, vz) ->
                new SparkStreakParticle(level, x, y, z, vx, vy, vz, options));
    }

    //鍛冶台の画面
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(com.zoma1101.swordskill.smithing.SmithingRegistry.forgeMenu(),
                com.zoma1101.swordskill.client.gui.SmithingForgeScreen::new);
    }

    //鍛造武器の色（tintindex 0 = メインカラー / 1 = サブカラー）
    @SubscribeEvent
    public static void registerItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
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
