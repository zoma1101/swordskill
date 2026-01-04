package com.zoma1101.swordskill.client.handler;


import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.model.AttackEffectModel;
import com.zoma1101.swordskill.entity.renderer.AttackEffectRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;


@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybindings.INSTANCE.SwordSkill_Selector_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_QuickSelect_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_HUD_Setting);
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
}
