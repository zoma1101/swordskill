package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.SkillSlotDisplayOverlay;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.model.AttackEffectModel;
import com.zoma1101.swordskill.entity.renderer.AttackEffectRenderer;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.zoma1101.swordskill.item.SampleItemRegistry.UNLOCKITEM;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        event.register(Keybindings.INSTANCE.SwordSkill_Wheel_Key);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud_skill_slot", SkillSlotDisplayOverlay.HUD_SKILL_SLOT);
        event.registerAboveAll("skill_wheel", com.zoma1101.swordskill.client.gui.SkillWheelOverlay.HUD_SKILL_WHEEL);
    }

    // レイヤー読み込み処理
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(AttackEffectModel.LAYER_LOCATION, AttackEffectModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SwordSkill_Entities.ATTACK_EFFECT.get(), AttackEffectRenderer::new);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // デフォルトのスキン (Steve) にレイヤーを追加
        PlayerRenderer defaultRenderer = event.getSkin("default");
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new SwordTrailLayer(defaultRenderer));
        }

        // スリムなスキン (Alex) にレイヤーを追加
        PlayerRenderer slimRenderer = event.getSkin("slim");
        if (slimRenderer != null) {
            slimRenderer.addLayer(new SwordTrailLayer(slimRenderer));
        }
    }

    @SubscribeEvent
    public static void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(UNLOCKITEM.get());
        }
    }

}
