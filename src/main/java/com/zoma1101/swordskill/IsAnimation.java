package com.zoma1101.swordskill;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


import static com.zoma1101.swordskill.AnimationUtils.AnimationRegister.AnimationSetup;
import static com.zoma1101.swordskill.AnimationUtils.PlayerAnim;


public class IsAnimation {
    private static boolean isPlayerAnimator(){
        String PLAYER_ANIMATOR_MODID = "playeranimator";
        return ModList.get().isLoaded(PLAYER_ANIMATOR_MODID);
    }

    public static void PlayerAnimation(int entityId, int SkillID, String type){
        if (isPlayerAnimator()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                net.minecraft.world.entity.Entity entity = mc.level.getEntity(entityId);
                if (entity instanceof net.minecraft.client.player.AbstractClientPlayer clientPlayer) {
                    PlayerAnim(clientPlayer, SkillID, type);
                }
            }
        }
    }

    @EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class AnimationRegister {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (isPlayerAnimator()) {
                AnimationSetup();
            }
        }

    }
}