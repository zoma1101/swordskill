package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {

    // 実在するテクスチャ simple.png を使用
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
            "textures/entity/simple.png");

    public AttackEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AttackEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource, int packedLight) {

        if (entity.hasTag(SkillTag.SHAPE_ARC)) {
            float outerRadius = entity.getEffectRadius().x;
            float innerRadius = outerRadius * 0.9f;
            int color = entity.getTrailColor();

            poseStack.pushPose();

            // スムーズな回転補間
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float roll = entity.getRotation();


            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch +90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

            render3DCrescent(poseStack, bufferSource, outerRadius, innerRadius, 180.0f, color, 24);
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880);
    }

    private static void render3DCrescent(PoseStack poseStack, MultiBufferSource bufferSource,
            float outerRadius, float innerRadius, float arcAngleDeg, int color, int segments) {

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * 2.0f; // 明るくするために 2倍
        float g = ((color >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (color & 0xFF) / 255.0f * 2.0f;

        // 色が黒または完全に透明な場合のデフォルト値
        if ((r + g + b) < 0.01f || a < 0.01f) {
            a = 1.0f;
            r = 0.5f;
            g = 1.5f;
            b = 2.0f;
        }

        VertexConsumer c = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        org.joml.Matrix3f norm = poseStack.last().normal();

        int light = 15728880;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        float arcRad = (float) Math.toRadians(arcAngleDeg);
        float startAngle = -arcRad / 2.0f;

        // 厚み
        float halfThick = 0.1f;

        for (int i = 0; i < segments; i++) {
            float t0 = startAngle + arcRad * i / segments;
            float t1 = startAngle + arcRad * (i + 1) / segments;

            float ox0 = (float) Math.sin(t0) * outerRadius;
            float oy0 = (float) Math.cos(t0) * outerRadius;
            float ox1 = (float) Math.sin(t1) * outerRadius;
            float oy1 = (float) Math.cos(t1) * outerRadius;
            float ix0 = (float) Math.sin(t0) * innerRadius;
            float iy0 = (float) Math.cos(t0) * innerRadius;
            float ix1 = (float) Math.sin(t1) * innerRadius;
            float iy1 = (float) Math.cos(t1) * innerRadius;

            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            // 表面 (Z+)
            c.vertex(mat, ox0, oy0, halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, 1).endVertex();
            c.vertex(mat, ox1, oy1, halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, 1).endVertex();
            c.vertex(mat, ix1, iy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, 1).endVertex();
            c.vertex(mat, ix0, iy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, 1).endVertex();

            // 裏面 (Z-)
            c.vertex(mat, ix0, iy0, -halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, -1).endVertex();
            c.vertex(mat, ix1, iy1, -halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, -1).endVertex();
            c.vertex(mat, ox1, oy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, -1).endVertex();
            c.vertex(mat, ox0, oy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 0, -1).endVertex();

            // 側面外側
            c.vertex(mat, ox0, oy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 1, 0).endVertex();
            c.vertex(mat, ox1, oy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 1, 0).endVertex();
            c.vertex(mat, ox1, oy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 1, 0).endVertex();
            c.vertex(mat, ox0, oy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, 1, 0).endVertex();

            // 側面内側
            c.vertex(mat, ix0, iy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, -1, 0).endVertex();
            c.vertex(mat, ix1, iy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, -1, 0).endVertex();
            c.vertex(mat, ix1, iy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, -1, 0).endVertex();
            c.vertex(mat, ix0, iy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light)
                    .normal(norm, 0, -1, 0).endVertex();
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AttackEffectEntity entity) {
        return TEXTURE;
    }
}
