package com.zoma1101.swordskill;

import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Objects;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AnimationUtils {

    public static void PlayerAnimation(int SkillID, String type){
        if (isPlayerAnimator()) {
            Player player = Minecraft.getInstance().player;
            ResourceLocation animationId;
            String AnimationName = SwordSkillRegistry.SKILLS.get(SkillID).getName();
            if (type.isEmpty()) {
                animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName);
            } else {
                animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName + "." + type);
            }


            ModifierLayer<IAnimation> animationLayer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) player).get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));

            if (animationLayer != null && PlayerAnimationRegistry.getAnimation(animationId) != null) {
                animationLayer.setAnimation(new KeyframeAnimationPlayer(Objects.requireNonNull(PlayerAnimationRegistry.getAnimation(animationId))));
            }
        }
    }
    //rightitemはZ軸で後ろY+いくらしい
    //Y軸でY- Z+ 方向に行くらしい
    //X軸で-X方向に行くらしい
    //手の方向そのままに剣配置　(0,3,2) -90

    @Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class AnimationRegister {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            if (isPlayerAnimator()) {
                //Set the player construct callback. It can be a lambda function.
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"),
                        42,
                        AnimationRegister::registerPlayerAnimation);
            }
        }

        private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
            return new ModifierLayer<>();
        }
    }

    private static boolean isPlayerAnimator(){
        String PLAYER_ANIMATOR_MODID = "playeranimator";
        return ModList.get().isLoaded(PLAYER_ANIMATOR_MODID);
    }

}