package com.zoma1101.swordskill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static com.zoma1101.swordskill.AnimationUtils.AnimationRegister.AnimationSetup;
import static com.zoma1101.swordskill.AnimationUtils.PlayerAnim;


public class IsAnimation {
    private static boolean isPlayerAnimator(){
        String PLAYER_ANIMATOR_MODID = "playeranimator";
        return ModList.get().isLoaded(PLAYER_ANIMATOR_MODID);
    }

    public static void PlayerAnimation(int SkillID, String type){
        if (isPlayerAnimator()) {
            LocalPlayer player = Minecraft.getInstance().player;
            PlayerAnim(player,SkillID,type);
        }
    }

    @Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class AnimationRegister {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (isPlayerAnimator()) {
                AnimationSetup();
            }
        }

    }
}