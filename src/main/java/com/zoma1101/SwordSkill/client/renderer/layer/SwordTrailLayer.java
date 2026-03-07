package com.zoma1101.swordskill.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

public class SwordTrailLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final int DEFAULT_COLOR = 0xFF33AAFF;
    public static final net.minecraft.resources.ResourceLocation DEFAULT_TEXTURE = net.minecraft.resources.ResourceLocation
            .fromNamespaceAndPath("swordskill", "textures/entity/simple_2.png");

    public SwordTrailLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static void updateSkillSettings(UUID playerUUID, int skillId) {
        SwordTrailManager.updateSkillSettings(playerUUID, skillId);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            if (!skills.isTrailEnabled()) {
                SwordTrailManager.clear(player.getUUID());
                return;
            }

            // 攻撃アニメーション中かどうかを確認
            if (player.getAttackAnim(0) > 0) {
                // 自分の一人称視点の場合は Handler 側の RenderHandEvent で記録するため、ここではスキップ
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (player.getUUID().equals(mc.player.getUUID()) && mc.options.getCameraType().isFirstPerson()) {
                    return;
                }

                poseStack.pushPose();
                PlayerModel<AbstractClientPlayer> model = this.getParentModel();
                if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                    model.rightArm.translateAndRotate(poseStack);
                } else {
                    model.leftArm.translateAndRotate(poseStack);
                }

                SwordTrailRecorder.record(player, poseStack);

                poseStack.popPose();
            } else {
                SwordTrailLayer.TrailSession session = SwordTrailManager.getSession(player.getUUID());
                session.filterOldPoints();
            }
        });
    }

    public static void capturePoint(PoseStack poseStack, TrailSession session, double camX, double camY, double camZ,
            org.joml.Quaternionf camRot) {
        Matrix4f matrix = poseStack.last().pose();

        // 1. モデル空間での剣の先端と根本の座標 [0, offset, 0] を、現在の描画行列(Entity View Matrix)で変換
        // Vector4f.mul(matrix) は v * M になるため、M * v を行う matrix.transform(...) を使用する
        Vector4f baseView4 = matrix.transform(new Vector4f(0, session.trailBaseOffset, 0, 1));
        Vector4f tipView4 = matrix.transform(new Vector4f(0, session.trailTipOffset, 0, 1));

        org.joml.Vector3f baseView = new org.joml.Vector3f(baseView4.x(), baseView4.y(), baseView4.z());
        org.joml.Vector3f tipView = new org.joml.Vector3f(tipView4.x(), tipView4.y(), tipView4.z());

        // 2. カメラの回転(Orientation)を適用して、ビュー空間内の向きをワールド向きへ変換
        // カメラの向き = ワールドから見たカメラの回転。これをビュー空間のズレベクトルに適用する。
        org.joml.Quaternionf rot = new org.joml.Quaternionf(camRot);
        rot.transform(baseView);
        rot.transform(tipView);

        // 3. カメラの絶対位置を足して、完全な「世界座標」を求める
        TrailPoint newPoint = new TrailPoint(
                new Vector3f((float) (baseView.x + camX), (float) (baseView.y + camY), (float) (baseView.z + camZ)),
                new Vector3f((float) (tipView.x + camX), (float) (tipView.y + camY), (float) (tipView.z + camZ)));

        session.addPoint(newPoint);
    }

    public static void captureFirstPerson(TrailSession session, net.minecraft.client.Camera camera,
            net.minecraft.client.model.geom.ModelPart arm) {
        // 一人称では「エンティティレンダー空間の再構築」を行わず、
        // カメラ位置 + カメラ回転 + 腕ボーン回転で直接ワールド座標を計算する。

        // 1. カメラ空間での剣のグリップ基準位置（ItemInHandRenderer の配置に近い値）
        // 右手の場合: (右寄り, 下寄り, カメラの近く)
        float swingExtent = session.trailTipOffset - session.trailBaseOffset;
        org.joml.Vector3f baseView = new org.joml.Vector3f(0.4f, -0.3f, -0.4f);
        org.joml.Vector3f tipView = new org.joml.Vector3f(0.4f, -0.3f - swingExtent * 0.4f, -0.4f);

        // 2. 腕ボーンのアニメーション回転（Player Animator が設定したスウィングの動き）を適用
        org.joml.Quaternionf armRot = new org.joml.Quaternionf()
                .rotationXYZ(arm.xRot, arm.yRot, arm.zRot);
        armRot.transform(baseView);
        armRot.transform(tipView);

        // 3. カメラの回転でカメラ空間 → ワールド空間に変換
        org.joml.Quaternionf camRot = new org.joml.Quaternionf(camera.rotation());
        camRot.transform(baseView);
        camRot.transform(tipView);

        // 4. カメラのワールド座標を足して絶対世界座標にする
        TrailPoint newPoint = new TrailPoint(
                new Vector3f(
                        (float) camera.getPosition().x + baseView.x,
                        (float) camera.getPosition().y + baseView.y,
                        (float) camera.getPosition().z + baseView.z),
                new Vector3f(
                        (float) camera.getPosition().x + tipView.x,
                        (float) camera.getPosition().y + tipView.y,
                        (float) camera.getPosition().z + tipView.z));

        if (session.points.isEmpty() || session.points.getLast().base.distance(newPoint.base) > 0.02f) {
            session.points.add(newPoint);
        }
        if (session.points.size() > session.maxPoints) {
            session.points.removeFirst();
        }
    }

    public static void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource, TrailSession session,
            net.minecraft.client.Camera camera) {
        if (session.points.size() < 2)
            return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(session.texture));

        poseStack.pushPose();

        // ★ 高精度・高安定な描画のための行列再構築
        // RenderLevelStageEventのPoseStackを一度 Identity にリセットする。
        // その後、カメラの「回転」のみを適用する。
        poseStack.last().pose().identity();
        poseStack.mulPose(camera.rotation().conjugate());

        // カメラの現在地（絶対世界座標）をオフセットとして使用
        float cx = (float) camera.getPosition().x;
        float cy = (float) camera.getPosition().y;
        float cz = (float) camera.getPosition().z;

        Matrix4f worldViewMatrix = poseStack.last().pose();

        for (int i = 0; i < session.points.size() - 1; i++) {
            TrailPoint p1 = session.points.get(i);
            TrailPoint p2 = session.points.get(i + 1);

            float alphaIdx = (float) i / (session.points.size() - 1);
            float nextAlphaIdx = (float) (i + 1) / (session.points.size() - 1);

            // 絶対世界座標(p1/p2)からカメラ位置(cx)を差し引いてモデル空間へ
            drawQuad(consumer, worldViewMatrix, p1, p2, cx, cy, cz, alphaIdx, nextAlphaIdx, session.color, 1.0f, 1.5f);
            drawQuad(consumer, worldViewMatrix, p1, p2, cx, cy, cz, alphaIdx, nextAlphaIdx, session.color, 1.2f, 0.5f);
        }
        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, TrailPoint p1, TrailPoint p2, float cx,
            float cy, float cz, float alpha, float nextAlpha, int colorARGB, float widthScale, float alphaScale) {
        float a = ((colorARGB >> 24) & 0xFF) / 255.0f;
        float r = ((colorARGB >> 16) & 0xFF) / 255.0f * 2.0f; // ★輝度を2倍に。
        float g = ((colorARGB >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (colorARGB & 0xFF) / 255.0f * 2.0f;

        float alpha1 = a * (1.0f - alpha) * alphaScale;
        float alpha2 = a * (1.0f - nextAlpha) * alphaScale;

        // widthScale に基づいて位置をオフセット
        Vector3f p1Base = new Vector3f(p1.base);
        Vector3f p1Tip = new Vector3f(p1.base).lerp(p1.tip, widthScale);
        Vector3f p2Base = new Vector3f(p2.base);
        Vector3f p2Tip = new Vector3f(p2.base).lerp(p2.tip, widthScale);

        consumer.vertex(matrix, p1Base.x - cx, p1Base.y - cy, p1Base.z - cz).color(r, g, b, alpha1).uv(alpha, 1)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, p1Tip.x - cx, p1Tip.y - cy, p1Tip.z - cz).color(r, g, b, alpha1).uv(alpha, 0)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, p2Tip.x - cx, p2Tip.y - cy, p2Tip.z - cz).color(r, g, b, alpha2).uv(nextAlpha, 0)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, p2Base.x - cx, p2Base.y - cy, p2Base.z - cz).color(r, g, b, alpha2).uv(nextAlpha, 1)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
    }

    public static class TrailSession {
        public final UUID entityUUID;
        public LinkedList<TrailPoint> points = new LinkedList<>();
        public int color = DEFAULT_COLOR;
        public net.minecraft.resources.ResourceLocation texture = DEFAULT_TEXTURE;
        public int maxPoints = 15;
        public float trailBaseOffset = 0.6f;
        public float trailTipOffset = 1.4f;
        public String animationName = "";
        public float animationLength = 0.5f;

        public TrailSession(UUID uuid) {
            this.entityUUID = uuid;
        }

        public void addPoint(TrailPoint newPoint) {
            // FPS依存をなくすため、サイズ上限ではなく時間での消失（TrailPointの寿命）を採用
            // がくがくするのを防ぐため、距離閾値は極小(0.005)にしてほぼ全フレームをキャプチャする
            if (points.isEmpty() || points.getLast().base.distance(newPoint.base) > 0.005f) {
                points.add(newPoint);
            }

            filterOldPoints();
        }

        public void filterOldPoints() {
            long now = System.currentTimeMillis();
            long lifeTimeMs = (long) ((maxPoints / 60.0f) * 1000.0f);
            while (!points.isEmpty() && now - points.getFirst().timestamp > lifeTimeMs) {
                points.removeFirst();
            }
        }
    }

    public static class TrailPoint {
        public final Vector3f base;
        public final Vector3f tip;
        public final long timestamp;

        public TrailPoint(Vector3f base, Vector3f tip) {
            this.base = base;
            this.tip = tip;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
