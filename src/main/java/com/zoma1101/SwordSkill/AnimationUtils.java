package com.zoma1101.swordskill;

import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class AnimationUtils {

    public static void PlayerAnim(Player player, int SkillID, String type) {
        ResourceLocation animationId;
        String AnimationName = SwordSkillRegistry.SKILLS.get(SkillID).getName();
        if (type != null && !type.isEmpty()) {
             animationId = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName + "." + type);
        } else {
             animationId = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName);
        }

        ModifierLayer<IAnimation> animationLayer = (ModifierLayer<IAnimation>) PlayerAnimationAccess
                .getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));

        if (animationLayer != null) {
            var anim = PlayerAnimationRegistry.getAnimation(animationId);
            // 指定されたIDで見つからない場合は、接尾辞なしの基本名でリキャスト
            if (anim == null && animationId.getPath().contains(".")) {
                String basePath = animationId.getPath().split("\\.")[0];
                animationId = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, basePath);
                anim = PlayerAnimationRegistry.getAnimation(animationId);
            }

            if (anim != null) {
                animationLayer.setAnimation(new KeyframeAnimationPlayer(anim));
            }
        }

    }

    /**
     * 一人称視点のPoseStackに、三人称と同じモデル変換を適用する。
     * SwordTrailLayer.getMatrix4f を用いることで、剣先と腕の動きを完全に一致させる。
     */
    public static void applyFirstPersonAnimation(AbstractClientPlayer player, PoseStack poseStack, float partialTicks) {
        // 現在のところ一人称描画のアニメーション同期は保留し、バニラの通常のアイテムスイングを利用します。
        // （詳細は FIRST_PERSON_ANIMATION_NOTES.md を参照）
        /*
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer.TrailSession session = 
                com.zoma1101.swordskill.client.renderer.layer.SwordTrailManager.getSession(player.getUUID());

        if (session != null && session.isActiveAnimation()) {
            float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
            if (t > session.animationLength) return;

            // アニメーションデータの取得
            org.joml.Vector3f bodyRotDeg = session.bodyRotTrack != null ? session.bodyRotTrack.evaluate(t) : new org.joml.Vector3f();
            org.joml.Vector3f bodyPosPx = session.bodyPosTrack != null ? session.bodyPosTrack.evaluate(t) : new org.joml.Vector3f();
            org.joml.Vector3f armRotDeg = session.armRotTrack != null ? session.armRotTrack.evaluate(t) : new org.joml.Vector3f();
            org.joml.Vector3f armPosPx = session.armPosTrack != null ? session.armPosTrack.evaluate(t) : new org.joml.Vector3f();
            org.joml.Vector3f itemRotDeg = session.itemRotTrack != null ? session.itemRotTrack.evaluate(t) : new org.joml.Vector3f();
            org.joml.Vector3f itemPosPx = session.itemPosTrack != null ? session.itemPosTrack.evaluate(t) : new org.joml.Vector3f();

            // --- 座標変換の適用 ---
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(armRotDeg.x));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(armRotDeg.y));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(armRotDeg.z));

            poseStack.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);

            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(itemRotDeg.x));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(itemRotDeg.y));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(itemRotDeg.z));
        }
        */
    }


    public static class AnimationRegister {
        public static void AnimationSetup() {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"),
                    42,
                    AnimationRegister::registerPlayerAnimation);
        }

        private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
            return new ModifierLayer<>();
        }
    }
}