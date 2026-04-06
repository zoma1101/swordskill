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

    public static net.minecraft.resources.ResourceLocation TEST_TEXTURE = null;

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    // 低FPSモード判定用: 瞬間値ではなく平滑化FPS＋ヒステリシスを使用して切り替えタイミングのブレを抑える
    private static float smoothedFps = 60.0f;
    private static boolean lowFpsMode = false;
    /** EMAの平滑化係数: 大きいほど反応が速い（0.0〜1.0） */
    private static final float FPS_SMOOTH = 0.15f;
    /** この平滑FPS以下になったら低FPSモードに入る */
    private static final float LOW_FPS_ENTER = 27.0f;
    /** この平滑FPS以上になったら通常モードに戻る（ヒステリシス） */
    private static final float LOW_FPS_EXIT  = 33.0f;

    private static class Scratch {
        final Vector3f v1 = new Vector3f();
        final Vector3f v2 = new Vector3f();
        final Vector3f v3 = new Vector3f();
        final Vector3f v4 = new Vector3f();
        final Vector3f v5 = new Vector3f();
        final Vector3f v6 = new Vector3f();
        final Vector3f v7 = new Vector3f();
        final Vector3f v8 = new Vector3f();
        final Vector3f v9 = new Vector3f();
        final Vector3f v10 = new Vector3f();
        final Vector3f v11 = new Vector3f();
        final Vector3f v12 = new Vector3f();
        final Vector3f res1 = new Vector3f();
        final Vector3f res2 = new Vector3f();
        final Vector3f res3 = new Vector3f();
        final Vector3f res4 = new Vector3f();
        final Vector4f v4f = new Vector4f();
        final Matrix4f mat = new Matrix4f();
        final org.joml.Quaternionf quat = new org.joml.Quaternionf();
    }

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
            
            updateVisibilityFromAnimation(session);

            if (!session.active) {
                session.filterOldPoints();
                return;
            }

            long elapsedMs = System.currentTimeMillis() - session.animStartMs;
            boolean animationRunning = session.animStartMs >= 0 && (elapsedMs < session.animationLength * 50L);

            if (animationRunning) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (player.getUUID().equals(Objects.requireNonNull(mc.player).getUUID())
                        && mc.options.getCameraType().isFirstPerson()) {
                    return;
                }

                if (!session.visibility) {
                    session.filterOldPoints();
                    return;
                }

                // Oculus(Iris)のシャドウパスを正確に検出してスキップする
                if (isOculusShadowPass()) {
                    return;
                }

                // フレーム番号による記録ガードを廃止。
                // シャドウパスが弾かれるため、複数パスが呼ばれても同じカメラ位置であれば
                // 距離デデュプ(>0.005f)で自然に重複が間引かれます。

                PlayerModel<AbstractClientPlayer> model = this.getParentModel();

                poseStack.pushPose();
                model.body.translateAndRotate(poseStack);
                switch (session.followBone) {
                    case MAIN_HAND -> {
                        if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) model.rightArm.translateAndRotate(poseStack);
                        else model.leftArm.translateAndRotate(poseStack);
                    }
                    case OFF_HAND -> {
                        if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) model.leftArm.translateAndRotate(poseStack);
                        else model.rightArm.translateAndRotate(poseStack);
                    }
                    case RIGHT_HAND, RIGHT_ARM -> model.rightArm.translateAndRotate(poseStack);
                    case LEFT_HAND, LEFT_ARM -> model.leftArm.translateAndRotate(poseStack);
                    case RIGHT_LEG -> model.rightLeg.translateAndRotate(poseStack);
                    case LEFT_LEG -> model.leftLeg.translateAndRotate(poseStack);
                    case BOTH_LEGS -> model.rightLeg.translateAndRotate(poseStack);
                    case HEAD -> model.head.translateAndRotate(poseStack);
                }
                applyItemAnimation(poseStack, session, session.followBone, false);
                SwordTrailRecorder.record(player, poseStack, false);
                poseStack.popPose();

                if (session.isDual || session.followBone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.BOTH_LEGS) {
                    poseStack.pushPose();
                    model.body.translateAndRotate(poseStack);
                    if (session.followBone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.BOTH_LEGS) {
                        model.leftLeg.translateAndRotate(poseStack);
                        applyItemAnimation(poseStack, session, com.zoma1101.swordskill.swordskills.SkillData.FollowBone.LEFT_LEG, true);
                    } else {
                        if (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                            model.leftArm.translateAndRotate(poseStack);
                        } else {
                            model.rightArm.translateAndRotate(poseStack);
                        }
                        applyItemAnimation(poseStack, session, com.zoma1101.swordskill.swordskills.SkillData.FollowBone.OFF_HAND, true);
                    }
                    SwordTrailRecorder.record(player, poseStack, true);
                    poseStack.popPose();
                }
            } else {
                session.filterOldPoints();
            }
        });
    }

    private static void applyItemAnimation(PoseStack poseStack, TrailSession session, com.zoma1101.swordskill.swordskills.SkillData.FollowBone bone, boolean isLeft) {
        if (session.animStartMs >= 0) {
            float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
            if (t <= session.animationLength / 20.0f) {
                AnimationKeyframeTrack rotTrack = null;
                AnimationKeyframeTrack posTrack = null;

                // ボーンの種類に応じてトラックを選択
                switch (bone) {
                    case MAIN_HAND, RIGHT_HAND, RIGHT_ARM -> {
                        rotTrack = session.itemRotTrack;
                        posTrack = session.itemPosTrack;
                    }
                    case OFF_HAND, LEFT_HAND, LEFT_ARM -> {
                        rotTrack = isLeft ? session.leftItemRotTrack : session.itemRotTrack;
                        posTrack = isLeft ? session.leftItemPosTrack : session.itemPosTrack;
                    }
                    case RIGHT_LEG -> {
                        rotTrack = session.rightLegRotTrack;
                        posTrack = session.rightLegPosTrack;
                    }
                    case LEFT_LEG -> {
                        rotTrack = session.leftLegRotTrack;
                        posTrack = session.leftLegPosTrack;
                    }
                    case BOTH_LEGS -> {
                        rotTrack = isLeft ? session.leftLegRotTrack : session.rightLegRotTrack;
                        posTrack = isLeft ? session.leftLegPosTrack : session.rightLegPosTrack;
                    }
                    case HEAD -> {
                        // 頭部用のトラックがあればここで設定可能
                    }
                }
                
                Scratch s = SCRATCH.get();
                Vector3f itemRotDeg = s.v5.set(0);
                if (rotTrack != null) rotTrack.evaluate(t, itemRotDeg);
                Vector3f itemPosPx = s.v6.set(0);
                if (posTrack != null) posTrack.evaluate(t, itemPosPx);

                poseStack.translate(0f, 0.7f, 0.0625f);

                // ★追加: 一人称視点かつ足の場合に少し上に持ち上げて見やすくする
                if (net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    if (bone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.RIGHT_LEG ||
                        bone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.LEFT_LEG ||
                        bone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.BOTH_LEGS) {
                        poseStack.translate(0f, 0.5f, 0f); // 0.5fほど上にずらす
                    }
                }

                poseStack.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-itemRotDeg.x - 90));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-itemRotDeg.y));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(itemRotDeg.z));
                poseStack.translate(0f, -session.trailBaseOffset, 0f);
            }
        }
    }

    public static void capturePoint(PoseStack poseStack, TrailSession session, double camX, double camY, double camZ,
            org.joml.Quaternionf camRot, boolean isLeft) {
        Matrix4f matrix = poseStack.last().pose();
        int count = session.pointCount;
        Vector3f[] worldPositions = new Vector3f[count];
        Vector3f[] positionsLeft = null;
        Vector3f[] positionsRight = null;
        if (session.isClaw) {
            positionsLeft = new Vector3f[count];
            positionsRight = new Vector3f[count];
        }

        Scratch s = SCRATCH.get();
        for (int i = 0; i < count; i++) {
            session.getLocalPoint(i, s.v1);
            Vector3f localPos = s.v1;

            if (session.trailScaleTrack != null && !session.trailScaleTrack.isEmpty()) {
                // trail.rotationトラックを本来の「回転」として処理する。
                // ただし古い作品（trailScaleTrackが無い時）は回転がスケールに化ける仕様だったので回転させない
                if (session.trailRotTrack != null && !session.trailRotTrack.isEmpty()) {
                    float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
                    Vector3f trRot = session.trailRotTrack.evaluate(t, s.res1);
                    localPos.rotateZ((float) Math.toRadians(trRot.z));
                    localPos.rotateY((float) Math.toRadians(trRot.y));
                    localPos.rotateX((float) Math.toRadians(trRot.x));
                }
            }

            localPos.x *= session.curWidthScaleX;
            localPos.z *= session.curWidthScaleZ;
            float totalLen = session.trailTipOffset - session.trailBaseOffset;
            float progress = (float)i / (count - 1);
            localPos.y = session.trailBaseOffset + (totalLen * progress * session.curLengthScale);

            if (session.isClaw) {
                Vector3f locL = s.v2.set(localPos.x + session.clawOffset, localPos.y, localPos.z);
                Vector3f locR = s.v3.set(localPos.x - session.clawOffset, localPos.y, localPos.z);

                Vector4f view4L = matrix.transform(s.v4f.set(locL.x, locL.y, locL.z, 1.0f));
                Vector3f view3L = s.v2.set(view4L.x(), view4L.y(), view4L.z());
                s.quat.set(camRot).transform(view3L);
                positionsLeft[i] = new Vector3f((float) (view3L.x + camX), (float) (view3L.y + camY), (float) (view3L.z + camZ));

                Vector4f view4R = matrix.transform(s.v4f.set(locR.x, locR.y, locR.z, 1.0f));
                Vector3f view3R = s.v3.set(view4R.x(), view4R.y(), view4R.z());
                s.quat.set(camRot).transform(view3R);
                positionsRight[i] = new Vector3f((float) (view3R.x + camX), (float) (view3R.y + camY), (float) (view3R.z + camZ));
            }

            Vector4f view4 = matrix.transform(s.v4f.set(localPos.x, localPos.y, localPos.z, 1.0f));
            Vector3f view3 = s.v1.set(view4.x(), view4.y(), view4.z());

            s.quat.set(camRot).transform(view3);

            worldPositions[i] = new Vector3f((float) (view3.x + camX), (float) (view3.y + camY),
                    (float) (view3.z + camZ));
        }

        TrailPoint newPoint = new TrailPoint(worldPositions, new Vector3f((float) camX, (float) camY, (float) camZ),
                new org.joml.Quaternionf(camRot));
        if (session.isClaw) {
            newPoint.positionsLeft = positionsLeft;
            newPoint.positionsRight = positionsRight;
        }
        if (isLeft) {
            session.addLeftPoint(newPoint);
        } else {
            session.addPoint(newPoint);
        }
    }

    public static void captureFirstPersonFromKeyframe(TrailSession session,
            net.minecraft.world.entity.player.Player player,
            float playerYaw) {

        session.isFirstPerson = true;

        if (session.animStartMs < 0)
            return;

        float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
        if (t > session.animationLength / 20.0f)
            return;

        updateVisibilityFromAnimation(session);

        if (!session.visibility)
            return;

        float yawRad = (float) Math.toRadians(playerYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        float cx = (float) org.joml.Math.lerp(player.xOld, player.getX(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick());
        float cy = (float) org.joml.Math.lerp(player.yOld, player.getY(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick()) + player.getEyeHeight();
        float cz = (float) org.joml.Math.lerp(player.zOld, player.getZ(),
                net.minecraft.client.Minecraft.getInstance().getPartialTick());

        captureFirstPersonPoint(session, player, yawRad, cos, sin, cx, cy, cz, t, false);
        
        if (session.isDual) {
            captureFirstPersonPoint(session, player, yawRad, cos, sin, cx, cy, cz, t, true);
        }
    }

    private static java.lang.reflect.Method irisShadowPassMethod = null;
    private static Object irisApiInstance = null;
    private static boolean irisApiChecked = false;

    /**
     * Oculus/Iris が現在シャドウパスを描画中かどうかをリフレクションで取得する。
     * 依存関係を追加せずに安全に他MODと連携するための実装。
     */
    private static boolean isOculusShadowPass() {
        if (!irisApiChecked) {
            irisApiChecked = true;
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApiInstance = apiClass.getMethod("getInstance").invoke(null);
                irisShadowPassMethod = apiClass.getMethod("isRenderingShadowPass");
            } catch (Throwable t) {
                // Oculus/Irisが存在しない場合はエラーを出さずに無視
            }
        }
        if (irisShadowPassMethod != null && irisApiInstance != null) {
            try {
                return (boolean) irisShadowPassMethod.invoke(irisApiInstance);
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    private static void captureFirstPersonPoint(TrailSession session,
            net.minecraft.world.entity.player.Player player,
            float yawRad, float cos, float sin, float cx, float cy, float cz,
            float t, boolean isLeft) {
        
        AnimationKeyframeTrack armRotTrack = isLeft ? session.leftArmRotTrack : session.armRotTrack;
        AnimationKeyframeTrack armPosTrack = isLeft ? session.leftArmPosTrack : session.armPosTrack;
        AnimationKeyframeTrack itemRotTrack = isLeft ? session.leftItemRotTrack : session.itemRotTrack;
        AnimationKeyframeTrack itemPosTrack = isLeft ? session.leftItemPosTrack : session.itemPosTrack;
        AnimationKeyframeTrack legRotTrack = isLeft ? session.leftLegRotTrack : session.rightLegRotTrack;
        AnimationKeyframeTrack legPosTrack = isLeft ? session.leftLegPosTrack : session.rightLegPosTrack;
        
        Scratch s = SCRATCH.get();
        Vector3f armRotDeg = s.v1.set(0);
        if (armRotTrack != null) armRotTrack.evaluate(t, armRotDeg);
        Vector3f armPosPx = s.v2.set(0);
        if (armPosTrack != null) armPosTrack.evaluate(t, armPosPx);
        Vector3f bodyRotDeg = s.v3.set(0);
        if (session.bodyRotTrack != null) session.bodyRotTrack.evaluate(t, bodyRotDeg);
        Vector3f bodyPosPx = s.v4.set(0);
        if (session.bodyPosTrack != null) session.bodyPosTrack.evaluate(t, bodyPosPx);
        Vector3f itemRotDeg = s.v5.set(0);
        if (itemRotTrack != null) itemRotTrack.evaluate(t, itemRotDeg);
        Vector3f itemPosPx = s.v6.set(0);
        if (itemPosTrack != null) itemPosTrack.evaluate(t, itemPosPx);
        Vector3f legRotDeg = s.v7.set(0);
        if (legRotTrack != null) legRotTrack.evaluate(t, legRotDeg);
        Vector3f legPosPx = s.v8.set(0);
        if (legPosTrack != null) legPosTrack.evaluate(t, legPosPx);

        Matrix4f matrix = s.mat.identity();
        
        // 足ボーンかどうかでマトリクス計算を切り替える
        boolean isLeg = (session.followBone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.RIGHT_LEG ||
                         session.followBone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.LEFT_LEG ||
                         session.followBone == com.zoma1101.swordskill.swordskills.SkillData.FollowBone.BOTH_LEGS);
        
        if (isLeg) {
            getLegMatrix4f(bodyRotDeg, bodyPosPx, legRotDeg, legPosPx, isLeft, matrix);
        } else {
            getMatrix4f(bodyRotDeg, bodyPosPx, armRotDeg, armPosPx, itemRotDeg, itemPosPx, isLeft, matrix);
        }

        int count = session.pointCount;
        Vector3f[] worldPositions = new Vector3f[count];
        Vector3f[] positionsLeft = null;
        Vector3f[] positionsRight = null;
        if (session.isClaw) {
            positionsLeft = new Vector3f[count];
            positionsRight = new Vector3f[count];
        }

        for (int i = 0; i < count; i++) {
            session.getLocalPoint(i, s.v1);
            Vector3f local = s.v1;

            if (session.trailScaleTrack != null && !session.trailScaleTrack.isEmpty()) {
                if (session.trailRotTrack != null && !session.trailRotTrack.isEmpty()) {
                    Vector3f trRot = session.trailRotTrack.evaluate(t, s.res1);
                    local.rotateZ((float) Math.toRadians(trRot.z));
                    local.rotateY((float) Math.toRadians(trRot.y));
                    local.rotateX((float) Math.toRadians(trRot.x));
                }
            }

            local.rotateX((float) Math.toRadians(-25));

            local.y = (local.y - session.trailTipOffset) * session.trailLengthScale * session.curLengthScale;
            local.x = local.x * session.trailLengthScale * session.curWidthScaleX;
            local.z = local.z * session.trailLengthScale * session.curWidthScaleZ;

            if (session.isClaw) {
                Vector3f locL = s.v2.set(local.x + session.clawOffset, local.y, local.z);
                Vector3f locR = s.v3.set(local.x - session.clawOffset, local.y, local.z);
                matrix.transformPosition(locL);
                float wxL = cos * locL.x + sin * locL.z;
                float wzL = sin * locL.x - cos * locL.z;
                positionsLeft[i] = new Vector3f(cx + wxL, cy + locL.y, cz + wzL);

                matrix.transformPosition(locR);
                float wxR = cos * locR.x + sin * locR.z;
                float wzR = sin * locR.x - cos * locR.z;
                positionsRight[i] = new Vector3f(cx + wxR, cy + locR.y, cz + wzR);
            }

            matrix.transformPosition(local);
            float wx = cos * local.x + sin * local.z;
            float wz = sin * local.x - cos * local.z;
            worldPositions[i] = new Vector3f(cx + wx, cy + local.y, cz + wz);
        }

        TrailPoint newPoint = new TrailPoint(worldPositions, new org.joml.Vector3f(cx, cy, cz),
                new org.joml.Quaternionf().rotationY(-yawRad));
        if (session.isClaw) {
            newPoint.positionsLeft = positionsLeft;
            newPoint.positionsRight = positionsRight;
        }
        if (isLeft) {
            session.addLeftPoint(newPoint);
        } else {
            session.addPoint(newPoint);
        }
    }


    public static void getMatrix4f(Vector3f bodyRotDeg, Vector3f bodyPosPx, Vector3f armRotDeg,
            Vector3f armPosPx, Vector3f itemRotDeg, Vector3f itemPosPx, boolean isLeft, Matrix4f matrix) {
        matrix.identity();
        // ボディ変換 (body)
        matrix.translate(0f, -0.75f, 0f); // ボディの中心
        matrix.rotateY((float) Math.toRadians(bodyRotDeg.y));
        matrix.rotateX((float) Math.toRadians(bodyRotDeg.x));
        matrix.rotateZ((float) Math.toRadians(bodyRotDeg.z));
        matrix.translate(bodyPosPx.x / 16f, bodyPosPx.y / 16f, bodyPosPx.z / 16f);

        // 腕変換 (right_arm / left_arm)
        float shoulderX = isLeft ? 0.3125f : -0.3125f;
        matrix.translate(shoulderX, 0.625f, 0f); // 腕の付け根
        matrix.rotateZ((float) Math.toRadians(armRotDeg.z));
        matrix.rotateY((float) Math.toRadians(armRotDeg.y));
        matrix.rotateX((float) Math.toRadians(armRotDeg.x));
        matrix.translate(armPosPx.x / 16f, armPosPx.y / 16f, armPosPx.z / 16f);

        // 手のひら・アイテム変換 (rightItem / leftItem)
        matrix.translate(0f, -0.5625f, 0.0625f); // 腕の先（手）
        matrix.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);
        matrix.rotateX((float) Math.toRadians(-itemRotDeg.x - 90));
        matrix.rotateY((float) Math.toRadians(-itemRotDeg.y));
        matrix.rotateZ((float) Math.toRadians(itemRotDeg.z));
    }

    public static void getLegMatrix4f(Vector3f bodyRotDeg, Vector3f bodyPosPx, Vector3f legRotDeg,
            Vector3f legPosPx, boolean isLeft, Matrix4f matrix) {
        matrix.identity();
        // ボディ変換 (body)
        matrix.translate(0f, -0.75f, 0f); // ボディの中心
        matrix.rotateY((float) Math.toRadians(bodyRotDeg.y));
        matrix.rotateX((float) Math.toRadians(bodyRotDeg.x));
        matrix.rotateZ((float) Math.toRadians(bodyRotDeg.z));
        matrix.translate(bodyPosPx.x / 16f, bodyPosPx.y / 16f, bodyPosPx.z / 16f);

        // 足変換 (right_leg / left_leg)
        float hipX = isLeft ? 0.125f : -0.125f;
        matrix.translate(hipX, 0f, 0f); // 股関節の位置 (ボディ中心から見て腰の位置)
        matrix.rotateZ((float) Math.toRadians(legRotDeg.z));
        matrix.rotateY((float) Math.toRadians(legRotDeg.y));
        matrix.rotateX((float) Math.toRadians(legRotDeg.x));
        matrix.translate(legPosPx.x / 16f, legPosPx.y / 16f, legPosPx.z / 16f);

        // 足のトレイルの基準点を足先に近づける
        matrix.translate(0f, -0.75f, 0f);
    }

    public static void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource, TrailSession session) {
        if (session.points.size() < 4)
            return;

        // 突きモーション時は一人称視点のトレイルを非表示にする (ビーム判定を見やすくするため)
        if (session.isFirstPerson && session.animationName != null) {
            String anim = session.animationName.toLowerCase();
            boolean isThrust = anim.contains("thrust") || anim.contains("strike") || 
                               anim.contains("spike") || anim.contains("linear") || 
                               anim.contains("sting") || anim.contains("pain") || 
                               anim.contains("cruci") || anim.contains("splash") || 
                               anim.contains("tier") || anim.contains("rosario") || 
                               anim.contains("shooting") || anim.contains("penetrator");
            if (isThrust) {
                float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
                boolean isSweeping = false;
                
                if (anim.equals("spark_thrust") && t > 1.0f) {
                    isSweeping = true;
                }
                
                if (!isSweeping) {
                    return;
                }
            }
        }

        net.minecraft.resources.ResourceLocation tex = TEST_TEXTURE != null ? TEST_TEXTURE : session.texture;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityNoOutline(tex));
        poseStack.pushPose();
        poseStack.last().pose().identity();
        Matrix4f worldViewMatrix = poseStack.last().pose();

        // 右手側のトレイル描画
        renderPointList(consumer, worldViewMatrix, session, session.points);
        
        if (session.isDual && !session.leftPoints.isEmpty()) {
            renderPointList(consumer, worldViewMatrix, session, session.leftPoints);
        }

        poseStack.popPose();
    }

    private static void renderPointList(VertexConsumer consumer, Matrix4f worldViewMatrix, TrailSession session, LinkedList<TrailPoint> pointList) {
        if (pointList.size() < 2) return;

        // 平滑化FPS（EMA）＋ヒステリシスで低FPSモードを判定
        // 瞬間値を使うと30付近で毎フレーム切り替わるため、閾値に幅を持たせて安定させる
        int rawFps = net.minecraft.client.Minecraft.getInstance().getFps();
        smoothedFps = smoothedFps * (1.0f - FPS_SMOOTH) + rawFps * FPS_SMOOTH;
        if (!lowFpsMode && smoothedFps < LOW_FPS_ENTER) lowFpsMode = true;
        if (lowFpsMode  && smoothedFps > LOW_FPS_EXIT)  lowFpsMode = false;
        boolean lowFps = lowFpsMode;

        TrailPoint p1 = null;
        int i = 0;
        int size = pointList.size();
        for (TrailPoint p2 : pointList) {
            if (p1 != null && !p2.isNewSegment) {
                float alphaIdx = (float) i / (size - 1);
                float nextAlphaIdx = (float) (i + 1) / (size - 1);

                for (int j = 0; j < session.pointCount - 1; j++) {
                    // 低FPS時はグローパス（widthScale=1.2, alphaScale=0.4）をスキップ
                    if (!lowFps) {
                        drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positions, p2.positions, j, alphaIdx, nextAlphaIdx, session.color, 1.2f, 0.4f, session.isFirstPerson);
                    }
                    drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positions, p2.positions, j, alphaIdx, nextAlphaIdx, session.color, 1.0f, 1.0f, session.isFirstPerson);

                    if (p1.positionsLeft != null && p2.positionsLeft != null) {
                        if (!lowFps) {
                            drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positionsLeft, p2.positionsLeft, j, alphaIdx, nextAlphaIdx, session.color, 1.2f, 0.4f, session.isFirstPerson);
                        }
                        drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positionsLeft, p2.positionsLeft, j, alphaIdx, nextAlphaIdx, session.color, 1.0f, 1.0f, session.isFirstPerson);
                        if (!lowFps) {
                            drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positionsRight, p2.positionsRight, j, alphaIdx, nextAlphaIdx, session.color, 1.2f, 0.4f, session.isFirstPerson);
                        }
                        drawSegmentQuad(consumer, worldViewMatrix, p1, p2, p1.positionsRight, p2.positionsRight, j, alphaIdx, nextAlphaIdx, session.color, 1.0f, 1.0f, session.isFirstPerson);
                    }
                }
                i++;
            } else if (p1 != null) {
                i++;
            }
            p1 = p2;
        }
    }

    private static void drawSegmentQuad(VertexConsumer consumer, Matrix4f matrix, TrailPoint p1, TrailPoint p2,
            Vector3f[] pos1Array, Vector3f[] pos2Array,
            int segmentIdx, float alpha, float nextAlpha, int colorARGB, float widthScale, float alphaScale,
            boolean isFirstPerson) {
        float a = ((colorARGB >> 24) & 0xFF) / 255.0f;
        float r = ((colorARGB >> 16) & 0xFF) / 255.0f * 2.0f;
        float g = ((colorARGB >> 8) & 0xFF) / 255.0f * 2.0f;
        float b = (colorARGB & 0xFF) / 255.0f * 2.0f;

        float alpha1 = a * (1.0f - alpha) * alphaScale;
        float alpha2 = a * (1.0f - nextAlpha) * alphaScale;

        Scratch s = SCRATCH.get();
        Vector3f base1 = pos1Array[0];
        Vector3f pos1_v1 = s.v1.set(base1).lerp(pos1Array[segmentIdx], widthScale);
        Vector3f pos1_v2 = s.v2.set(base1).lerp(pos1Array[segmentIdx + 1], widthScale);

        Vector3f base2 = pos2Array[0];
        Vector3f pos2_v3 = s.v3.set(base2).lerp(pos2Array[segmentIdx + 1], widthScale);
        Vector3f pos2_v4 = s.v4.set(base2).lerp(pos2Array[segmentIdx], widthScale);

        Vector3f v1 = toViewSpace(pos1_v1, p1.camPos, p1.camRot, isFirstPerson, s.res1);
        Vector3f v2 = toViewSpace(pos1_v2, p1.camPos, p1.camRot, isFirstPerson, s.res2);
        Vector3f v3 = toViewSpace(pos2_v3, p2.camPos, p2.camRot, isFirstPerson, s.res3);
        Vector3f v4 = toViewSpace(pos2_v4, p2.camPos, p2.camRot, isFirstPerson, s.res4);

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

        Scratch s = SCRATCH.get();
        if (session.trailTrack != null && !session.trailTrack.isEmpty()) {
            // FPSが低い場合、フレーム間で on→off→on の遷移が発生しても
            // 1フレームに1回しか評価しないと中間の状態変化が検出できない。
            // フレーム間を複数サンプリングしてセグメント分割を正確に検出する。
            float lastT = session.lastVisibilityCheckT;
            session.lastVisibilityCheckT = t;

            final int SAMPLES = 4;
            boolean prevState = session.visibility;
            float interval = t - lastT;
            if (interval > 0 && lastT >= 0) {
                for (int k = 1; k <= SAMPLES; k++) {
                    float sampleT = lastT + interval * k / SAMPLES;
                    Vector3f val = session.trailTrack.evaluate(sampleT, s.v1);
                    boolean sampleState = (val.x > -0.5f);
                    if (!prevState && sampleState) {
                        // このフレーム間で off→on の遷移を検出 → 新規セグメントを予約
                        session.pendingNewSegment = true;
                    }
                    prevState = sampleState;
                }
            } else {
                // 初回フレームはシンプルに評価
                Vector3f val = session.trailTrack.evaluate(t, s.v1);
                prevState = (val.x > -0.5f);
                if (!session.visibility && prevState) {
                    session.pendingNewSegment = true;
                }
            }
            session.visibility = prevState;
        } else {
            session.visibility = true;
        }

        if (session.trailScaleTrack != null && !session.trailScaleTrack.isEmpty()) {
            Vector3f scaleVal = session.trailScaleTrack.evaluate(t, s.v1);
            session.curWidthScaleX = scaleVal.x != 0 ? scaleVal.x : 1.0f;
            session.curWidthScaleZ = scaleVal.y != 0 ? scaleVal.y : 1.0f;
            session.curLengthScale = scaleVal.z != 0 ? scaleVal.z : 1.0f;
        } else if (session.trailRotTrack != null && !session.trailRotTrack.isEmpty()) {
            // 後方互換性のため、scaleトラックが無い場合は昔のようにrotation値でscaleする
            Vector3f rotVal = session.trailRotTrack.evaluate(t, s.v1);
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
            boolean isFirstPerson, Vector3f dest) {
        dest.set(worldPos.x - camPos.x, worldPos.y - camPos.y, worldPos.z - camPos.z);
        Scratch s = SCRATCH.get();
        if (isFirstPerson) {
            Vector3f viewOffset = s.v12.set(0.12f, 0.12f, 0.35f);
            s.quat.set(camRot).transform(viewOffset);
            dest.sub(viewOffset);
        }
        s.quat.set(camRot).conjugate().transform(dest);
        return dest;
    }

    public static class TrailSession {
        public final UUID entityUUID;
        public LinkedList<TrailPoint> points = new LinkedList<>();
        public LinkedList<TrailPoint> leftPoints = new LinkedList<>();

        public int color = DEFAULT_COLOR;
        public com.zoma1101.swordskill.swordskills.SkillData.FollowBone followBone = com.zoma1101.swordskill.swordskills.SkillData.FollowBone.MAIN_HAND;
        public net.minecraft.resources.ResourceLocation texture = DEFAULT_TEXTURE;
        public int maxPoints = 25;
        public float trailBaseOffset = 0.6f;
        public float trailTipOffset = 1.4f;
        public float trailLengthScale = 0.55f;
        public int pointCount = 2;
        public float arcAngle = 0f;
        public boolean active = true;
        public boolean isFirstPerson = false;

        public String animationName = "";
        public float animationLength = 0.5f;
        public long animStartMs = -1L;

        public AnimationKeyframeTrack armRotTrack = null;
        public AnimationKeyframeTrack armPosTrack = null;
        public AnimationKeyframeTrack bodyRotTrack = null;
        public AnimationKeyframeTrack bodyPosTrack = null;
        public AnimationKeyframeTrack itemRotTrack = null;
        public AnimationKeyframeTrack itemPosTrack = null;
        
        public AnimationKeyframeTrack leftArmRotTrack = null;
        public AnimationKeyframeTrack leftArmPosTrack = null;
        public AnimationKeyframeTrack leftItemRotTrack = null;
        public AnimationKeyframeTrack leftItemPosTrack = null;
        
        public AnimationKeyframeTrack rightLegRotTrack = null;
        public AnimationKeyframeTrack rightLegPosTrack = null;
        public AnimationKeyframeTrack leftLegRotTrack = null;
        public AnimationKeyframeTrack leftLegPosTrack = null;
        
        public AnimationKeyframeTrack trailTrack = null;
        public AnimationKeyframeTrack trailRotTrack = null;
        public AnimationKeyframeTrack trailScaleTrack = null;
        public boolean pendingNewSegment = false;
        public boolean visibility = true;
        /** updateVisibilityFromAnimation の前回評価時刻（秒）。フレーム間補間に使用 */
        public float lastVisibilityCheckT = -1.0f;
        /** Oculusなど複数パスレンダラーの重複記録防止用: 絶対描画フレーム番号 */
        public long lastRecordedFrame = -1L;

        public float curWidthScaleX = 1.0f;
        public float curWidthScaleZ = 1.0f;
        public float curLengthScale = 1.0f;

        public boolean isClaw = false;
        public float clawOffset = 0.25f;
        
        public boolean isDual = false;

        public TrailSession(UUID uuid) {
            this.entityUUID = uuid;
        }

        public boolean isActiveAnimation() {
            if (!active || animStartMs < 0) return false;
            float t = (System.currentTimeMillis() - animStartMs) / 1000.0f;
            return t <= animationLength;
        }

        public void getLocalPoint(int index, Vector3f dest) {
            if (pointCount <= 1) {
                dest.set(0, trailBaseOffset, 0);
                return;
            }

            float progress = (float) index / (pointCount - 1);
            float totalLength = trailTipOffset - trailBaseOffset;

            if (arcAngle > 0) {
                float angleRad = (float) Math.toRadians(arcAngle);
                float currentAngle = (progress - 0.5f) * angleRad;
                float radius = totalLength / angleRad;
                dest.set(
                        (float) Math.sin(currentAngle) * radius,
                        (float) Math.cos(currentAngle) * radius + trailBaseOffset - radius,
                        0);
            } else {
                dest.set(0, trailBaseOffset + totalLength * progress, 0);
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

        public void addLeftPoint(TrailPoint newPoint) {
            if (leftPoints.isEmpty() || leftPoints.getLast().positions[0].distance(newPoint.positions[0]) > 0.005f) {
                leftPoints.add(newPoint);
            }
            filterOldPoints();
        }

        public void filterOldPoints() {
            long now = System.currentTimeMillis();
            long lifeTimeMs = (long) ((maxPoints / 60.0f) * 1000.0f);
            while (!points.isEmpty() && now - points.getFirst().timestamp > lifeTimeMs) {
                points.removeFirst();
            }
            while (!leftPoints.isEmpty() && now - leftPoints.getFirst().timestamp > lifeTimeMs) {
                leftPoints.removeFirst();
            }
        }
    }

    public static class TrailPoint {
        public final Vector3f[] positions;
        public Vector3f[] positionsLeft = null;
        public Vector3f[] positionsRight = null;
        
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