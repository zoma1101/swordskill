package com.zoma1101.swordskill.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.swordskills.SkillData;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WeaponTrailHandler {
    private static final long TRAIL_LIFETIME_NANOS = 450_000_000L;
    private static final int MAX_TRAIL_SAMPLES = 48;
    // 爪武器は1本の手から3本のソードスキルトレイルを発生させる
    private static final int CLAW_TRAIL_COUNT = 3;
    private static final double CLAW_TRAIL_SPREAD = 0.22D;
    private static final RenderType TRAIL_RENDER_TYPE = RenderType.create(
            "swordskill_trail",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final Map<TrailKey, ArrayDeque<TrailSample>> TRAILS = new HashMap<>();
    private static final Map<ArmKey, PendingTrail> PENDING_DIRECTIONS = new HashMap<>();
    private static final Map<Integer, Integer> SKILL_COLORS = new HashMap<>();
    private static final Map<Integer, Double> TRAIL_LENGTHS = new HashMap<>();
    private static final double DEFAULT_TRAIL_LENGTH = 1.5D;
    private static ClientLevel observedLevel;

    private WeaponTrailHandler() {
    }

    public static void setSkillColor(int entityId, String color) {
        try {
            SKILL_COLORS.put(entityId, Integer.parseInt(color.substring(1), 16));
        } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
            SKILL_COLORS.remove(entityId);
        }
    }

    /** スキルごとのトレイル長を設定する。突き系スキルでは長くして突きを強調する。 */
    public static void setTrailLength(int entityId, double length) {
        if (Double.isFinite(length) && length > 0.0D) {
            TRAIL_LENGTHS.put(entityId, length);
        } else {
            TRAIL_LENGTHS.remove(entityId);
        }
    }

    public static void clearTrailLength(int entityId) {
        TRAIL_LENGTHS.remove(entityId);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        clearOnLevelChange(Minecraft.getInstance().level);
        long now = System.nanoTime();
        if (player.isInvisible() || player.isSpectator()) {
            PENDING_DIRECTIONS.keySet().removeIf(key -> key.entityId == player.getId());
            return;
        }
        if (player instanceof IAnimatedPlayer animatedPlayer
                && animatedPlayer.playerAnimator_getAnimation().isActive()) {
            var animation = animatedPlayer.playerAnimator_getAnimation();
            animation.setTickDelta(event.getPartialTick());
            Vec3f bodyPosition = animation.get3DTransform("body", TransformType.POSITION, Vec3f.ZERO);
            Vec3f bodyRotation = animation.get3DTransform("body", TransformType.ROTATION, Vec3f.ZERO);
            Vec3f bodyScale = animation.get3DTransform("body", TransformType.SCALE,
                    new Vec3f(1.0F, 1.0F, 1.0F));
            PlayerRenderer renderer = event.getRenderer();
            PlayerModel<?> model = renderer.getModel();
            Vec3 renderOrigin = player.getPosition(event.getPartialTick())
                    .add(renderer.getRenderOffset(player, event.getPartialTick()));
            float bodyYaw = Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
            HumanoidArm mainArm = player.getMainArm();
            HumanoidArm offArm = mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            appendPendingTrail(player, model, mainArm, bodyYaw, renderOrigin,
                    bodyPosition, bodyRotation, bodyScale, now);
            appendPendingTrail(player, model, offArm, bodyYaw, renderOrigin,
                    bodyPosition, bodyRotation, bodyScale, now);
        }
        PENDING_DIRECTIONS.keySet().removeIf(key -> key.entityId == player.getId());
        renderTrails(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player, now);
    }

    /** Captures the actual held-item pose after PlayerAnimator has applied rightItem. */
    public static void captureRenderedItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack heldItemPose, net.minecraft.world.level.Level level, int seed) {
        if (!(entity instanceof AbstractClientPlayer player)
                || !(player instanceof IAnimatedPlayer animatedPlayer)
                || !animatedPlayer.playerAnimator_getAnimation().isActive()
                || stack.isEmpty()
                || (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }

        clearOnLevelChange(Minecraft.getInstance().level);
        BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, entity, seed);
        PoseStack itemPose = new PoseStack();
        itemPose.last().pose().set(heldItemPose.last().pose());
        itemPose.last().normal().set(heldItemPose.last().normal());
        model = model.applyTransform(context, itemPose, leftHand);

        BladeAxis bladeAxis = findBladeAxis(model);
        Matrix4f itemMatrix = new Matrix4f(itemPose.last().pose());
        Vector3f direction = itemMatrix.transformDirection(bladeAxis.direction());
        if (direction.lengthSquared() < 1.0E-8F) {
            return;
        }
        float trailLength = (float) (double) TRAIL_LENGTHS.getOrDefault(player.getId(), DEFAULT_TRAIL_LENGTH);
        direction.normalize(trailLength);

        Vec3 bladeDirection = new Vec3(direction.x, direction.y, direction.z);
        Vec3 unitDirection = bladeDirection.normalize();
        // 3本のトレイルは常に水平方向(左右)へ並べる
        Vec3 side = horizontalPerpendicular(unitDirection);

        HumanoidArm arm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        PENDING_DIRECTIONS.put(new ArmKey(player.getId(), arm),
                new PendingTrail(bladeDirection, side));
    }

    private static void appendPendingTrail(Player player, PlayerModel<?> model, HumanoidArm arm, float bodyYaw,
            Vec3 renderOrigin, Vec3f bodyPosition, Vec3f bodyRotation, Vec3f bodyScale, long now) {
        ArmKey armKey = new ArmKey(player.getId(), arm);
        PendingTrail pending = PENDING_DIRECTIONS.remove(armKey);
        ItemStack heldItem = arm == player.getMainArm() ? player.getMainHandItem() : player.getOffhandItem();
        if (pending == null || heldItem.isEmpty()) {
            return;
        }
        boolean claw = isClawWeapon(heldItem);
        int variantCount = claw ? CLAW_TRAIL_COUNT : 1;
        Vec3 baseWorld = sampleGrip(model, arm, bodyYaw, renderOrigin,
                bodyPosition, bodyRotation, bodyScale);
        for (int variant = 0; variant < variantCount; variant++) {
            double offsetAmount = claw
                    ? CLAW_TRAIL_SPREAD * (variant - (variantCount - 1) / 2.0D)
                    : 0.0D;
            Vec3 base = baseWorld.add(pending.side().scale(offsetAmount));
            Vec3 tip = base.add(pending.direction());
            TrailKey key = new TrailKey(player.getId(), arm, variant);
            ArrayDeque<TrailSample> samples = TRAILS.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            TrailSample last = samples.peekLast();
            if (last == null || last.base.distanceToSqr(base) + last.tip.distanceToSqr(tip) > 0.0000025) {
                samples.addLast(new TrailSample(base, tip, now));
            }
            pruneSamples(samples, now);
            while (samples.size() > MAX_TRAIL_SAMPLES) {
                samples.removeFirst();
            }
        }
    }

    private static boolean isClawWeapon(ItemStack heldItem) {
        return WeaponTypeDetector.detectWeaponTypes(heldItem).contains(SkillData.WeaponType.CLAW);
    }

    /** 刃軸に直交する水平方向のベクトル。爪トレイルを左右に並べる際のオフセット軸に使う。 */
    private static Vec3 horizontalPerpendicular(Vec3 direction) {
        Vec3 horizontal = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (horizontal.lengthSqr() < 1.0E-6) {
            horizontal = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        return horizontal.normalize();
    }

    private static Vec3 sampleGrip(PlayerModel<?> model, HumanoidArm arm, float bodyYaw,
            Vec3 renderOrigin, Vec3f bodyPosition, Vec3f bodyRotation, Vec3f bodyScale) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(bodyScale.getX(), bodyScale.getY(), bodyScale.getZ());
        poseStack.translate(bodyPosition.getX(), bodyPosition.getY() + 0.7F, bodyPosition.getZ());
        poseStack.mulPose(Axis.ZP.rotation(bodyRotation.getZ()));
        poseStack.mulPose(Axis.YP.rotation(bodyRotation.getY()));
        poseStack.mulPose(Axis.XP.rotation(bodyRotation.getX()));
        poseStack.translate(0.0D, -0.7D, 0.0D);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        model.body.translateAndRotate(poseStack);
        (arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm).translateAndRotate(poseStack);
        Vector3f grip = poseStack.last().pose().transformPosition(new Vector3f(0.0F, 0.75F, 0.0F));
        return renderOrigin.add(grip.x, grip.y, grip.z);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        clearOnLevelChange(minecraft.level);
        if (minecraft.level == null) {
            return;
        }
        long now = System.nanoTime();
        Iterator<Map.Entry<TrailKey, ArrayDeque<TrailSample>>> trailIterator = TRAILS.entrySet().iterator();
        while (trailIterator.hasNext()) {
            Map.Entry<TrailKey, ArrayDeque<TrailSample>> entry = trailIterator.next();
            pruneSamples(entry.getValue(), now);
            if (entry.getValue().isEmpty()) {
                trailIterator.remove();
            }
        }
    }

    private static BladeAxis findBladeAxis(BakedModel model) {
        List<Vector3f> vertices = new ArrayList<>();
        Direction[] directions = Direction.values();
        for (int side = -1; side < directions.length; side++) {
            Direction direction = side < 0 ? null : directions[side];
            for (var quad : model.getQuads(null, direction, RandomSource.create(0L))) {
                int[] packedVertices = quad.getVertices();
                for (int vertex = 0; vertex + 2 < packedVertices.length; vertex += 8) {
                    vertices.add(new Vector3f(Float.intBitsToFloat(packedVertices[vertex]),
                            Float.intBitsToFloat(packedVertices[vertex + 1]),
                            Float.intBitsToFloat(packedVertices[vertex + 2])));
                }
            }
        }

        if (vertices.isEmpty()) {
            return new BladeAxis(new Vector3f(0.0F, -0.5F, 0.0F), new Vector3f(0.0F, 1.0F, 0.0F));
        }

        Vector3f center = new Vector3f();
        for (Vector3f vertex : vertices) {
            center.add(vertex);
        }
        center.div(vertices.size());

        double[][] covariance = new double[3][3];
        for (Vector3f vertex : vertices) {
            double x = vertex.x - center.x;
            double y = vertex.y - center.y;
            double z = vertex.z - center.z;
            covariance[0][0] += x * x;
            covariance[0][1] += x * y;
            covariance[0][2] += x * z;
            covariance[1][1] += y * y;
            covariance[1][2] += y * z;
            covariance[2][2] += z * z;
        }
        covariance[1][0] = covariance[0][1];
        covariance[2][0] = covariance[0][2];
        covariance[2][1] = covariance[1][2];

        int dominantComponent = covariance[1][1] >= covariance[0][0]
                && covariance[1][1] >= covariance[2][2] ? 1
                : covariance[0][0] >= covariance[2][2] ? 0 : 2;
        Vector3f direction = new Vector3f();
        direction.setComponent(dominantComponent, 1.0F);
        for (int iteration = 0; iteration < 16; iteration++) {
            Vector3f next = new Vector3f(
                    (float) (covariance[0][0] * direction.x + covariance[0][1] * direction.y
                            + covariance[0][2] * direction.z),
                    (float) (covariance[1][0] * direction.x + covariance[1][1] * direction.y
                            + covariance[1][2] * direction.z),
                    (float) (covariance[2][0] * direction.x + covariance[2][1] * direction.y
                            + covariance[2][2] * direction.z));
            if (next.lengthSquared() < 1.0E-12F) {
                break;
            }
            direction.set(next).normalize();
        }

        float minProjection = Float.POSITIVE_INFINITY;
        float maxProjection = Float.NEGATIVE_INFINITY;
        for (Vector3f vertex : vertices) {
            Vector3f relative = new Vector3f(vertex).sub(center);
            float projection = relative.dot(direction);
            minProjection = Math.min(minProjection, projection);
            maxProjection = Math.max(maxProjection, projection);
        }

        // Use the actual mesh cross-section at the hilt end. Projecting the
        // model's centroid onto the blade axis can shift the trail sideways.
        float gripEndTolerance = Math.max(0.005F, (maxProjection - minProjection) * 0.025F);
        Vector3f base = new Vector3f();
        int gripEndVertexCount = 0;
        for (Vector3f vertex : vertices) {
            float projection = new Vector3f(vertex).sub(center).dot(direction);
            if (projection <= minProjection + gripEndTolerance) {
                base.add(vertex);
                gripEndVertexCount++;
            }
        }
        if (gripEndVertexCount > 0) {
            base.div(gripEndVertexCount);
        } else {
            base.set(center).fma(minProjection, direction);
        }
        return new BladeAxis(base, direction);
    }

    private static void renderTrails(PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight, Player player, long now) {
        VertexConsumer vertices = buffers.getBuffer(TRAIL_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        Matrix4f inverseMatrix = new Matrix4f(matrix).invert();
        int skillColor = saturateColor(SKILL_COLORS.getOrDefault(player.getId(), 0x54D8FF));
        int darkColor = mixWithBlack(skillColor, 0.45F);
        int brightColor = mixWithWhite(skillColor, 0.4F);
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (Map.Entry<TrailKey, ArrayDeque<TrailSample>> entry : TRAILS.entrySet()) {
            if (entry.getKey().entityId != player.getId()) {
                continue;
            }
            ArrayDeque<TrailSample> samples = entry.getValue();
            pruneSamples(samples, now);
            if (samples.size() < 2) {
                continue;
            }

            TrailSample[] points = samples.toArray(TrailSample[]::new);
            int subdivisions = 6;
            for (int i = 0; i < points.length - 1; i++) {
                TrailSample p0 = points[Math.max(0, i - 1)];
                TrailSample p1 = points[i];
                TrailSample p2 = points[i + 1];
                TrailSample p3 = points[Math.min(points.length - 1, i + 2)];
                for (int step = 0; step < subdivisions; step++) {
                    float from = step / (float) subdivisions;
                    float to = (step + 1) / (float) subdivisions;
                    TrailSample fromSample = interpolate(p0, p1, p2, p3, from);
                    TrailSample toSample = interpolate(p0, p1, p2, p3, to);
                    float age = Math.min(1.0F,
                            (now - toSample.createdAtNanos) / (float) TRAIL_LIFETIME_NANOS);
                    float ageAlpha = (1.0F - age) * (1.0F - age);
                    float lengthAlpha = (i + to) / (points.length - 1.0F);
                    float alpha = 0.82F * ageAlpha * lengthAlpha;
                    if (alpha > 0.01F) {
                        renderRibbonSegment(vertices, matrix, fromSample, toSample, alpha, inverseMatrix,
                                cameraPosition, darkColor, skillColor, brightColor);
                    }
                }
            }
        }
    }

    private static TrailSample interpolate(TrailSample p0, TrailSample p1, TrailSample p2, TrailSample p3,
            float t) {
        return new TrailSample(catmullRom(p0.base, p1.base, p2.base, p3.base, t),
                catmullRom(p0.tip, p1.tip, p2.tip, p3.tip, t),
                p1.createdAtNanos + (long) ((p2.createdAtNanos - p1.createdAtNanos) * t));
    }

    private static Vec3 catmullRom(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return p1.scale(2.0)
                .add(p2.subtract(p0).scale(t))
                .add(p0.scale(2.0).subtract(p1.scale(5.0)).add(p2.scale(4.0)).subtract(p3).scale(t2))
                .add(p3.subtract(p0).add(p1.scale(3.0)).subtract(p2.scale(3.0)).scale(t3))
                .scale(0.5);
    }

    private static void renderRibbonSegment(VertexConsumer vertices, Matrix4f matrix,
            TrailSample previous, TrailSample current, float alpha, Matrix4f inverseMatrix, Vec3 cameraPosition,
            int darkColor, int mainColor, int brightColor) {
        Vec3 oldBase = previous.base;
        Vec3 oldTip = previous.tip;
        Vec3 newBase = current.base;
        Vec3 newTip = current.tip;

        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldBase, darkColor, alpha * 0.45F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldTip, mainColor, alpha);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newTip, brightColor, alpha);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newBase, mainColor, alpha * 0.5F);

        Vec3 oldCoreLeft = oldBase.lerp(oldTip, 0.45);
        Vec3 oldCoreRight = oldBase.lerp(oldTip, 0.55);
        Vec3 newCoreLeft = newBase.lerp(newTip, 0.45);
        Vec3 newCoreRight = newBase.lerp(newTip, 0.55);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldCoreLeft, 0xFFFFFF, alpha * 0.85F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldCoreRight, 0xFFFFFF, alpha * 0.85F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newCoreRight, 0xFFFFFF, alpha * 0.85F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newCoreLeft, 0xFFFFFF, alpha * 0.85F);

        Vec3 oldCenterWorld = previous.base.add(previous.tip).scale(0.5);
        Vec3 newCenterWorld = current.base.add(current.tip).scale(0.5);
        Vec3 movement = newCenterWorld.subtract(oldCenterWorld);
        Vec3 bladeDirection = previous.tip.add(current.tip)
                .subtract(previous.base).subtract(current.base).scale(0.5);
        Vec3 viewDirection = cameraPosition.subtract(oldCenterWorld.add(newCenterWorld).scale(0.5));
        double movementLength = movement.length();
        double bladeLength = bladeDirection.length();
        if (movementLength < 1.0E-5 || bladeLength < 1.0E-5 || viewDirection.lengthSqr() < 1.0E-8) {
            return;
        }

        Vec3 normal = bladeDirection.cross(movement);
        double facing = normal.lengthSqr() < 1.0E-8
                ? 0.0
                : Math.abs(normal.normalize().dot(viewDirection.normalize()));
        float edgeOnAlpha = 1.0F - smoothstep(0.08F, 0.45F, (float) facing);
        if (edgeOnAlpha <= 0.01F) {
            return;
        }

        Vec3 side = movement.normalize().cross(viewDirection.normalize());
        if (side.lengthSqr() < 1.0E-8) {
            side = movement.normalize().cross(new Vec3(0.0, 1.0, 0.0));
        }
        if (side.lengthSqr() < 1.0E-8) {
            side = movement.normalize().cross(new Vec3(1.0, 0.0, 0.0));
        }
        side = side.normalize().scale(Math.min(0.18, bladeLength * 0.12));

        float billboardAlpha = alpha * edgeOnAlpha;
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldCenterWorld.subtract(side), mainColor, billboardAlpha * 0.55F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, oldCenterWorld.add(side), brightColor, billboardAlpha * 0.55F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newCenterWorld.add(side), brightColor, billboardAlpha * 0.55F);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, newCenterWorld.subtract(side), mainColor, billboardAlpha * 0.55F);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int saturateColor(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int gray = (red + green + blue) / 3;
        red = Mth.clamp((int) (gray + (red - gray) * 1.35F), 0, 255);
        green = Mth.clamp((int) (gray + (green - gray) * 1.35F), 0, 255);
        blue = Mth.clamp((int) (gray + (blue - gray) * 1.35F), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private static int mixWithBlack(int color, float amount) {
        return ((int) (((color >> 16) & 0xFF) * amount) << 16)
                | ((int) (((color >> 8) & 0xFF) * amount) << 8)
                | (int) ((color & 0xFF) * amount);
    }

    private static int mixWithWhite(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return ((int) (red + (255 - red) * amount) << 16)
                | ((int) (green + (255 - green) * amount) << 8)
                | (int) (blue + (255 - blue) * amount);
    }

    private static void addVertex(VertexConsumer vertices, Matrix4f matrix, Matrix4f inverseMatrix,
            Vec3 cameraPosition, Vec3 worldPosition, int color, float alpha) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int vertexAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Vec3 cameraRelative = worldPosition.subtract(cameraPosition);
        Vector3f localPosition = inverseMatrix.transformPosition(
                new Vector3f((float) cameraRelative.x, (float) cameraRelative.y, (float) cameraRelative.z));
        vertices.addVertex(matrix, localPosition.x(), localPosition.y(), localPosition.z())
                .setColor(red, green, blue, vertexAlpha);
    }

    private static void pruneSamples(ArrayDeque<TrailSample> samples, long now) {
        while (!samples.isEmpty() && now - samples.peekFirst().createdAtNanos > TRAIL_LIFETIME_NANOS) {
            samples.removeFirst();
        }
    }

    private static void clearOnLevelChange(ClientLevel currentLevel) {
        if (observedLevel != currentLevel) {
            TRAILS.clear();
            PENDING_DIRECTIONS.clear();
            SKILL_COLORS.clear();
            TRAIL_LENGTHS.clear();
            observedLevel = currentLevel;
        }
    }

    private record ArmKey(int entityId, HumanoidArm arm) {
    }

    private record TrailKey(int entityId, HumanoidArm arm, int variant) {
    }

    private record PendingTrail(Vec3 direction, Vec3 side) {
    }

    private record BladeAxis(Vector3f base, Vector3f direction) {
    }

    private record TrailSample(Vec3 base, Vec3 tip, long createdAtNanos) {
    }
}
