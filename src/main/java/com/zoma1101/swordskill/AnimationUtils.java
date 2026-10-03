package com.zoma1101.swordskill;

import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.client.handler.WeaponTrailHandler;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.Objects;
import java.util.Set;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AnimationUtils {
    private static final Set<String> FIRST_PERSON_RENDER_MODS = Set.of("bettercombat", "epicfight", "firstperson");
    private static final FirstPersonConfiguration FIRST_PERSON_CONFIGURATION = new FirstPersonConfiguration()
            .setShowRightArm(true)
            .setShowLeftArm(true);

    //rightitemはZ軸で後ろY+いくらしい
    //Y軸でY- Z+ 方向に行くらしい
    //X軸で-X方向に行くらしい
    //手の方向そのままに剣配置　(0,3,2) -90

    public static void PlayerAnim(Player player,int SkillID, String type){
        var skill = SwordSkillRegistry.SKILLS.get(SkillID);
        if (skill == null) {
            return;
        }
        if (player instanceof AbstractClientPlayer clientPlayer) {
            if (skill != null && skill.getDataDefinition() != null) {
                WeaponTrailHandler.setSkillColor(clientPlayer.getId(), skill.getDataDefinition().color());
            } else {
                WeaponTrailHandler.setSkillColor(clientPlayer.getId(), "#54D8FF");
            }
        }
        ResourceLocation animationId;
        String AnimationName = skill != null && skill.getDataDefinition() != null
                ? skill.getDataDefinition().animation()
                : skill.getName();
        if (skill.getDataDefinition() != null && skill.getType() == SkillData.SkillType.RUSH) {
            var definition = skill.getDataDefinition();
            type = switch (type) {
                case "start" -> definition.rushStartAnimation();
                case "move" -> definition.rushMoveAnimation();
                case "finish" -> definition.rushFinishAnimation();
                default -> type;
            };
        }
        if (type.isEmpty()) {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName);
        } else {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName + "." + type);
        }


        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));

        if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer
                && PlayerAnimationRegistry.getAnimation(animationId) != null) {
            KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(
                    (dev.kosmx.playerAnim.core.data.KeyframeAnimation) Objects.requireNonNull(PlayerAnimationRegistry.getAnimation(animationId)));
            animationPlayer.setFirstPersonMode(shouldUsePlayerAnimatorFirstPerson()
                    ? FirstPersonMode.THIRD_PERSON_MODEL
                    : FirstPersonMode.NONE);
            animationPlayer.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
            animationLayer.setAnimation(animationPlayer);
        }

    }

    public static void PlayerChargeAnim(Player player, int skillId, String type) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        FrozenKeyframeAnimationPlayer animationPlayer = getChargeAnimationPlayer(player, skillId, type);
        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
        if (animationPlayer != null && playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
            animationLayer.setAnimation(animationPlayer);
        }
    }

    public static void StopPlayerAnimation(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                    .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
            if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
                animationLayer.setAnimation(null);
            }
        }
    }

    private static FrozenKeyframeAnimationPlayer getChargeAnimationPlayer(Player player, int skillId, String type) {
        var skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill == null || !(player instanceof AbstractClientPlayer clientPlayer)) {
            return null;
        }

        String animationName = skill.getDataDefinition() != null
                ? skill.getDataDefinition().animation()
                : skill.getName();
        if (skill.getDataDefinition() != null && skill.getType() == SkillData.SkillType.RUSH) {
            var definition = skill.getDataDefinition();
            type = switch (type) {
                case "start" -> definition.rushStartAnimation();
                case "move" -> definition.rushMoveAnimation();
                case "finish" -> definition.rushFinishAnimation();
                default -> type;
            };
        }
        ResourceLocation animationId = type.isEmpty()
                ? fromNamespaceAndPath(SwordSkill.MOD_ID, animationName)
                : fromNamespaceAndPath(SwordSkill.MOD_ID, animationName + "." + type);
        var animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null) {
            return null;
        }

        FrozenKeyframeAnimationPlayer animationPlayer = new FrozenKeyframeAnimationPlayer(
                (dev.kosmx.playerAnim.core.data.KeyframeAnimation) animation);
        animationPlayer.setFirstPersonMode(shouldUsePlayerAnimatorFirstPerson()
                ? FirstPersonMode.THIRD_PERSON_MODEL
                : FirstPersonMode.NONE);
        animationPlayer.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
        if (skill.getDataDefinition() != null) {
            WeaponTrailHandler.setSkillColor(clientPlayer.getId(), skill.getDataDefinition().color());
        }
        return animationPlayer;
    }

    private static class FrozenKeyframeAnimationPlayer extends KeyframeAnimationPlayer {
        private FrozenKeyframeAnimationPlayer(dev.kosmx.playerAnim.core.data.KeyframeAnimation animation) {
            super(animation);
        }

        @Override
        public void tick() {
        }

        @Override
        public void setupAnim(float tickDelta) {
            super.setupAnim(0.0F);
        }
    }

    private static boolean shouldUsePlayerAnimatorFirstPerson() {
        return FIRST_PERSON_RENDER_MODS.stream().noneMatch(ModList.get()::isLoaded);
    }

    private static class SwordSkillAnimationLayer extends ModifierLayer<IAnimation> {
        @Override
        public FirstPersonMode getFirstPersonMode(float tickDelta) {
            return shouldUsePlayerAnimatorFirstPerson()
                    ? FirstPersonMode.THIRD_PERSON_MODEL
                    : super.getFirstPersonMode(tickDelta);
        }

        @Override
        public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
            return shouldUsePlayerAnimatorFirstPerson()
                    ? FIRST_PERSON_CONFIGURATION
                    : super.getFirstPersonConfiguration(tickDelta);
        }
    }

    public static class AnimationRegister {
        public static void AnimationSetup() {
                //Set the player construct callback. It can be a lambda function.
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"),
                        42,
                        AnimationRegister::registerPlayerAnimation);
        }
        private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
            return new SwordSkillAnimationLayer();
        }
    }
}