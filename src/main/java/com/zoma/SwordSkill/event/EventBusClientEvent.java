package com.zoma.SwordSkill.event;

import com.zoma.SwordSkill.entity.SwordSkill_Entities;
import com.zoma.SwordSkill.entity.renderer.AttackEffectRenderer;
import com.zoma.SwordSkill.main.SwordSkill;
import com.zoma.SwordSkill.entity.model.AttackEffectModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public class EventBusClientEvent {

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
