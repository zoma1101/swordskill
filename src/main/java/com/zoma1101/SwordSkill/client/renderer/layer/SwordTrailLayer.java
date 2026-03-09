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
import org.jetbrains.annotations.NotNull;
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
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            if (!skills.isTrailEnabled()) {
                SwordTrailManager.clear(player.getUUID());
                return;
            }

            SwordTrailLayer.TrailSession session = SwordTrailManager.getSession(player.getUUID());
            if (!session.active) {
                session.filterOldPoints();
                return;
            }

            if (player.getAttackAnim(0) > 0) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (player.getUUID().equals(Objects.requireNonNull(mc.player).getUUID())
                        && mc.options.getCameraType().isFirstPerson()) {
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
                session.filterOldPoints();
            }
        });
    }

    public static void capturePoint(PoseStack poseStack, TrailSession session, double camX, double camY, double camZ,
            org.joml.Quaternionf camRot) {
        Matrix4f matrix = poseStack.last().pose();

        // poseStackはカメラ相対空間で構築されているため
        // camRotでワールド向きに回転してからカメラ位置を加算する
        Vector4f baseView4 = matrix.transform(new Vector4f(0, session.trailBaseOffset, 0, 1));
        Vector4f tipView4 = matrix.transform(new Vector4f(0, session.trailTipOffset, 0, 1));

        org.joml.Vector3f baseView = new org.joml.Vector3f(baseView4.x(), baseView4.y(), baseView4.z());
        org.joml.Vector3f tipView = new org.joml.Vector3f(tipView4.x(), tipView4.y(), tipView4.z());

        org.joml.Quaternionf rot = new org.joml.Quaternionf(camRot);
        rot.transform(baseView);
        rot.transform(tipView);

        TrailPoint newPoint = new TrailPoint(
                new Vector3f((float) (baseView.x + camX), (float) (baseView.y + camY), (float) (baseView.z + camZ)),
                new Vector3f((float) (tipView.x + camX), (float) (tipView.y + camY), (float) (tipView.z + camZ)),
                new Vector3f((float) camX, (float) camY, (float) camZ),
                new org.joml.Quaternionf(camRot));

        session.addPoint(newPoint);
    }

    /**
     * 一人称視点用トレイル座標計算。
     *
     * @param session   対象セッション
     * @param player    ローカルプレイヤー（partialTicks補間位置の取得に使用）
     * @param playerYaw プレイヤーの現在yaw（度）
     */
    public static void captureFirstPersonFromKeyframe(TrailSession session,
            net.minecraft.world.entity.player.Player player,
            float playerYaw) {

        if (session.animStartMs < 0)
            return;

        float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
        if (t > session.animationLength)
            return;

        Vector3f armRotDeg = session.armRotTrack != null ? session.armRotTrack.evaluate(t) : new Vector3f();
        Vector3f armPosPx = session.armPosTrack != null ? session.armPosTrack.evaluate(t) : new Vector3f();
        Vector3f bodyRotDeg = session.bodyRotTrack != null ? session.bodyRotTrack.evaluate(t) : new Vector3f();

        Matrix4f matrix = getMatrix4f(bodyRotDeg, armRotDeg, armPosPx);

        float span = (session.trailTipOffset - session.trailBaseOffset) * session.trailLengthScale;
        Vector3f baseVS = new Vector3f(0f, -span, 0f);
        Vector3f tipVS = new Vector3f(0f, 0f, 0f);

        matrix.transformPosition(baseVS);
        matrix.transformPosition(tipVS);

        // --- ここから視点方向へのずらし処理 ---
        // FORWARD_OFFSET: 前方にどれくらい出すか（0.1f〜0.2f程度が自然です）

        // --- 手動XZ回転計算 ---
        float yawRad = (float) Math.toRadians(playerYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        float worldBaseX = cos * baseVS.x + sin * baseVS.z;
        float worldBaseZ = sin * baseVS.x - cos * baseVS.z;
        float worldTipX = cos * tipVS.x + sin * tipVS.z;
        float worldTipZ = sin * tipVS.x - cos * tipVS.z;

        // --- 最終座標確定（cx, cy, cz に視点オフセット ox, oy, oz を加算） ---
        float partialTick = net.minecraft.client.Minecraft.getInstance().getPartialTick();
        float cx = (float) org.joml.Math.lerp(player.xOld, player.getX(), partialTick);
        float cy = (float) org.joml.Math.lerp(player.yOld, player.getY(), partialTick) + player.getEyeHeight();
        float cz = (float) org.joml.Math.lerp(player.zOld, player.getZ(), partialTick);

        org.joml.Vector3f recCamPos = new org.joml.Vector3f(cx, cy, cz);
        org.joml.Quaternionf recCamRot = new org.joml.Quaternionf().rotationY(-yawRad);

        TrailPoint newPoint = new TrailPoint(
                new Vector3f(cx + worldBaseX, cy + baseVS.y, cz + worldBaseZ),
                new Vector3f(cx + worldTipX, cy + tipVS.y, cz + worldTipZ),
                recCamPos,
                recCamRot);

        session.addPoint(newPoint);
    }

    private static @NotNull Matrix4f getMatrix4f(Vector3f bodyRotDeg, Vector3f armRotDeg, Vector3f armPosPx) {
        Matrix4f matrix = new Matrix4f();
        matrix.translate(0f, -0.75f, 0f);
        matrix.rotateY((float) Math.toRadians(bodyRotDeg.y));
        matrix.translate(0f, 0.625f, 0f);
        matrix.rotateZ((float) Math.toRadians(armRotDeg.z));
        matrix.rotateY((float) Math.toRadians(armRotDeg.y));
        matrix.rotateX((float) Math.toRadians(armRotDeg.x));
        matrix.translate(armPosPx.x / 16f, armPosPx.y / 16f, armPosPx.z / 16f);
        matrix.translate(0f, -0.5625f, 0.0625f);
        return matrix;
    }

    public static void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource, TrailSession session) {
        if (session.points.size() < 2)
            return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(session.texture));

        poseStack.pushPose();

        // 各TrailPointが記録時のcamPos/camRotを持つため、
        // poseStackはidentityのみ。conjugateは drawQuad 内で各点ごとに適用する。
        poseStack.last().pose().identity();

        Matrix4f worldViewMatrix = poseStack.last().pose();

        for (int i = 0; i < session.points.size() - 1; i++) {
            TrailPoint p1 = session.points.get(i);
            TrailPoint p2 = session.points.get(i + 1);

            float alphaIdx = (float) i / (session.points.size() - 1);
            float nextAlphaIdx = (float) (i + 1) / (session.points.size() - 1);

            drawQuad(consumer, worldViewMatrix, p1, p2, alphaIdx, nextAlphaIdx, session.color, 1.0f, 1.5f);
            drawQuad(consumer, worldViewMatrix, p1, p2, alphaIdx, nextAlphaIdx, session.color, 1.2f, 0.5f);
        }
        poseStack.popPose();
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f matrix, TrailPoint p1, TrailPoint p2,
            float alpha, float nextAlpha, int colorARGB, float widthScale, float alphaScale) {
        float a = ((colorARGB >> 24) & 0xFF) / 255.0f;
        float r = ((colorARGB >> 16) & 0xFF) / 255.0f * 2.0f;
        float g = ((colorARGB >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (colorARGB & 0xFF) / 255.0f * 2.0f;

        float alpha1 = a * (1.0f - alpha) * alphaScale;
        float alpha2 = a * (1.0f - nextAlpha) * alphaScale;

        // 各点を記録時カメラのビュー空間に変換:
        // viewPos = recordedCamRot.conjugate × (worldPos - recordedCamPos)
        // 一人称・三人称ともに同じ処理。TrailPointに必ずcamPos/camRotが入っている前提。
        Vector3f b1 = toViewSpace(p1.base, p1.camPos, p1.camRot);
        Vector3f t1 = toViewSpace(new Vector3f(p1.base).lerp(p1.tip, widthScale), p1.camPos, p1.camRot);
        Vector3f b2 = toViewSpace(p2.base, p2.camPos, p2.camRot);
        Vector3f t2 = toViewSpace(new Vector3f(p2.base).lerp(p2.tip, widthScale), p2.camPos, p2.camRot);

        consumer.vertex(matrix, b1.x, b1.y, b1.z).color(r, g, b, alpha1).uv(alpha, 1)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, t1.x, t1.y, t1.z).color(r, g, b, alpha1).uv(alpha, 0)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, t2.x, t2.y, t2.z).color(r, g, b, alpha2).uv(nextAlpha, 0)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, b2.x, b2.y, b2.z).color(r, g, b, alpha2).uv(nextAlpha, 1)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(15728880)
                .normal(0, 1, 0).endVertex();
    }

    private static Vector3f toViewSpace(Vector3f worldPos, Vector3f camPos, org.joml.Quaternionf camRot) {
        Vector3f rel = new Vector3f(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
        // これを有効にするだけで劇的に改善するはずです
        new org.joml.Quaternionf(camRot).conjugate().transform(rel);
        return rel;
    }

    public static class TrailSession {
        public final UUID entityUUID;
        public LinkedList<TrailPoint> points = new LinkedList<>();

        public int color = DEFAULT_COLOR;
        public net.minecraft.resources.ResourceLocation texture = DEFAULT_TEXTURE;
        public int maxPoints = 15;
        public float trailBaseOffset = 0.6f;
        public float trailTipOffset = 1.4f;
        public float trailLengthScale = 0.55f;
        public boolean active = true;

        public String animationName = "";
        public float animationLength = 0.5f;
        public long animStartMs = -1L;

        public AnimationKeyframeTrack armRotTrack = null;
        public AnimationKeyframeTrack armPosTrack = null;
        public AnimationKeyframeTrack bodyRotTrack = null;

        public TrailSession(UUID uuid) {
            this.entityUUID = uuid;
        }

        public void addPoint(TrailPoint newPoint) {
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
        /** 記録時のカメラ位置・回転。一人称・三人称共通で描画時の変換に使用。 */
        public final Vector3f camPos;
        public final org.joml.Quaternionf camRot;

        public TrailPoint(Vector3f base, Vector3f tip, Vector3f camPos, org.joml.Quaternionf camRot) {
            this.base = base;
            this.tip = tip;
            this.camPos = camPos;
            this.camRot = camRot;
            this.timestamp = System.currentTimeMillis();
        }
    }
}