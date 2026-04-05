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
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {

        private static final ResourceLocation TEXTURE = fromNamespaceAndPath(SwordSkill.MOD_ID,
                        "textures/entity/dust.png");
        private static final ResourceLocation BEAM_TEXTURE = fromNamespaceAndPath(SwordSkill.MOD_ID,
                        "textures/entity/beam.png");

        private static final float[] SIN_TABLE_64 = new float[65];
        private static final float[] COS_TABLE_64 = new float[65];

        static {
                for (int i = 0; i <= 64; i++) {
                        SIN_TABLE_64[i] = (float) Math.sin(i * Math.PI * 2 / 64);
                        COS_TABLE_64[i] = (float) Math.cos(i * Math.PI * 2 / 64);
                }
        }

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
 
                        // 1. プレイヤーの向きに合わせる (+180で正面を向ける)
                        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw ));
                        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                        
                        // 2. 視線軸に対して傾ける (Roll)
                        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
                        
                        // 3. 三日月メッシュの基準角度を調整
                        poseStack.mulPose(Axis.XP.rotationDegrees(90));
 
                        render3DCrescent(poseStack, bufferSource, outerRadius, innerRadius, 180.0f, color, 24);
                        poseStack.popPose();
                }
 
                // 突きの演出効果 (ビームと波動のレンダリング)
                if (entity.hasTag(SkillTag.RAY) || entity.hasTag(SkillTag.SHAPE_THRUST)) {
                        poseStack.pushPose();
                        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
                        float yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
                        float roll = entity.getRotation();
 
                        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
                        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(roll)); // 突きに対しても視線軸回転を適用

                        // よりプレイヤーに近い位置（手前）からエフェクトを開始
                        poseStack.translate(0, 0, -1.2f);

                        int color = entity.getTrailColor();
                        float aBase = ((color >> 24) & 0xFF) / 255.0f;
                        float rBase = ((color >> 16) & 0xFF) / 255.0f;
                        float gBase = ((color >> 8) & 0xFF) / 255.0f;
                        float bBase = (color & 0xFF) / 255.0f;

                        // 1. 貫通ビームのレンダリング (RAYタグがある場合)
                        if (entity.hasTag(SkillTag.RAY)) {
                                boolean isVorpalStrike = entity.hasTag(SkillTag.POWERFUL_THRUST);
                                boolean isFirstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();

                                // ヴォーパルストライク等の特殊攻撃、または一人称視点の場合のみビームを表示
                                if (isVorpalStrike || isFirstPerson) {
                                        float beamR = rBase * 2.0f;
                                        float beamG = gBase * 2.0f;
                                        float beamB = bBase * 2.0f;
                                        renderThrustBeam(entity, poseStack, bufferSource, partialTicks, beamR, beamG,
                                                        beamB, aBase);
                                }
                        }

                        // 2. 波動（ドーナツ状の衝撃波）のレンダリング (SHAPE_THRUSTタグがある場合)
                        if (entity.hasTag(SkillTag.SHAPE_THRUST)) {
                                if (entity.hasTag(SkillTag.POWERFUL_THRUST)) {
                                        // 強力な突きの多重波動（手前・中間・奥）
                                        for (int i = 0; i < 3; i++) {
                                                poseStack.pushPose();
                                                poseStack.translate(0, 0, i * -1.5f);
                                                renderThrustWave(entity, poseStack, bufferSource, partialTicks,
                                                                i * 2.0f);
                                                poseStack.popPose();
                                        }
                                } else {
                                        renderThrustWave(entity, poseStack, bufferSource, partialTicks, 0);
                                }
                        }

                        poseStack.popPose();
                }
        }

        private static void render3DCrescent(PoseStack poseStack, MultiBufferSource bufferSource,
                        float outerRadius, float innerRadius, float arcAngleDeg, int color, int segments) {

                float a = ((color >> 24) & 0xFF) / 255.0f;
                float rBase = ((color >> 16) & 0xFF) / 255.0f;
                float gBase = ((color >> 8) & 0xFF) / 255.0f;
                float bBase = (color & 0xFF) / 255.0f;

                // トレイルと同じ色に合わせるため、3日月弧も* 2.0f (セーフなみ)
                float r_final = rBase * 2.0f;
                float g_final = gBase * 2.0f;
                float b_final = bBase * 2.0f;

                if ((r_final + g_final + b_final) < 0.01f || a < 0.01f) {
                        a = 1.0f;
                        r_final = 0.5f;
                        g_final = 1.5f;
                        b_final = 2.0f;
                }

                // 深度書き込みなしのRenderTypeを使用し、鮮やかな色味を100%維持
                VertexConsumer c = bufferSource.getBuffer(RenderType.entityNoOutline(TEXTURE));
                Matrix4f mat = poseStack.last().pose();
                org.joml.Matrix3f norm = poseStack.last().normal();

                int light = 15728880;
                int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

                float arcRad = (float) Math.toRadians(arcAngleDeg);
                float startAngle = -arcRad / 2.0f;
                float halfThick = 0.05f;

                float sin0 = (float) Math.sin(startAngle);
                float cos0 = (float) Math.cos(startAngle);

                for (int i = 0; i < segments; i++) {
                        float t1 = startAngle + arcRad * (i + 1) / segments;
                        float sin1 = (float) Math.sin(t1);
                        float cos1 = (float) Math.cos(t1);

                        float u0 = (float) i / segments;
                        float u1 = (float) (i + 1) / segments;

                        // 外側のオーラ (1.2倍の広がり, 0.4倍の透明度)
                        drawCrescentLayer(c, mat, norm, sin0, cos0, sin1, cos1, outerRadius, innerRadius, halfThick, r_final,
                                        g_final, b_final,
                                        a * 0.4f, 1.2f, u0, u1, light, overlay);
                        // 内側の芯 (1.0倍, 標準透明度)
                        drawCrescentLayer(c, mat, norm, sin0, cos0, sin1, cos1, outerRadius, innerRadius, halfThick, r_final,
                                        g_final, b_final, a,
                                        1.0f, u0, u1, light, overlay);
                        
                        sin0 = sin1;
                        cos0 = cos1;
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

                // 側面 (外側)
                c.vertex(mat, ox0, oy0, -halfThick).color(r, g, b, a).uv(u0, 0).overlayCoords(overlay).uv2(light)
                                .normal(norm, 0, 1, 0).endVertex();
                c.vertex(mat, ox1, oy1, -halfThick).color(r, g, b, a).uv(u1, 0).overlayCoords(overlay).uv2(light)
                                .normal(norm, 0, 1, 0).endVertex();
                c.vertex(mat, ox1, oy1, halfThick).color(r, g, b, a).uv(u1, 1).overlayCoords(overlay).uv2(light)
                                .normal(norm, 0, 1, 0).endVertex();
                c.vertex(mat, ox0, oy0, halfThick).color(r, g, b, a).uv(u0, 1).overlayCoords(overlay).uv2(light)
                                .normal(norm, 0, 1, 0).endVertex();
        }

        private static void renderThrustWave(AttackEffectEntity entity, PoseStack poseStack,
                        MultiBufferSource bufferSource,
                        float partialTicks, float syncOffset) {
                float age = entity.tickCount + partialTicks + syncOffset;
                int color = entity.getTrailColor();
                float aBase = ((color >> 24) & 0xFF) / 255.0f;

                // あえて* 2.0fでオーバーフローを発生させてトレイルと色を完全に合わせる
                float r = ((color >> 16) & 0xFF) / 255.0f * 2.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f * 2.0f;
                float b = (color & 0xFF) / 255.0f * 2.0f;

                // 深度書き込みを無効化したTranslucent(entityNoOutline)を使用することで、
                // 色味(アルファ合成)を維持しつつ、重なりによるチラつき(Z-fighting)を完全に消し去る
                VertexConsumer c = bufferSource.getBuffer(RenderType.entityNoOutline(TEXTURE));
                Matrix4f mat = poseStack.last().pose();

                for (int ring = 0; ring < 3; ring++) {
                        float ringOffset = ring * 3.5f;
                        float cycleTicks = 12.0f;
                        float progress = ((age + ringOffset) % cycleTicks) / cycleTicks;

                        float radiusScale = entity.getEffectRadius().x();
                        float radius = (0.3f + progress * 2.8f) * radiusScale * 0.5f;
                        float alpha = aBase * (1.0f - (float) Math.pow(progress, 1.5));

                        if (alpha <= 0)
                                continue;

                        int segments = 64;

                        // 深度書き込みが無効なため、層を重ねてもチラつきません
                        float[] widthScales = { 1.0f, 1.15f, 1.3f };
                        float[] alphaScales = { 1.5f, 0.8f, 0.4f }; 
                        for (int pass = 0; pass < 3; pass++) {
                                float passAlpha = Math.min(alpha * alphaScales[pass], 1.0f);
                                float ws = widthScales[pass];
                                // 微小なオフセットだけで描画順序を安定させる
                                float zOffset = (ring * 0.01f) + (pass * 0.001f); 

                                for (int i = 0; i < segments; i++) {
                                        float sin0 = SIN_TABLE_64[i], cos0 = COS_TABLE_64[i];
                                        float sin1 = SIN_TABLE_64[i+1], cos1 = COS_TABLE_64[i+1];

                                        float x0 = sin0 * radius * ws, y0 = cos0 * radius * ws;
                                        float x1 = sin1 * radius * ws, y1 = cos1 * radius * ws;
                                        float ix0 = x0 * 0.90f, iy0 = y0 * 0.90f;
                                        float ix1 = x1 * 0.90f, iy1 = y1 * 0.90f;

                                        float u0 = (float) i / segments * 2.0f;
                                        float u1 = (float) (i + 1) / segments * 2.0f;
                                        float v_min = 0.0f;
                                        float v_max = 1.0f;

                                        c.vertex(mat, x0, y0, zOffset).color(r, g, b, passAlpha).uv(u0, v_min).overlayCoords(0)
                                                        .uv2(15728880).normal(0, 0, 1).endVertex();
                                        c.vertex(mat, x1, y1, zOffset).color(r, g, b, passAlpha).uv(u1, v_min).overlayCoords(0)
                                                        .uv2(15728880).normal(0, 0, 1).endVertex();
                                        c.vertex(mat, ix1, iy1, zOffset).color(r, g, b, passAlpha).uv(u1, v_max).overlayCoords(0)
                                                        .uv2(15728880).normal(0, 0, 1).endVertex();
                                        c.vertex(mat, ix0, iy0, zOffset).color(r, g, b, passAlpha).uv(u0, v_max).overlayCoords(0)
                                                        .uv2(15728880).normal(0, 0, 1).endVertex();
                                }
                        }
                }
        }

        private static void renderThrustBeam(AttackEffectEntity entity, PoseStack poseStack,
                        MultiBufferSource bufferSource,
                        float partialTicks, float r, float g, float b, float a) {
                float age = entity.tickCount + partialTicks;
                float progress = Math.min(age / 5.0f, 1.0f);
                float maxLength = entity.getEffectRadius().z();
                float currentLength = maxLength * progress;
                float baseWidth = 0.2f * entity.getEffectRadius().x();

                // 同様に深度書き込みなしのTranslucentを使用
                VertexConsumer c = bufferSource.getBuffer(RenderType.entityNoOutline(BEAM_TEXTURE)); 

                // 3層構造で厚塗りし、色味を最大化
                float[] widthMults = { 1.0f, 1.15f, 1.3f };
                float[] alphaMults = { 1.5f, 0.8f, 0.4f };

                for (int layer = 0; layer < 3; layer++) {
                        float layerA = Math.min(a * alphaMults[layer], 1.0f);
                        float layerWidth = baseWidth * widthMults[layer];
                        float layerOffset = layer * 0.001f; 

                        // 十字型のクアッドでビームを描画
                        for (int i = 0; i < 2; i++) {
                                poseStack.pushPose();
                                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 90));
                                Matrix4f m = poseStack.last().pose();

                                // 根元をシャープにする (layerWidth * 0.5f)
                                c.vertex(m, -layerWidth * 0.5f, layerOffset, -currentLength * 0.7f).color(r, g, b, layerA).uv(0, 0)
                                                .overlayCoords(0).uv2(15728880).normal(0, 1, 0).endVertex();
                                c.vertex(m, layerWidth * 0.5f, layerOffset, -currentLength * 0.7f).color(r, g, b, layerA).uv(1, 0)
                                                .overlayCoords(0).uv2(15728880).normal(0, 1, 0).endVertex();

                                // 先端
                                c.vertex(m, 0, layerOffset, currentLength).color(r, g, b, layerA).uv(1, 1).overlayCoords(0)
                                                .uv2(15728880).normal(0, 1, 0).endVertex();
                                c.vertex(m, 0, layerOffset, currentLength).color(r, g, b, layerA).uv(0, 1).overlayCoords(0)
                                                .uv2(15728880).normal(0, 1, 0).endVertex();

                                poseStack.popPose();
                        }
                }
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull AttackEffectEntity entity) {
                return TEXTURE;
        }
}
