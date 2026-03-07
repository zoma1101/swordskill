package com.zoma1101.swordskill;

import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

import java.util.Objects;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AnimationUtils {

    // rightitemはZ軸で後ろY+いくらしい
    // Y軸でY- Z+ 方向に行くらしい
    // X軸で-X方向に行くらしい
    // 手の方向そのままに剣配置 (0,3,2) -90

    public static void PlayerAnim(Player player, int SkillID, String type) {
        ResourceLocation animationId;
        String AnimationName = SwordSkillRegistry.SKILLS.get(SkillID).getName();
        if (type.isEmpty()) {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName);
        } else {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName + "." + type);
        }

        ModifierLayer<IAnimation> animationLayer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));

        if (animationLayer != null && PlayerAnimationRegistry.getAnimation(animationId) != null) {
            animationLayer.setAnimation(new KeyframeAnimationPlayer(
                    Objects.requireNonNull(PlayerAnimationRegistry.getAnimation(animationId))));
        }

    }

    /**
     * 一人称視点のPoseStackに、(Player Animatorが動かしている)三人称モデルのボーン回転を同期させる
     */
    public static void applyFirstPersonAnimation(AbstractClientPlayer player, PoseStack poseStack) {
        // プレイヤーレンダラー経由で、アニメーション適用済みのモデルを取得
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(player);
        if (renderer instanceof PlayerRenderer playerRenderer) {
            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();

            // 利き腕に応じて、三人称モデルの腕の回転を一人称の PoseStack に適用
            // Player Animatorは model.rightArm などの ModelPart を直接回転させている
            net.minecraft.client.model.geom.ModelPart arm = (player
                    .getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT)
                            ? model.rightArm
                            : model.leftArm;

            // 回転を適用 (Minecraft標準の X -> Y -> Z 順)
            if (arm.xRot != 0)
                poseStack.mulPose(new Quaternionf().rotationX(arm.xRot));
            if (arm.yRot != 0)
                poseStack.mulPose(new Quaternionf().rotationY(arm.yRot));
            if (arm.zRot != 0)
                poseStack.mulPose(new Quaternionf().rotationZ(arm.zRot));

            // ボーン自体の移動オフセット(1/16)を適用
            poseStack.translate(arm.x / 16.0f, arm.y / 16.0f, arm.z / 16.0f);
        }
    }

    public static class AnimationRegister {
        public static void AnimationSetup() {
            // Set the player construct callback. It can be a lambda function.
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"),
                    42,
                    AnimationRegister::registerPlayerAnimation);
        }

        private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
            return new ModifierLayer<>();
        }
    }
}