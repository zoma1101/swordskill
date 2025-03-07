package com.zoma.SwordSkill.entity.renderer.skillRenderer;

import com.mojang.blaze3d.vertex.*;
import com.zoma.SwordSkill.main.SwordSkill;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.*;

import java.lang.Math;

public class BlueEffectRenderer {

    private static final float R = 1f;
    private static final float G = 1f;
    private static final float B = 1f;

    private static final int FRAME_TIME = 2; // 各フレームの表示時間 (Tick数)

    public static void renderEffect(PoseStack poseStack, MultiBufferSource bufferSource, int light, Vector3f scale, Vec3 rotation, Vec3 rotationCenter, int age, String setTexture) {
        poseStack.pushPose(); // 変換を保存

        // 現在のアニメーションフレームを計算
        int frame = (age / FRAME_TIME) % 7; // アニメーションフレーム数を7に固定

        // 動的にテクスチャ配列を生成
        ResourceLocation[] textures = new ResourceLocation[7];
        for (int i = 0; i < 7; i++) {
            textures[i] = new ResourceLocation(SwordSkill.MOD_ID, "textures/entity/" + setTexture + "/" + i + ".png");
        }

        ResourceLocation currentTexture = textures[frame];

        // JOMLのMatrix4fを取得
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // 回転適用
        rotateEffect(matrix, normalMatrix, rotation, rotationCenter);

        // スケール適用
        matrix.scale((float) scale.x, (float) scale.y, (float) scale.z);

        // 頂点データ作成 (テクスチャ座標, 法線, ライト)
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(currentTexture));

        builder.vertex(matrix, -0.5f, 0f, -0.5f)
                .color(R, G, B, 1f)
                .uv(0f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, 0, 0, 1)
                .endVertex();

        builder.vertex(matrix, 0.5f, 0f, -0.5f)
                .color(R, G, B, 1f)
                .uv(1f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, 0, 0, 1)
                .endVertex();

        builder.vertex(matrix, 0.5f, 0f, 0.5f)
                .color(R, G, B, 1f)
                .uv(1f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, 0, 0, 1)
                .endVertex();

        builder.vertex(matrix, -0.5f, 0f, 0.5f)
                .color(R, G, B, 1f)
                .uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, 0, 0, 1)
                .endVertex();

        poseStack.popPose(); // 変換を元に戻す
    }

    /**
     * 指定された回転を適用するメソッド
     */
    private static void rotateEffect(Matrix4f matrix, Matrix3f normalMatrix, Vec3 rotation, Vec3 rotationCenter) {
        // 角度をラジアンに変換
        float rotationX = (float) Math.toRadians(rotation.x);
        float rotationY = (float) Math.toRadians(rotation.y);
        float rotationZ = (float) Math.toRadians(rotation.z);

        // クォータニオンの適用順を修正 (Y → X → Z)
        Quaternionf quaternionY = new Quaternionf().rotationY(rotationY);
        Quaternionf quaternionX = new Quaternionf().rotationX(rotationX);
        Quaternionf quaternionZ = new Quaternionf().rotationZ(rotationZ);

        // 回転の順序を Y → X → Z にする
        quaternionY.mul(quaternionX).mul(quaternionZ);

        // 回転の中心を考慮
        if (!rotationCenter.equals(Vec3.ZERO)) {
            matrix.translate((float) rotationCenter.x, (float) rotationCenter.y, (float) rotationCenter.z);
            matrix.rotate(quaternionY);
            matrix.translate((float) -rotationCenter.x, (float) -rotationCenter.y, (float) -rotationCenter.z);
        } else {
            matrix.rotate(quaternionY);
        }

        normalMatrix.rotate(quaternionY);
    }
}