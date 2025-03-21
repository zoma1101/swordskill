package com.zoma1101.SwordSkill.entity.renderer.skillRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class WhipEffectRenderer_backup {

    private static final float R = 1f;
    private static final float G = 1f;
    private static final float B = 1f;
    private static final float THICKNESS = 0.03f; // 線の太さを調整
    private static final float LENGTH = 0.3f; // 方向ベクトルの長さを調整
    private static final int STEPS = 80; // 軌跡を構成する線の数
    private static final int MAX_AGE = 5; // エフェクトが完全に表示されるまでの Age の最大値
    private static final float START_OFFSET = 0.4f; // basePosition からの描画開始位置のオフセット
    private static final int INDIVIDUAL_FADE_OUT_DURATION = 10; // 個々の線分が消えるまでの時間 (ティック)

    public static void whip_renderEffect(PoseStack poseStack, MultiBufferSource bufferSource, int light, Vector3f scale, String setTexture, Vec2 StartRot, Vec2 FinishRot, int Age, Vec2 entityRotation) {
        poseStack.pushPose(); // 変換を保存
        ResourceLocation textures = fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + setTexture + ".png");
        // JOMLのMatrix4fを取得
        Matrix4f matrix = poseStack.last().pose();

        // スケール適用 (必要に応じて)
        matrix.scale(scale.x, scale.y, scale.z);

        // 描画するステップ数を Age に基づいて計算
        int drawSteps = Mth.clamp((int) (STEPS * (float) Age / MAX_AGE), 0, STEPS);

        for (int i = 0; i <= drawSteps; i++) {
            // 角度を補間
            float yaw = Mth.lerp((float) i / (float) STEPS, StartRot.y, FinishRot.y);
            float pitch = Mth.lerp((float) i / (float) STEPS, StartRot.x, FinishRot.x);

            // 方向ベクトルを生成 (entityRotation を考慮)
            Vector3f baseRotation = rotateLookVec(new Vec2(pitch, yaw));
            Vector3f rotatedVector = rotateVectorByEntityRotation(baseRotation, entityRotation);

            // 描画開始位置をオフセット
            Vector3f offsetStartPos = new Vector3f(rotatedVector).normalize().mul(START_OFFSET);

            // 終点のベクトル (方向ベクトルに長さを掛け合わせ、始点に加算)
            Vector3f end = new Vector3f(rotatedVector).normalize().mul(LENGTH).add(offsetStartPos);

            // 透明度を Age とステップ数に基づいて計算
            final float alpha = getAlpha(Age, i, STEPS);

            // 線の描画
            renderLine(poseStack, bufferSource, light, offsetStartPos, end, alpha, textures, THICKNESS);
        }

        poseStack.popPose(); // 変換を元に戻す
    }

    private static float getAlpha(int Age, int i, int steps) {
        float alpha = 1.0f;
        if (Age < MAX_AGE) {
            // 徐々に表示
            alpha = Math.min(1.0f, (float) Age / MAX_AGE);
        }

        // 個々の透明化処理
        int spawnTime = (int) (i * (float)MAX_AGE / steps); // この線分が描画され始めたおおよその時間
        int individualAge = Age - spawnTime;
        if (individualAge > 0) {
            alpha *= Math.max(0.0f, 1.0f - (float) individualAge / INDIVIDUAL_FADE_OUT_DURATION);
        }
        return alpha;
    }

    private static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource, int light, Vector3f start, Vector3f end, float alphaBase, ResourceLocation textures, float thickness) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        Vector3f direction = new Vector3f(end).sub(start).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f(direction).cross(up).normalize();
        Vector3f offset = new Vector3f(right).mul(thickness / 2.0f);
        Vector3f normal = new Vector3f(0, 1, 0);

        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(textures));

        Vector3f p1 = new Vector3f(start).add(offset);
        Vector3f p2 = new Vector3f(start).sub(offset);
        Vector3f p3 = new Vector3f(end).sub(offset.mul(2));
        Vector3f p4 = new Vector3f(end).add(offset.mul(2));

        // 透明度をUV座標に基づいて調整
        float blurFactor = 0.4f; // ぼかしの強さを調整する係数 (0.0 ~ 1.0)

        // 頂点の順序を反転 (例: p1 -> p4 -> p3 -> p2)
        builder.vertex(matrix, p1.x(), p1.y(), p1.z())
                .color(R, G, B, alphaBase * (1 - blurFactor * (Math.abs(0f - 0.5f) + Math.abs(1f - 0.5f)))) // UV (0f, 1f)
                .uv(0f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.x(), normal.y(), normal.z())
                .endVertex();
        builder.vertex(matrix, p4.x(), p4.y(), p4.z())
                .color(R, G, B, alphaBase * (1 - blurFactor * (Math.abs(0f - 0.5f) + Math.abs(0f - 0.5f)))) // UV (0f, 0f)
                .uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.x(), normal.y(), normal.z())
                .endVertex();
        builder.vertex(matrix, p3.x(), p3.y(), p3.z())
                .color(R, G, B, alphaBase * (1 - blurFactor * (Math.abs(1f - 0.5f) + Math.abs(0f - 0.5f)))) // UV (1f, 0f)
                .uv(1f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.x(), normal.y(), normal.z())
                .endVertex();
        builder.vertex(matrix, p2.x(), p2.y(), p2.z())
                .color(R, G, B, alphaBase * (1 - blurFactor * (Math.abs(1f - 0.5f) + Math.abs(1f - 0.5f)))) // UV (1f, 1f)
                .uv(1f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private static Vector3f rotateLookVec(Vec2 MinecraftRotation) {
        float pitch = MinecraftRotation.x * ((float)Math.PI / 180F);
        float yaw = -MinecraftRotation.y * ((float)Math.PI / 180F);
        float cosPitch = Mth.cos(pitch);
        float sinPitch = Mth.sin(pitch);
        float cosYaw = Mth.cos(yaw);
        float sinYaw = Mth.sin(yaw);
        return new Vector3f(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    private static Vector3f rotateVectorByEntityRotation(Vector3f vector, Vec2 entityRotation) {
        float yaw = -entityRotation.y * Mth.DEG_TO_RAD;
        float pitch = entityRotation.x * Mth.DEG_TO_RAD;

        Quaternionf yawRotation = new Quaternionf().rotationY(yaw);
        Quaternionf pitchRotation = new Quaternionf().rotationX(pitch);

        Quaternionf combinedRotation = yawRotation.mul(pitchRotation);
        return combinedRotation.transform(vector);
    }
}