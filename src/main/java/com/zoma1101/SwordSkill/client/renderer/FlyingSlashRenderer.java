package com.zoma1101.swordskill.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 飛翔斬撃（SHAPE_ARC タグ）の三日月モデルを AFTER_PARTICLES で確実に描画する。
 * SwordTrailRenderer と同じ方式で endBatch() を自力で呼ぶ。
 */
public class FlyingSlashRenderer {

    private static final List<AttackEffectEntity> targets = new ArrayList<>();

    public static void register(AttackEffectEntity entity) {
        if (!targets.contains(entity))
            targets.add(entity);
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;

        // デバッグ: render が呼ばれているか確認
        if (!targets.isEmpty()) {
            mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal(
                    "[FSR] render called, targets=" + targets.size()));
        }

        // SwordTrailLayer.renderTrail と全く同じパターン
        // event の PoseStack を使う（直接書き換えず、pushPose/popPose で絶対に元に戻す）
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        Vector3f camPos = new Vector3f(
                (float) camera.getPosition().x,
                (float) camera.getPosition().y,
                (float) camera.getPosition().z);
        Quaternionf camRot = camera.rotation();

        Iterator<AttackEffectEntity> iter = targets.iterator();
        while (iter.hasNext()) {
            AttackEffectEntity entity = iter.next();

            if (entity.isRemoved()) {
                iter.remove();
                continue;
            }

            if (!entity.hasTag(SkillTag.SHAPE_ARC))
                continue;

            float outerRadius = entity.getEffectRadius().x;
            float innerRadius = outerRadius * 0.55f;
            int color = entity.getTrailColor();

            // エンティティ位置をビュー空間に変換（SwordTrailLayer.toViewSpace と同じ方法）
            Vector3f entityRel = new Vector3f(
                    (float) entity.getX() - camPos.x,
                    (float) entity.getY() - camPos.y,
                    (float) entity.getZ() - camPos.z);
            new Quaternionf(camRot).conjugate().transform(entityRel);

            // エンティティの向き（Yaw/Pitch/RollZ）をクォータニオンに変換
            Quaternionf entityRot = new Quaternionf()
                    .rotateY((float) Math.toRadians(-entity.getYRot()))
                    .rotateX((float) Math.toRadians(entity.getXRot()))
                    .rotateZ((float) Math.toRadians(entity.getRotation()));

            poseStack.pushPose();
            // SwordTrailLayer.renderTrail と同じく、push 後に identity にリセット
            poseStack.last().pose().identity();

            // ビュー空間でエンティティ位置へ移動してからエンティティの向きを適用
            poseStack.translate(entityRel.x, entityRel.y, entityRel.z);
            poseStack.mulPose(entityRot);

            renderCrescent(poseStack, bufferSource, outerRadius, innerRadius, 270.0f, color, 24);

            poseStack.popPose();
        }

        bufferSource.endBatch();
    }

    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/misc/white.png");

    private static void renderCrescent(PoseStack poseStack, MultiBufferSource bufferSource,
            float outerRadius, float innerRadius,
            float arcAngleDeg, int color, int segments) {
        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = Math.min(((color >> 16) & 0xFF) / 255.0f * 2.0f, 1.0f);
        float g = Math.min(((color >> 8) & 0xFF) / 255.0f * 2.0f, 1.0f);
        float b = Math.min((color & 0xFF) / 255.0f * 2.0f, 1.0f);

        VertexConsumer c = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        Matrix4f mat = poseStack.last().pose();
        Matrix3f norm = poseStack.last().normal();

        float arcRad = (float) Math.toRadians(arcAngleDeg);
        float startAngle = -arcRad / 2.0f;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        for (int i = 0; i < segments; i++) {
            float t0 = startAngle + arcRad * i / segments;
            float t1 = startAngle + arcRad * (i + 1) / segments;

            float ox0 = (float) Math.sin(t0) * outerRadius, oy0 = (float) Math.cos(t0) * outerRadius;
            float ox1 = (float) Math.sin(t1) * outerRadius, oy1 = (float) Math.cos(t1) * outerRadius;
            float ix0 = (float) Math.sin(t0) * innerRadius, iy0 = (float) Math.cos(t0) * innerRadius;
            float ix1 = (float) Math.sin(t1) * innerRadius, iy1 = (float) Math.cos(t1) * innerRadius;

            float u0 = (float) i / segments, u1 = (float) (i + 1) / segments;

            // 表面
            quad(c, mat, norm, ox0, oy0, ox1, oy1, ix1, iy1, ix0, iy0, r, g, b, a, u0, u1, overlay);
            // 裏面
            quad(c, mat, norm, ix0, iy0, ix1, iy1, ox1, oy1, ox0, oy0, r, g, b, a, u0, u1, overlay);
        }
    }

    private static void quad(VertexConsumer c, Matrix4f m, Matrix3f n,
            float x0, float y0, float x1, float y1,
            float x2, float y2, float x3, float y3,
            float r, float g, float b, float a,
            float u0, float u1, int overlay) {
        c.vertex(m, x0, y0, 0).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(15728880).normal(n, 0, 0, 1)
                .endVertex();
        c.vertex(m, x1, y1, 0).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(15728880).normal(n, 0, 0, 1)
                .endVertex();
        c.vertex(m, x2, y2, 0).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(15728880).normal(n, 0, 0, 1)
                .endVertex();
        c.vertex(m, x3, y3, 0).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(15728880).normal(n, 0, 0, 1)
                .endVertex();
    }
}
