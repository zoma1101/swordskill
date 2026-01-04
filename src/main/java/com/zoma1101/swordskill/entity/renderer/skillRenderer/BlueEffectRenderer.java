package com.zoma1101.swordskill.entity.renderer.skillRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.zoma1101.swordskill.config.ClientConfig.SwordSkillEffect_System;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class BlueEffectRenderer {

    private static final float R_COLOR = 1.0f;
    private static final float G_COLOR = 1.0f;
    private static final float B_COLOR = 1.0f;
    private static final float ALPHA = 1.0f;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int FRAME_TIME = 2;
    private static final int ANIMATION_FRAMES = 7;

    private static final Map<String, ResourceLocation[]> TEXTURE_CACHE = new ConcurrentHashMap<>();

    private static ResourceLocation[] getOrLoadTextures(String setTexture) {
        return TEXTURE_CACHE.computeIfAbsent(setTexture, key -> {
            ResourceLocation[] textures = new ResourceLocation[ANIMATION_FRAMES];
            for (int i = 0; i < ANIMATION_FRAMES; i++) {
                textures[i] = fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + key + "/" + i + ".png");
            }
            return textures;
        });
    }

    public static void renderEffect(Vec2 minecraftRotation, PoseStack poseStack, MultiBufferSource bufferSource, int light, Vector3f scale, float zRotation, int age, String setTexture) {
        if (minecraftRotation == null || Float.isNaN(minecraftRotation.x) || Float.isNaN(minecraftRotation.y) ||
                scale == null || Float.isNaN(scale.x()) || Float.isNaN(scale.y()) || Float.isNaN(scale.z()) ||
                Float.isNaN(zRotation) || setTexture == null || setTexture.isEmpty()) {
            LOGGER.warn("BlueEffectRenderer: Invalid input parameters. Skipping render. Rotation: {}, Scale: {}, Z-Rotation: {}, Texture: {}", minecraftRotation, scale, zRotation, setTexture);
            return;
        }

        poseStack.pushPose();

        int frame = (age / FRAME_TIME) % ANIMATION_FRAMES;
        ResourceLocation[] textures = getOrLoadTextures(setTexture);
        ResourceLocation currentTexture = textures[frame];

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Minecraft_Rotationに基づいて回転を適用
        applyMinecraftRotation(matrix, normalMatrix, minecraftRotation, zRotation);

        Vector3f Nomal = determineLocalNormalDirection(zRotation);
        // スケーリング
        poseStack.scale(scale.x(), scale.y(), scale.z());

        VertexConsumer builder = getVertexConsumer(bufferSource, currentTexture);
        PoseStack.Pose pose = poseStack.last();

        float[][] corners = {
                {-0.5f, 0f, -0.5f, 0f, 1f},  // 左下
                { 0.5f, 0f, -0.5f, 1f, 1f},  // 右下
                { 0.5f, 0f, 0.5f, 1f, 0f},  // 右上
                {-0.5f, 0f, 0.5f, 0f, 0f}   // 左上
        };

        for (float[] v : corners) {
            builder
                    .addVertex(pose, v[0], v[1], v[2])
                    .setColor(R_COLOR, G_COLOR, B_COLOR, ALPHA)
                    .setUv(v[3], v[4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, Nomal.x(), Nomal.y(), Nomal.z());
        }

        poseStack.popPose();
    }


    private static VertexConsumer getVertexConsumer(MultiBufferSource bufferSource, ResourceLocation currentTexture) {
        return switch (SwordSkillEffect_System.get()) {
            case 1 -> bufferSource.getBuffer(RenderType.entityTranslucent(currentTexture));
            case 2 -> bufferSource.getBuffer(RenderType.entitySmoothCutout(currentTexture));
            case 3 -> bufferSource.getBuffer(RenderType.entityCutoutNoCull(currentTexture));
            default -> bufferSource.getBuffer(RenderType.entityTranslucentEmissive(currentTexture));
        };
    }

    private static Vector3f determineLocalNormalDirection(float rotation) {
        if (rotation > -80 && rotation < 80){
            return new Vector3f(0,1,0);
        }
        else if (rotation < 110 && rotation > 70){
            return new Vector3f(1,0,0);
        }
        else if (rotation > -110 && rotation < -70){
            return  new Vector3f(-1,0,0);
        }
        else {
            return new Vector3f(0, -1, 0);
        }
    }

    private static void applyMinecraftRotation(Matrix4f matrix, Matrix3f normalMatrix, Vec2 Minecraft_Rotation, float rotation) {
        // Minecraftの回転角度をラジアンに変換
        float rotationY = (float) Math.toRadians(-Minecraft_Rotation.y);
        float rotationX = (float) Math.toRadians(Minecraft_Rotation.x);
        float rotationZ = (float) Math.toRadians(rotation);

        // クォータニオンの適用順を修正 (Y → X → Z)
        Quaternionf quaternionY = new Quaternionf().rotationY(rotationY);
        Quaternionf quaternionX = new Quaternionf().rotationX(rotationX);
        Quaternionf quaternionZ = new Quaternionf().rotationZ(rotationZ);

        // 回転の順序を Y → X → Z にする
        quaternionY.mul(quaternionX).mul(quaternionZ);

        matrix.rotate(quaternionY);
        normalMatrix.rotate(quaternionY);
    }
}
