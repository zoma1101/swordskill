package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 三日月（クレセント）形のポリゴンをプロシージャルに描画するユーティリティ。
 * AttackEffectRenderer から呼び出して使う。
 */
public class CrescentRenderer {

    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/misc/white.png");

    /**
     * 三日月形を現在の PoseStack 位置に描画する。
     *
     * @param poseStack    描画トランスフォーム
     * @param bufferSource バッファソース
     * @param outerRadius  外円の半径（ブロック単位）
     * @param innerRadius  内円の半径（ブロック単位）
     * @param arcAngleDeg  弧の広がり角度（度数法）
     * @param color        ARGB 色（int）
     * @param segments     分割数（多いほど滑らか）
     */
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
            float outerRadius, float innerRadius,
            float arcAngleDeg, int color, int segments) {

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 発光エミッシブ → 2倍ブライト
        r = Math.min(r * 2.0f, 1.0f);
        g = Math.min(g * 2.0f, 1.0f);
        b = Math.min(b * 2.0f, 1.0f);

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucentEmissive(WHITE));

        Matrix4f mat = poseStack.last().pose();

        float arcRad = (float) Math.toRadians(arcAngleDeg);
        float startAngle = -arcRad / 2.0f;

        for (int i = 0; i < segments; i++) {
            float t0 = startAngle + arcRad * i / segments;
            float t1 = startAngle + arcRad * (i + 1) / segments;

            // 外側の2頂点
            float ox0 = (float) Math.sin(t0) * outerRadius;
            float oy0 = (float) Math.cos(t0) * outerRadius;
            float ox1 = (float) Math.sin(t1) * outerRadius;
            float oy1 = (float) Math.cos(t1) * outerRadius;

            // 内側の2頂点
            float ix0 = (float) Math.sin(t0) * innerRadius;
            float iy0 = (float) Math.cos(t0) * innerRadius;
            float ix1 = (float) Math.sin(t1) * innerRadius;
            float iy1 = (float) Math.cos(t1) * innerRadius;

            // UV は簡易でよい
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

            // 表面
            quad(consumer, mat, ox0, oy0, ox1, oy1, ix1, iy1, ix0, iy0, r, g, b, a, u0, u1, overlay);
            // 裏面（両面表示）
            quad(consumer, mat, ix0, iy0, ix1, iy1, ox1, oy1, ox0, oy0, r, g, b, a, u0, u1, overlay);
        }
    }

    private static void quad(VertexConsumer c, Matrix4f m,
            float x0, float y0, float x1, float y1,
            float x2, float y2, float x3, float y3,
            float r, float g, float b, float a,
            float u0, float u1, int overlay) {
        c.vertex(m, x0, y0, 0).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(15728880).normal(0, 0, 1)
                .endVertex();
        c.vertex(m, x1, y1, 0).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(15728880).normal(0, 0, 1)
                .endVertex();
        c.vertex(m, x2, y2, 0).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(15728880).normal(0, 0, 1)
                .endVertex();
        c.vertex(m, x3, y3, 0).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(15728880).normal(0, 0, 1)
                .endVertex();
    }
}
