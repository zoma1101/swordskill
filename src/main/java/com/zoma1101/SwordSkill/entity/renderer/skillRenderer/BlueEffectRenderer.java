package com.zoma1101.SwordSkill.entity.renderer.skillRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class BlueEffectRenderer {

    private static final float R = 2f;
    private static final float G = 2f;
    private static final float B = 2f;

    private static final int FRAME_TIME = 2; // 各フレームの表示時間 (Tick数)

    public static void renderEffect(Vec2 Minecraft_Rotation, PoseStack poseStack, MultiBufferSource bufferSource, int light, Vector3f scale, float rotation, int age, String setTexture) {
        poseStack.pushPose(); // 変換を保存

        // 現在のアニメーションフレームを計算
        int frame = (age / FRAME_TIME) % 7; // アニメーションフレーム数を7に固定

        // 動的にテクスチャ配列を生成
        ResourceLocation[] textures = new ResourceLocation[7];
        for (int i = 0; i < 7; i++) {
            textures[i] = fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + setTexture + "/" + i + ".png");
        }

        ResourceLocation currentTexture = textures[frame];

        // JOMLのMatrix4fを取得
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();

        // Minecraft_Rotationに基づいて回転を適用
        applyMinecraftRotation(matrix, normalMatrix, Minecraft_Rotation, rotation);

        // スケール適用
        matrix.scale(scale.x, scale.y, scale.z);

        Vector3f cameraDirection;
        if (rotation >= -90 && rotation <= 90){
            cameraDirection = new Vector3f(0,1,0);
        }
        else {cameraDirection = new Vector3f(0, -1, 0);}

        // 頂点データ作成 (テクスチャ座標, 法線, ライト)
        VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(currentTexture));

        builder.vertex(matrix, -0.5f, 0f, -0.5f)
                .color(R, G, B, 1f)
                .uv(0f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                .endVertex();

        builder.vertex(matrix, 0.5f, 0f, -0.5f)
                .color(R, G, B, 1f)
                .uv(1f, 1f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                .endVertex();

        builder.vertex(matrix, 0.5f, 0f, 0.5f)
                .color(R, G, B, 1f)
                .uv(1f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                .endVertex();

        builder.vertex(matrix, -0.5f, 0f, 0.5f)
                .color(R, G, B, 1f)
                .uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                .endVertex();

        poseStack.popPose(); // 変換を元に戻す
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