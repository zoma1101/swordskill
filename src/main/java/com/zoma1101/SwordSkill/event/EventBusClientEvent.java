package com.zoma1101.SwordSkill.event;

import com.zoma1101.SwordSkill.entity.SwordSkill_Entities;
import com.zoma1101.SwordSkill.entity.model.WhipEffectModel;
import com.zoma1101.SwordSkill.entity.renderer.AttackEffectRenderer;
import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.entity.model.AttackEffectModel;
import com.zoma1101.SwordSkill.entity.renderer.WhipEffectRenderer;
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
        event.registerLayerDefinition(WhipEffectModel.LAYER_LOCATION,WhipEffectModel::createBodyLayer);
    }
    //レンダラー読み込み処理
    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(SwordSkill_Entities.ATTACK_EFFECT.get(), AttackEffectRenderer::new);
        event.registerEntityRenderer(SwordSkill_Entities.WHIP_EFFECT.get(), WhipEffectRenderer::new);
    }
}
