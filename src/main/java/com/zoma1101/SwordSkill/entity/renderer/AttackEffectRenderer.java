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
            "textures/entity/simple_2.png");

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

            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
            float roll = entity.getRotation();

            poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch + 90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

            render3DCrescent(poseStack, bufferSource, outerRadius, innerRadius, 180.0f, color, 24);
            poseStack.popPose();
        }

        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880);
    }

    private static void render3DCrescent(PoseStack poseStack, MultiBufferSource bufferSource,
            float outerRadius, float innerRadius, float arcAngleDeg, int color, int segments) {

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f * 2.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (color & 0xFF) / 255.0f * 2.0f;

        if ((r + g + b) < 0.01f || a < 0.01f) {
            a = 1.0f; r = 0.5f; g = 1.5f; b = 2.0f;
        }

        VertexConsumer c = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f mat = poseStack.last().pose();
        org.joml.Matrix3f norm = poseStack.last().normal();

        int light = 15728880;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        float arcRad = (float) Math.toRadians(arcAngleDeg);
        float startAngle = -arcRad / 2.0f;
        float halfThick = 0.05f;

        for (int i = 0; i < segments; i++) {
            float t0 = startAngle + arcRad * i / segments;
            float t1 = startAngle + arcRad * (i + 1) / segments;

            float sin0 = (float) Math.sin(t0), cos0 = (float) Math.cos(t0);
            float sin1 = (float) Math.sin(t1), cos1 = (float) Math.cos(t1);

            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            // 外側のオーラ (1.2倍の広がり, 0.4倍の透明度)
            drawCrescentLayer(c, mat, norm, sin0, cos0, sin1, cos1, outerRadius, innerRadius, halfThick, r, g, b, a * 0.4f, 1.2f, u0, u1, light, overlay);
            // 内側の芯 (1.0倍, 標準透明度)
            drawCrescentLayer(c, mat, norm, sin0, cos0, sin1, cos1, outerRadius, innerRadius, halfThick, r, g, b, a, 1.0f, u0, u1, light, overlay);
        }
    }

    private static void drawCrescentLayer(VertexConsumer c, Matrix4f mat, org.joml.Matrix3f norm, 
            float sin0, float cos0, float sin1, float cos1, 
            float outerRadius, float innerRadius, float halfThick, 
            float r, float g, float b, float a, float scale, 
            float u0, float u1, int light, int overlay) {
        
        float or0 = outerRadius * scale, or1 = outerRadius * scale;
        float ir0 = innerRadius, ir1 = innerRadius;

        float ox0 = sin0 * or0, oy0 = cos0 * or0;
        float ox1 = sin1 * or1, oy1 = cos1 * or1;
        float ix0 = sin0 * ir0, iy0 = cos0 * ir0;
        float ix1 = sin1 * ir1, iy1 = cos1 * ir1;

        // 表面 (Z+)
        c.vertex(mat, ox0, oy0, halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, 1).endVertex();
        c.vertex(mat, ox1, oy1, halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, 1).endVertex();
        c.vertex(mat, ix1, iy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, 1).endVertex();
        c.vertex(mat, ix0, iy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, 1).endVertex();

        // 裏面 (Z-)
        c.vertex(mat, ix0, iy0, -halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, -1).endVertex();
        c.vertex(mat, ix1, iy1, -halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, -1).endVertex();
        c.vertex(mat, ox1, oy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, -1).endVertex();
        c.vertex(mat, ox0, oy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 0, -1).endVertex();

        // 側面 (外側)
        c.vertex(mat, ox0, oy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 1, 0).endVertex();
        c.vertex(mat, ox1, oy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light).normal(norm, 0, 1, 0).endVertex();
        c.vertex(mat, ox1, oy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 1, 0).endVertex();
        c.vertex(mat, ox0, oy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light).normal(norm, 0, 1, 0).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AttackEffectEntity entity) {
        return TEXTURE;
    }
}
