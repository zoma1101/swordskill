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
            
            // アニメーションの状態を常に更新（activeがtrueの間）
            updateVisibilityFromAnimation(session);

            if (!session.active) {
                session.filterOldPoints();
                return;
            }

            // 三人称視点の記録処理
            // バニラのスイング中、またはカスタムアニメーションの再生時間内であれば記録を継続
            long elapsedMs = System.currentTimeMillis() - session.animStartMs;
            boolean animationRunning = session.animStartMs >= 0 && (elapsedMs < session.animationLength * 50L);

            if (player.getAttackAnim(0) > 0 || animationRunning) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (player.getUUID().equals(Objects.requireNonNull(mc.player).getUUID())
                        && mc.options.getCameraType().isFirstPerson()) {
                    return;
                }

                if (!session.visibility) {
                    session.filterOldPoints();
                    return;
                }

                poseStack.pushPose();
                PlayerModel<AbstractClientPlayer> model = this.getParentModel();

                // ボディの回転・移動を適用（屈みやアニメーションによる位置ズレを解消）
                model.body.translateAndRotate(poseStack);

                if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                    model.rightArm.translateAndRotate(poseStack);
                } else {
                    model.leftArm.translateAndRotate(poseStack);
                }

                // 三人称視点でも rightItem のアニメーション情報を適用する
                if (session.animStartMs >= 0) {
                    float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
                    if (t <= session.animationLength) {
                        org.joml.Vector3f itemRotDeg = session.itemRotTrack != null ? session.itemRotTrack.evaluate(t)
                                : new org.joml.Vector3f();
                        org.joml.Vector3f itemPosPx = session.itemPosTrack != null ? session.itemPosTrack.evaluate(t)
                                : new org.joml.Vector3f();

                        // 1. 腕の付け根（肩）から先端（手）の位置付近へ移動 (約12ピクセル = 0.75ブロック)
                        poseStack.translate(0f, 0.7f, 0.0625f);

                        // 2. アニメーションの位置オフセットを適用
                        poseStack.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);

                        // 3. アニメーションの回転を適用
                        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-itemRotDeg.x - 90));
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-itemRotDeg.y));
                        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(itemRotDeg.z));

                        // 4. トレイル生成の基準点(trailBaseOffset)を考慮して座標を戻す
                        poseStack.translate(0f, -session.trailBaseOffset, 0f);
                    }
                }

                SwordTrailRecorder.record(player, poseStack);
                poseStack.popPose();
            } else {
                session.filterOldPoints();
            }
        });
    }

    /**
     * 指定されたポーズスタックの状態からトレイルのポイントを生成・キャプチャします。
     */
    public static void capturePoint(PoseStack poseStack, TrailSession session, double camX, double camY, double camZ,
            org.joml.Quaternionf camRot) {
        Matrix4f matrix = poseStack.last().pose();
        int count = session.pointCount;
        Vector3f[] worldPositions = new Vector3f[count];

        for (int i = 0; i < count; i++) {
            Vector3f localPos = new Vector3f(session.getLocalPoint(i));
            
            // スケール適用
            localPos.x *= session.curWidthScaleX;
            localPos.z *= session.curWidthScaleZ;
            float totalLen = session.trailTipOffset - session.trailBaseOffset;
            float progress = (float)i / (count - 1);
            localPos.y = session.trailBaseOffset + (totalLen * progress * session.curLengthScale);

            Vector4f view4 = matrix.transform(new Vector4f(localPos.x, localPos.y, localPos.z, 1.0f));
            Vector3f view3 = new Vector3f(view4.x(), view4.y(), view4.z());

            // カメラの回転を適用して、ビュー空間から世界相対空間（カメラからの向き）へ変換
            org.joml.Quaternionf rot = new org.joml.Quaternionf(camRot);
            rot.transform(view3);

            // カメラの絶対座標を足して世界絶対座標にする
            worldPositions[i] = new Vector3f((float) (view3.x + camX), (float) (view3.y + camY),
                    (float) (view3.z + camZ));
        }

        TrailPoint newPoint = new TrailPoint(worldPositions, new Vector3f((float) camX, (float) camY, (float) camZ),
                new org.joml.Quaternionf(camRot));
        session.addPoint(newPoint);
    }

    public static void captureFirstPersonFromKeyframe(TrailSession session,
            net.minecraft.world.entity.player.Player player,
            float playerYaw) {

        session.isFirstPerson = true;

        if (session.animStartMs < 0)
            return;

        float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
        if (t > session.animationLength)
            return;

        // アニメーションのキーフレームに従って visibility を更新
        updateVisibilityFromAnimation(session);

        if (!session.visibility)
            return;

        Vector3f armRotDeg = session.armRotTrack != null ? session.armRotTrack.evaluate(t) : new Vector3f();
        Vector3f armPosPx = session.armPosTrack != null ? session.armPosTrack.evaluate(t) : new Vector3f();
        Vector3f bodyRotDeg = session.bodyRotTrack != null ? session.bodyRotTrack.evaluate(t) : new Vector3f();
        Vector3f bodyPosPx = session.bodyPosTrack != null ? session.bodyPosTrack.evaluate(t) : new Vector3f();
        Vector3f itemRotDeg = session.itemRotTrack != null ? session.itemRotTrack.evaluate(t) : new Vector3f();
        Vector3f itemPosPx = session.itemPosTrack != null ? session.itemPosTrack.evaluate(t) : new Vector3f();

        Matrix4f matrix = getMatrix4f(bodyRotDeg, bodyPosPx, armRotDeg, armPosPx, itemRotDeg, itemPosPx);

        float yawRad = (float) Math.toRadians(playerYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        float cx = (float) org.joml.Math.lerp(player.xOld, player.getX(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick());
        float cy = (float) org.joml.Math.lerp(player.yOld, player.getY(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick()) + player.getEyeHeight();
        float cz = (float) org.joml.Math.lerp(player.zOld, player.getZ(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick());

        int count = session.pointCount;
        Vector3f[] worldPositions = new Vector3f[count];

        // --- 刺突（スラスト）強化ロジック ---
        Vector3f currentOrigin = new Vector3f(0, 0, 0);
        matrix.transformPosition(currentOrigin);

        float thrustOffset = 0f;
        if (session.lastOrigin != null) {
            Vector3f velocity = new Vector3f(currentOrigin).sub(session.lastOrigin);
            if (velocity.z < -0.01f) {
                thrustOffset = -velocity.z * 1.5f;
            }
        }
        session.lastOrigin = currentOrigin;

        for (int i = 0; i < count; i++) {
            Vector3f local = new Vector3f(session.getLocalPoint(i));

            local.rotateX((float) Math.toRadians(-25));

            local.y = (local.y - session.trailTipOffset) * session.trailLengthScale * session.curLengthScale;
            local.x = local.x * session.trailLengthScale * session.curWidthScaleX;
            local.z = local.z * session.trailLengthScale * session.curWidthScaleZ;

            local.z -= thrustOffset;

            matrix.transformPosition(local);

            float wx = cos * local.x + sin * local.z;
            float wz = sin * local.x - cos * local.z;

            worldPositions[i] = new Vector3f(cx + wx, cy + local.y, cz + wz);
        }

        TrailPoint newPoint = new TrailPoint(worldPositions, new org.joml.Vector3f(cx, cy, cz),
                new org.joml.Quaternionf().rotationY(-yawRad));
        session.addPoint(newPoint);
    }

    private static @NotNull Matrix4f getMatrix4f(Vector3f bodyRotDeg, Vector3f bodyPosPx, Vector3f armRotDeg,
            Vector3f armPosPx, Vector3f itemRotDeg, Vector3f itemPosPx) {
        Matrix4f matrix = new Matrix4f();
        // ボディ変換 (body)
        matrix.translate(0f, -0.75f, 0f); // ボディの中心
        matrix.rotateY((float) Math.toRadians(bodyRotDeg.y));
        matrix.rotateX((float) Math.toRadians(bodyRotDeg.x));
        matrix.rotateZ((float) Math.toRadians(bodyRotDeg.z));
        matrix.translate(bodyPosPx.x / 16f, bodyPosPx.y / 16f, bodyPosPx.z / 16f);

        // 腕変換 (right_arm)
        matrix.translate(0f, 0.625f, 0f); // 腕の付け根
        matrix.rotateZ((float) Math.toRadians(armRotDeg.z));
        matrix.rotateY((float) Math.toRadians(armRotDeg.y));
        matrix.rotateX((float) Math.toRadians(armRotDeg.x));
        matrix.translate(armPosPx.x / 16f, armPosPx.y / 16f, armPosPx.z / 16f);

        // 手のひら・アイテム変換 (rightItem)
        matrix.translate(0f, -0.5625f, 0.0625f); // 腕の先（手）
        matrix.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);
        matrix.rotateX((float) Math.toRadians(-itemRotDeg.x - 90));
        matrix.rotateY((float) Math.toRadians(-itemRotDeg.y));
        matrix.rotateZ((float) Math.toRadians(itemRotDeg.z));

        return matrix;
    }

    public static void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource, TrailSession session) {
        if (session.points.size() < 4)
            return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(session.texture));
        poseStack.pushPose();
        poseStack.last().pose().identity();
        Matrix4f worldViewMatrix = poseStack.last().pose();

        for (int i = 0; i < session.points.size() - 1; i++) {
            TrailPoint p1 = session.points.get(i);
            TrailPoint p2 = session.points.get(i + 1);

            // 新しいセグメントの開始点（OFF -> ON 切り替え）の場合は前の点とつながない
            if (p2.isNewSegment)
                continue;

            float alphaIdx = (float) i / (session.points.size() - 1);
            float nextAlphaIdx = (float) (i + 1) / (session.points.size() - 1);

            // ポイント数（セグメント数）に応じて接続面を描画
            for (int j = 0; j < session.pointCount - 1; j++) {
                // 外側のオーラ（薄め）
                drawSegmentQuad(consumer, worldViewMatrix, p1, p2, j, alphaIdx, nextAlphaIdx, session.color, 1.2f,
                        0.4f, session.isFirstPerson);
                // 内側の芯（濃いめ）
                drawSegmentQuad(consumer, worldViewMatrix, p1, p2, j, alphaIdx, nextAlphaIdx, session.color, 1.0f,
                        1.0f, session.isFirstPerson);
            }
        }
        poseStack.popPose();
    }

    private static void drawSegmentQuad(VertexConsumer consumer, Matrix4f matrix, TrailPoint p1, TrailPoint p2,
            int segmentIdx, float alpha, float nextAlpha, int colorARGB, float widthScale, float alphaScale,
            boolean isFirstPerson) {
        float a = ((colorARGB >> 24) & 0xFF) / 255.0f;
        float r = ((colorARGB >> 16) & 0xFF) / 255.0f * 2.0f;
        float g = ((colorARGB >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (colorARGB & 0xFF) / 255.0f * 2.0f;

        float alpha1 = a * (1.0f - alpha) * alphaScale;
        float alpha2 = a * (1.0f - nextAlpha) * alphaScale;

        // Base を基準に widthScale 分だけ縮小（先端方向へのLerp）
        Vector3f base1 = p1.positions[0];
        Vector3f pos1_v1 = new Vector3f(base1).lerp(p1.positions[segmentIdx], widthScale);
        Vector3f pos1_v2 = new Vector3f(base1).lerp(p1.positions[segmentIdx + 1], widthScale);

        Vector3f base2 = p2.positions[0];
        Vector3f pos2_v3 = new Vector3f(base2).lerp(p2.positions[segmentIdx + 1], widthScale);
        Vector3f pos2_v4 = new Vector3f(base2).lerp(p2.positions[segmentIdx], widthScale);

        Vector3f v1 = toViewSpace(pos1_v1, p1.camPos, p1.camRot, isFirstPerson);
        Vector3f v2 = toViewSpace(pos1_v2, p1.camPos, p1.camRot, isFirstPerson);
        Vector3f v3 = toViewSpace(pos2_v3, p2.camPos, p2.camRot, isFirstPerson);
        Vector3f v4 = toViewSpace(pos2_v4, p2.camPos, p2.camRot, isFirstPerson);

        float u1 = alpha;
        float u2 = nextAlpha;
        float v_min = (float) segmentIdx / (p1.positions.length - 1);
        float v_max = (float) (segmentIdx + 1) / (p1.positions.length - 1);

        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

        consumer.vertex(matrix, v1.x, v1.y, v1.z).color(r, g, b, alpha1).uv(u1, v_max).overlayCoords(overlay)
                .uv2(15728880).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, v2.x, v2.y, v2.z).color(r, g, b, alpha1).uv(u1, v_min).overlayCoords(overlay)
                .uv2(15728880).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, v3.x, v3.y, v3.z).color(r, g, b, alpha2).uv(u2, v_min).overlayCoords(overlay)
                .uv2(15728880).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, v4.x, v4.y, v4.z).color(r, g, b, alpha2).uv(u2, v_max).overlayCoords(overlay)
                .uv2(15728880).normal(0, 1, 0).endVertex();
    }

    private static void updateVisibilityFromAnimation(TrailSession session) {
        if (session.animStartMs < 0) return;

        float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;

        // 表示・非表示トラック (trail.pos.x)
        if (session.trailTrack != null && !session.trailTrack.isEmpty()) {
            Vector3f val = session.trailTrack.evaluate(t);
            boolean newState = (val.x > -0.5f);
            if (!session.visibility && newState) {
                session.pendingNewSegment = true;
            }
            session.visibility = newState;
        } else {
            session.visibility = true;
        }

        // 幅・長さスケールトラック (trail.rot rx, ry, rz)
        if (session.trailRotTrack != null && !session.trailRotTrack.isEmpty()) {
            Vector3f rotVal = session.trailRotTrack.evaluate(t);
            // rx=横幅, ry=立幅, rz=長さ。 0の場合はデフォルト1.0
            session.curWidthScaleX = rotVal.x != 0 ? rotVal.x : 1.0f;
            session.curWidthScaleZ = rotVal.y != 0 ? rotVal.y : 1.0f;
            session.curLengthScale = rotVal.z != 0 ? rotVal.z : 1.0f;
        } else {
            session.curWidthScaleX = 1.0f;
            session.curWidthScaleZ = 1.0f;
            session.curLengthScale = 1.0f;
        }
    }

    private static Vector3f toViewSpace(Vector3f worldPos, Vector3f camPos, org.joml.Quaternionf camRot,
            boolean isFirstPerson) {
        Vector3f rel = new Vector3f(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
        if (isFirstPerson) {
            // 一人称のときだけ、仮想的な視点位置をずらして投影することで
            // トレイルの発生位置と疑似的な立体感を調整します
            
            // X: 左右 (プラスで右へ)
            // Y: 上下 (プラスで上へ)
            // Z: 奥/手前 (プラスで手前へ、マイナスで前方へ)
            // ユーザーの要望により1ブロック分前方 (-1.0f) にオフセット
            Vector3f viewOffset = new Vector3f(0.12f, 0.12f, 0.35f);

            new org.joml.Quaternionf(camRot).transform(viewOffset);
            rel.sub(viewOffset);
        }
        new org.joml.Quaternionf(camRot).conjugate().transform(rel);
        return rel;
    }

    public static class TrailSession {
        public final UUID entityUUID;
        public LinkedList<TrailPoint> points = new LinkedList<>();

        public int color = DEFAULT_COLOR;
        public net.minecraft.resources.ResourceLocation texture = DEFAULT_TEXTURE;
        public int maxPoints = 25;
        public float trailBaseOffset = 0.6f;
        public float trailTipOffset = 1.4f;
        public float trailLengthScale = 0.55f;
        public int pointCount = 2; // デフォルトは直線(2点)
        public float arcAngle = 0f; // 円弧の角度
        public boolean active = true;
        public boolean isFirstPerson = false; // 一人称用トレイル(カメラに追従)かどうか

        public String animationName = "";
        public float animationLength = 0.5f;
        public long animStartMs = -1L;

        public AnimationKeyframeTrack armRotTrack = null;
        public AnimationKeyframeTrack armPosTrack = null;
        public AnimationKeyframeTrack bodyRotTrack = null;
        public AnimationKeyframeTrack bodyPosTrack = null;
        public AnimationKeyframeTrack itemRotTrack = null;
        public AnimationKeyframeTrack itemPosTrack = null;
        public AnimationKeyframeTrack trailTrack = null;
        public AnimationKeyframeTrack trailRotTrack = null;
        public boolean pendingNewSegment = false;
        public boolean visibility = true; // アニメーション内での表示スイッチ

        public float curWidthScaleX = 1.0f;
        public float curWidthScaleZ = 1.0f;
        public float curLengthScale = 1.0f;

        public Vector3f lastOrigin = null; // 前回のフレームの手の原点位置（ブースト計算用）

        public TrailSession(UUID uuid) {
            this.entityUUID = uuid;
        }

        /**
         * セッションの設定に基づいて、ローカル空間での頂点座標を返します。
         */
        public Vector3f getLocalPoint(int index) {
            if (pointCount <= 1)
                return new Vector3f(0, trailBaseOffset, 0);

            float progress = (float) index / (pointCount - 1);
            float totalLength = trailTipOffset - trailBaseOffset;

            if (arcAngle > 0) {
                // 円弧状に配置
                float angleRad = (float) Math.toRadians(arcAngle);
                float currentAngle = (progress - 0.5f) * angleRad;
                float radius = totalLength / angleRad;
                return new Vector3f(
                        (float) Math.sin(currentAngle) * radius,
                        (float) Math.cos(currentAngle) * radius + trailBaseOffset - radius,
                        0);
            } else {
                // 直線状に配置
                return new Vector3f(0, trailBaseOffset + totalLength * progress, 0);
            }
        }

        public void addPoint(TrailPoint newPoint) {
            if (pendingNewSegment) {
                newPoint.isNewSegment = true;
                pendingNewSegment = false;
            }
            if (points.isEmpty() || points.getLast().positions[0].distance(newPoint.positions[0]) > 0.005f) {
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
        public final Vector3f[] positions;
        public final long timestamp;
        public final Vector3f camPos;
        public final org.joml.Quaternionf camRot;
        public boolean isNewSegment = false;

        public TrailPoint(Vector3f[] positions, Vector3f camPos, org.joml.Quaternionf camRot) {
            this.positions = positions;
            this.camPos = camPos;
            this.camRot = camRot;
            this.timestamp = System.currentTimeMillis();
        }
    }
}