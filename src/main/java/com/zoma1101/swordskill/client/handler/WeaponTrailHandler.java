package com.zoma1101.swordskill.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.config.ClientConfig.TrailRenderMode;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
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
    private static final Map<TrailRenderMode, Map<ResourceLocation, RenderType>> TRAIL_RENDER_TYPES =
        new EnumMap<>(TrailRenderMode.class);
    private static final Map<TrailRenderMode, Map<ResourceLocation, RenderType>> TRAIL_DEPTH_TYPES =
        new EnumMap<>(TrailRenderMode.class);
    private static final Map<SkillData.WeaponType, ResourceLocation> TRAIL_TEXTURES = Map.ofEntries(
            textureMapping(SkillData.WeaponType.ONE_HANDED_SWORD, "one_handed_sword"),
            textureMapping(SkillData.WeaponType.TWO_HANDED_SWORD, "two_handed_sword"),
            textureMapping(SkillData.WeaponType.KATANA, "katana"),
            textureMapping(SkillData.WeaponType.RAPIER, "rapier"),
            textureMapping(SkillData.WeaponType.DUALSWORD, "dualsword"),
            textureMapping(SkillData.WeaponType.AXE, "axe"),
            textureMapping(SkillData.WeaponType.CLAW, "claw"),
            textureMapping(SkillData.WeaponType.SPEAR, "spear"),
            textureMapping(SkillData.WeaponType.MACE, "mace"),
            textureMapping(SkillData.WeaponType.SCYTHE, "scythe"),
            textureMapping(SkillData.WeaponType.DAGGER, "dagger"));
    private static final ResourceLocation DEFAULT_TRAIL_TEXTURE = trailTexture("default");
    /** 武器が複数の武器種に該当する場合のテクスチャ優先順。 */
    private static final List<SkillData.WeaponType> TRAIL_TEXTURE_PRIORITY = List.of(
            SkillData.WeaponType.DUALSWORD,
            SkillData.WeaponType.DAGGER,
            SkillData.WeaponType.SPEAR,
            SkillData.WeaponType.RAPIER,
            SkillData.WeaponType.KATANA,
            SkillData.WeaponType.CLAW,
            SkillData.WeaponType.ONE_HANDED_SWORD,
            SkillData.WeaponType.TWO_HANDED_SWORD,
            SkillData.WeaponType.AXE,
            SkillData.WeaponType.MACE,
            SkillData.WeaponType.SCYTHE);
    private static final Map<TrailKey, ArrayDeque<TrailSample>> TRAILS = new HashMap<>();
    private static final Map<ArmKey, PendingTrail> PENDING_DIRECTIONS = new HashMap<>();
    private static final Map<Integer, Integer> SKILL_COLORS = new HashMap<>();
    private static final Map<Integer, Double> TRAIL_LENGTHS = new HashMap<>();
    private static final double DEFAULT_TRAIL_LENGTH = 1.5D;
    // 刃から火花を散らす間隔（ナノ秒）。sparkDensity で割って密度を上げる。
    private static final long BLADE_SPARK_INTERVAL_NANOS = 80_000_000L;
    private static final Map<Integer, Long> NEXT_BLADE_SPARK_NANOS = new HashMap<>();
    private static ClientLevel observedLevel;
    private static boolean irisShadowLookupDone;
    private static Object irisApiInstance;
    private static java.lang.reflect.Method irisShadowPassMethod;

    private WeaponTrailHandler() {
    }

    /**
     * Iris のシャドウパス中かどうか。シャドウパスはライト視点の行列で描画されるため、
     * そこでトレイルのサンプルを記録・描画するとメインパスの行列と混ざって
     * スパイク状・ギザギザの形状になる。Iris が無い場合は常に false。
     */
    private static boolean isShadowPass() {
        if (!irisShadowLookupDone) {
            irisShadowLookupDone = true;
            String[] candidates = {
                    "net.irisshaders.iris.api.v0.IrisApi",
                    "net.irisshaders.iris.api.IrisApi"
            };
            for (String name : candidates) {
                try {
                    Class<?> clazz = Class.forName(name);
                    Object instance = clazz.getMethod("getInstance").invoke(null);
                    irisShadowPassMethod = clazz.getMethod("isRenderingShadowPass");
                    irisApiInstance = instance;
                    break;
                } catch (Throwable ignored) {
                    // Iris が無い / API が異なる場合は候補を続ける
                }
            }
        }
        if (irisApiInstance == null || irisShadowPassMethod == null) {
            return false;
        }
        try {
            Object result = irisShadowPassMethod.invoke(irisApiInstance);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
            irisApiInstance = null;
            irisShadowPassMethod = null;
            return false;
        }
    }

    private static Map.Entry<SkillData.WeaponType, ResourceLocation> textureMapping(
            SkillData.WeaponType type, String name) {
        return Map.entry(type, trailTexture(name));
    }

    private static ResourceLocation trailTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
                "textures/entity/weapon_trail/" + name + ".png");
    }

    /**
     * トレイルの色用 RenderType。
     *
     * <p>Iris 系シェーダーパック（BSL / Photon / Solas / Bliss / Complementary）は
     * 遅延レンダリングの Gバッファ＋ブルームを使うため、エミッシブ＋加算合成の
     * 明るすぎるジオメトリは白飛び・スパイク状ブルームとして破綻する。
     * また加算合成は明るい空に埋もれて見えなくなる。</p>
     *
     * <p>色パスは深度を書き込まない。振りの折り返し地点ではトレイルが自分自身と
     * 重なるため、深度を書くとZファイトして点滅する。深度は別パス
     * （{@link #trailDepthType(ResourceLocation)}）で書き、Photon 等が
     * depth==1.0 の空で上書きするのを防ぐ。</p>
     */
    private static RenderType trailRenderType(ResourceLocation texture) {
        return trailType(TRAIL_RENDER_TYPES, texture, false);
    }

    /** 深度だけを書き込むパス。色は書かないので描画順の影響を受けない。 */
    private static RenderType trailDepthType(ResourceLocation texture) {
        return trailType(TRAIL_DEPTH_TYPES, texture, true);
    }

    private static RenderType trailType(Map<TrailRenderMode, Map<ResourceLocation, RenderType>> cache,
            ResourceLocation texture, boolean depthOnly) {
        TrailRenderMode mode = ClientConfig.trailRenderMode.get();
        Map<ResourceLocation, RenderType> byMode = cache.computeIfAbsent(
                mode, m -> new HashMap<>());
        return byMode.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState.CompositeStateBuilder state = RenderType.CompositeState.builder()
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(depthOnly
                            ? RenderStateShard.DEPTH_WRITE
                            : RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    // entity_translucent / emissive シェーダーは Sampler2(ライトマップ) と
                    // Sampler1(オーバーレイ) を参照する。これらを有効にしないと
                    // 未バインドのテクスチャをサンプルしてしまい、バニラ（シェーダー無し）で
                    // 色が黒くなる（overlayColor が (0,0,0,0) → mix で黒になる）。
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY);
            if (depthOnly) {
                state.setTransparencyState(RenderStateShard.NO_TRANSPARENCY);
            }
            switch (mode) {
                case ADDITIVE -> {
                    state.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER);
                    if (!depthOnly) {
                        state.setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY);
                    }
                }
                case EMISSIVE -> {
                    state.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER);
                    if (!depthOnly) {
                        state.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY);
                    }
                }
                default -> {
                    state.setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER);
                    if (!depthOnly) {
                        state.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY);
                    }
                }
            }
            return RenderType.create(
                    depthOnly ? "swordskill_trail_depth" : "swordskill_trail",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    16384,
                    false,
                    // 提出順（尾→頭）で描画順を固定し、カメラ移動での並び替えによる
                    // ちらつきを防ぐ
                    false,
                    state.createCompositeState(false));
        });
    }

    /** エンティティの武器種から使用するトレイルテクスチャを決める。未知種は default。 */
    private static ResourceLocation trailTextureFor(LivingEntity entity, HumanoidArm arm) {
        ItemStack stack = arm == entity.getMainArm() ? entity.getMainHandItem() : entity.getOffhandItem();
        var detected = WeaponTypeDetector.detectWeaponTypes(stack);
        for (SkillData.WeaponType type : TRAIL_TEXTURE_PRIORITY) {
            if (detected.contains(type)) {
                ResourceLocation texture = TRAIL_TEXTURES.get(type);
                if (texture != null) {
                    return texture;
                }
            }
        }
        return DEFAULT_TRAIL_TEXTURE;
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
        // Iris のシャドウパスはライト視点の行列で発火する。ここで記録・描画すると
        // メインパスのサンプルと混ざってスパイク状・ギザギザの形状になるためスキップする。
        if (isShadowPass()) {
            return;
        }
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

    /**
     * スキルを振っているマネキンの剣からもプレイヤーと同じトレイルを生成する。
     * playerAnimator はモブに適用されないため、体の追加変形は無しでサンプリングする。
     */
    @SubscribeEvent
    public static void onRenderMannequin(RenderLivingEvent.Post<LivingEntity, ?> event) {
        // シャドウパスではライト視点の行列になるため記録・描画しない
        if (isShadowPass()) {
            return;
        }
        if (!(event.getEntity() instanceof SkillMannequinEntity mannequin)) {
            return;
        }
        clearOnLevelChange(Minecraft.getInstance().level);
        long now = System.nanoTime();
        if (mannequin.isInvisible() || !MannequinSkillAnimations.isSwinging(mannequin)) {
            PENDING_DIRECTIONS.keySet().removeIf(key -> key.entityId == mannequin.getId());
            return;
        }
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) {
            return;
        }
        Vec3 renderOrigin = mannequin.getPosition(event.getPartialTick())
                .add(event.getRenderer().getRenderOffset(mannequin, event.getPartialTick()));
        float bodyYaw = Mth.rotLerp(event.getPartialTick(), mannequin.yBodyRotO, mannequin.yBodyRot);
        HumanoidArm mainArm = mannequin.getMainArm();
        HumanoidArm offArm = mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        Vec3f zero = Vec3f.ZERO;
        Vec3f one = new Vec3f(1.0F, 1.0F, 1.0F);
        appendPendingTrail(mannequin, model, mainArm, bodyYaw, renderOrigin, zero, zero, one, now);
        appendPendingTrail(mannequin, model, offArm, bodyYaw, renderOrigin, zero, zero, one, now);
        PENDING_DIRECTIONS.keySet().removeIf(key -> key.entityId == mannequin.getId());
        renderTrails(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), mannequin, now);
    }

    /** Captures the actual held-item pose after PlayerAnimator has applied rightItem. */
    public static void captureRenderedItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
            boolean leftHand, PoseStack heldItemPose, net.minecraft.world.level.Level level, int seed) {
        // シャドウパスでは heldItemPose がライト視点になり、刃の向きが壊れるため記録しない
        if (isShadowPass()) {
            return;
        }
        if (stack.isEmpty()
                || (context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        && context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }
        boolean animated = entity instanceof AbstractClientPlayer player
                && player instanceof IAnimatedPlayer animatedPlayer
                && animatedPlayer.playerAnimator_getAnimation().isActive();
        boolean swingingMannequin = entity instanceof SkillMannequinEntity mannequin
                && MannequinSkillAnimations.isSwinging(mannequin);
        if (!animated && !swingingMannequin) {
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
        int entityId = entity.getId();
        float trailLength = (float) (double) TRAIL_LENGTHS.getOrDefault(entityId, DEFAULT_TRAIL_LENGTH);
        direction.normalize(trailLength);

        Vec3 bladeDirection = new Vec3(direction.x, direction.y, direction.z);
        Vec3 unitDirection = bladeDirection.normalize();
        // 3本のトレイルは常に水平方向(左右)へ並べる
        Vec3 side = horizontalPerpendicular(unitDirection);

        HumanoidArm arm = leftHand ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        PENDING_DIRECTIONS.put(new ArmKey(entityId, arm),
                new PendingTrail(bladeDirection, side));
    }

    private static void appendPendingTrail(LivingEntity entity, PlayerModel<?> model, HumanoidArm arm, float bodyYaw,
            Vec3 renderOrigin, Vec3f bodyPosition, Vec3f bodyRotation, Vec3f bodyScale, long now) {
        ArmKey armKey = new ArmKey(entity.getId(), arm);
        PendingTrail pending = PENDING_DIRECTIONS.remove(armKey);
        ItemStack heldItem = arm == entity.getMainArm() ? entity.getMainHandItem() : entity.getOffhandItem();
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
            TrailKey key = new TrailKey(entity.getId(), arm, variant);
            ArrayDeque<TrailSample> samples = TRAILS.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            TrailSample last = samples.peekLast();
            boolean moved = last == null
                    || last.base.distanceToSqr(base) + last.tip.distanceToSqr(tip) > 0.0000025;
            // チャージ中は静止していてもトレイルが見えるように、刃の進行方向と逆側に
            // 短い合成サンプルを追加してセグメントを生成する
            if (moved || ClientForgeHandler.isCharging) {
                if (ClientForgeHandler.isCharging && !moved && last != null) {
                    Vec3 dir = pending.direction();
                    double len = dir.length();
                    if (len > 1.0E-6) {
                        Vec3 back = dir.scale(-0.4D / len);
                        samples.addLast(new TrailSample(base.add(back), tip.add(back),
                                now - 16_000_000L));
                    }
                }
                samples.addLast(new TrailSample(base, tip, now));
                if (variant == 0 && last != null) {
                    emitBladeSparks(entity, base, tip, tip.subtract(last.tip), now, last.createdAtNanos());
                }
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

    /**
     * 振っている武器の刃に沿って、トレイルと同色の火花を散らす。
     * 火花の速さは刃先の移動速度（ブロック/秒）から求めるので、振り抜くほど速く流れる。
     */
    private static void emitBladeSparks(LivingEntity entity, Vec3 base, Vec3 tip, Vec3 motion, long now,
            long previousNanos) {
        boolean coloured = ClientConfig.trailSparks.get();
        boolean streaks = ClientConfig.sparkStreaks.get();
        if (!coloured && !streaks) {
            return;
        }
        double density = ClientConfig.sparkDensity.get();
        if (density <= 0.0D) {
            return;
        }
        long next = NEXT_BLADE_SPARK_NANOS.getOrDefault(entity.getId(), 0L);
        if (now < next) {
            return;
        }
        NEXT_BLADE_SPARK_NANOS.put(entity.getId(), now + (long) (BLADE_SPARK_INTERVAL_NANOS / density));
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Vec3 blade = tip.subtract(base);
        if (blade.lengthSqr() < 1.0E-6D) {
            return;
        }
        RandomSource random = level.random;
        Vec3 flow = motion.lengthSqr() > 1.0E-9D ? motion.normalize() : blade.normalize();
        // 刃先の速さをブロック/秒で求めて、1tickあたりの火花の速さへ変換する
        double elapsedSeconds = Math.max((now - previousNanos) / 1.0E9D, 1.0E-4D);
        double tipSpeed = motion.length() / elapsedSeconds;
        float sparkSpeed = (float) Mth.clamp(0.55D + tipSpeed * 0.08D, 0.55D, 1.60D);
        int color = saturateColor(SKILL_COLORS.getOrDefault(entity.getId(), 0x54D8FF));

        if (coloured) {
            int count = Mth.clamp(1 + (random.nextFloat() < density ? 1 : 0), 1, 3);
            for (int index = 0; index < count; index++) {
                double along = 0.35D + random.nextDouble() * 0.65D;
                Vec3 position = base.lerp(tip, along);
                Vec3 normal = randomPerpendicular(blade, random);
                Vec3 velocity = flow.scale(sparkSpeed * (0.7D + random.nextDouble() * 0.6D))
                        .add(normal.scale(0.10D + random.nextDouble() * 0.30D))
                        .add(0.0D, 0.03D + random.nextDouble() * 0.10D, 0.0D);
                float scale = 0.55F + random.nextFloat() * 0.5F;
                level.addParticle(new SkillSparkParticleOptions(color, scale),
                        position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
            }
        }

        if (streaks && random.nextFloat() < 0.45F * (float) density) {
            double along = 0.55D + random.nextDouble() * 0.45D;
            Vec3 position = base.lerp(tip, along);
            Vec3 normal = randomPerpendicular(blade, random);
            Vec3 velocity = flow.scale(sparkSpeed * (0.9D + random.nextDouble() * 0.8D))
                    .add(normal.scale(0.16D + random.nextDouble() * 0.34D))
                    .add(0.0D, 0.03D + random.nextDouble() * 0.08D, 0.0D);
            level.addParticle(new SparkStreakParticleOptions(color, 0.8F + random.nextFloat() * 0.5F),
                    position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
        }
    }

    /** 刃軸に直交するランダムな単位ベクトル。火花を刃から外側へ散らすのに使う。 */
    private static Vec3 randomPerpendicular(Vec3 axis, RandomSource random) {
        Vec3 unit = axis.normalize();
        Vec3 reference = Math.abs(unit.y) > 0.95D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = unit.cross(reference);
        if (side.lengthSqr() < 1.0E-6D) {
            side = unit.cross(new Vec3(0.0D, 0.0D, 1.0D));
        }
        Vec3 perpendicular = side.normalize();
        Vec3 other = unit.cross(perpendicular).normalize();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return perpendicular.scale(Math.cos(angle)).add(other.scale(Math.sin(angle)));
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
            int packedLight, LivingEntity entity, long now) {
        Matrix4f matrix = poseStack.last().pose();
        Matrix4f inverseMatrix = new Matrix4f(matrix).invert();
        int skillColor = saturateColor(SKILL_COLORS.getOrDefault(entity.getId(), 0x54D8FF));
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (Map.Entry<TrailKey, ArrayDeque<TrailSample>> entry : TRAILS.entrySet()) {
            if (entry.getKey().entityId != entity.getId()) {
                continue;
            }
            ArrayDeque<TrailSample> samples = entry.getValue();
            pruneSamples(samples, now);
            if (samples.size() < 2) {
                continue;
            }

            // 爪武器は 3 重トレイルで発光しすぎるため、明るさを設定で抑える
            HumanoidArm mainArm = entity.getMainArm();
            ItemStack heldItem = (entry.getKey().arm == mainArm)
                    ? entity.getMainHandItem() : entity.getOffhandItem();
            boolean claw = isClawWeapon(heldItem);
            float clawBrightness = claw ? (float) ClientConfig.clawTrailBrightness.get().doubleValue() : 1.0F;
            float trailOpacity = (float) ClientConfig.trailOpacity.get().doubleValue();
            float trailGlow = (float) ClientConfig.trailGlow.get().doubleValue();
            // 加算モードでは層が重なって白飛びしやすいため、強度を設定で抑える
            float modeScale = 1.0F;
            if (ClientConfig.trailRenderMode.get() == TrailRenderMode.ADDITIVE) {
                modeScale = (float) ClientConfig.trailAdditiveIntensity.get().doubleValue();
            }

            // 武器種ごとにテクスチャ（＝RenderType）を選ぶ
            ResourceLocation trailTexture = trailTextureFor(entity, entry.getKey().arm);
            TrailSample[] points = samples.toArray(TrailSample[]::new);
            int subdivisions = 6;

            // 1) 色パス（深度書き込みなし）。折り返しで自分自身と重なってもZファイトしない。
            // 2) 深度パス（色なし）。Photon 等が depth==1.0 の空で上書きするのを防ぐ。
            // getBuffer は別 RenderType を要求した時点で前のバッファを描画するため、
            // 必ず「色 → 深度」の順に積む。
            VertexConsumer colorVertices = buffers.getBuffer(trailRenderType(trailTexture));
            emitTrailSegments(colorVertices, points, subdivisions, matrix, inverseMatrix,
                    cameraPosition, skillColor, now, clawBrightness, trailOpacity, modeScale, trailGlow);
            VertexConsumer depthVertices = buffers.getBuffer(trailDepthType(trailTexture));
            emitTrailSegments(depthVertices, points, subdivisions, matrix, inverseMatrix,
                    cameraPosition, skillColor, now, clawBrightness, trailOpacity, modeScale, trailGlow);
        }
    }

    private static void emitTrailSegments(VertexConsumer vertices, TrailSample[] points, int subdivisions,
            Matrix4f matrix, Matrix4f inverseMatrix, Vec3 cameraPosition, int skillColor, long now,
            float clawBrightness, float trailOpacity, float modeScale, float trailGlow) {
        int totalPoints = (points.length - 1) * subdivisions;
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
                // 1枚描きになったぶん、以前のレイヤー合成より減衰を緩めて明るさを保つ
                float ageAlpha = (float) Math.pow(1.0F - age, 0.8F);
                float lengthT = (i + to) / (points.length - 1.0F);
                float lengthAlpha = 0.70F + 0.30F * lengthT;
                float alpha = ageAlpha * lengthAlpha * clawBrightness * trailOpacity * modeScale;
                if (alpha > 0.01F) {
                    // V はテイル先端(古い側)からトレイルの頭(新しい側)へ 0..1
                    int k = i * subdivisions + step;
                    float v0 = k / (float) totalPoints;
                    float v1 = (k + 1) / (float) totalPoints;
                    renderRibbonSegment(vertices, matrix, fromSample, toSample, alpha,
                            inverseMatrix, cameraPosition, skillColor, v0, v1, lengthT,
                            trailGlow);
                }
            }
        }
    }

    private static TrailSample interpolate(TrailSample p0, TrailSample p1, TrailSample p2, TrailSample p3,
            float t) {
        // 求心Catmull-Rom(alpha=0.5)。フレーム間隔が不均一でもループや尖り(ギザギザ)が出ない。
        // base と tip で同じノット列を使い、刃幅が崩れないようにする。
        double t0 = 0.0;
        double t1 = t0 + knot(p0, p1);
        double t2 = t1 + knot(p1, p2);
        double t3 = t2 + knot(p2, p3);
        double tt = t1 + (t2 - t1) * t;
        Vec3 base = centripetal(p0.base, p1.base, p2.base, p3.base, t0, t1, t2, t3, tt);
        Vec3 tip = centripetal(p0.tip, p1.tip, p2.tip, p3.tip, t0, t1, t2, t3, tt);
        return new TrailSample(base, tip,
                p1.createdAtNanos + (long) ((p2.createdAtNanos - p1.createdAtNanos) * t));
    }

    /** 求心パラメータのノット間隔（距離の平方根。0 は最小値でガード）。 */
    private static double knot(TrailSample a, TrailSample b) {
        double d = 0.5 * (a.base.distanceTo(b.base) + a.tip.distanceTo(b.tip));
        return Math.max(1.0E-4, Math.sqrt(d));
    }

    private static Vec3 centripetal(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            double t0, double t1, double t2, double t3, double t) {
        Vec3 a1 = p0.lerp(p1, (t - t0) / (t1 - t0));
        Vec3 a2 = p1.lerp(p2, (t - t1) / (t2 - t1));
        Vec3 a3 = p2.lerp(p3, (t - t2) / (t3 - t2));
        Vec3 b1 = a1.lerp(a2, (t - t0) / (t2 - t0));
        Vec3 b2 = a2.lerp(a3, (t - t1) / (t3 - t1));
        return b1.lerp(b2, (t - t1) / (t2 - t1));
    }

    /**
     * テクスチャ付きリボンを1枚で描く。
     * U (0..1) は刃幅の根元→先端、V (v0..v1) はトレイルの弧に沿った方向。
     * headT はトレイルの根元(古い=0)→頭(新しい=1)。
     * 頭（刃元付近）だけ白く強く光らせ、尾側はスキル本来の色を濃く見せる。
     *
     * <p>以前は同じ平面に5枚のレイヤー（本体／中間色／芯／外周グロー／白芯）を
     * 重ねていたが、深度を書き込む半透明描画ではレイヤー同士がZファイトし、
     * 上辺のギザギザや内部の黒い亀裂として現れる。発光は1枚の頂点色
     * （スキル色→白へのグラデーション）で表現して重なりを無くす。</p>
     */
    private static void renderRibbonSegment(VertexConsumer vertices, Matrix4f matrix,
            TrailSample previous, TrailSample current, float alpha, Matrix4f inverseMatrix,
            Vec3 cameraPosition, int tint, float v0, float v1, float headT,
            float glow) {
        // 白い発光エリア: 頭のごく狭い範囲だけ
        float hot = smoothstep(headT, 0.84F, 0.97F);
        // 中間色エリア: 頭に向かってスキル色から白へ寄せる
        float mid = smoothstep(headT, 0.62F, 0.72F);

        // 発光の強さ(glow): 白さと不透明度をスケール
        // 1枚描きでは尾側まで白を混ぜないと暗く見えるため、全体に白を載せる
        float whiteAmount = Mth.clamp((0.22F + 0.28F * mid + 0.50F * hot) * glow, 0.0F, 1.0F);
        int color = mixWithWhite(tint, whiteAmount);
        float alphaScale = Mth.clamp(0.95F + 0.05F * glow, 0.0F, 1.0F);

        emitTrailLayer(vertices, matrix, inverseMatrix, cameraPosition, previous, current,
                color, alpha * alphaScale, 0.0F, 1.0F, v0, v1);
    }

    private static float smoothstep(float x, float edge0, float edge1) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** 刃の根元→先端を fromFrac..toFrac の範囲に切り出して 1 クワッド描画する。 */
    private static void emitTrailLayer(VertexConsumer vertices, Matrix4f matrix, Matrix4f inverseMatrix,
            Vec3 cameraPosition, TrailSample previous, TrailSample current, int tint, float alpha,
            float fromFrac, float toFrac, float v0, float v1) {
        Vec3 previousSpan = previous.tip.subtract(previous.base);
        Vec3 currentSpan = current.tip.subtract(current.base);
        Vec3 previousBase = previous.base.add(previousSpan.scale(fromFrac));
        Vec3 previousTip = previous.base.add(previousSpan.scale(toFrac));
        Vec3 currentBase = current.base.add(currentSpan.scale(fromFrac));
        Vec3 currentTip = current.base.add(currentSpan.scale(toFrac));
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, previousBase, tint, alpha, 0.0F, v0);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, previousTip, tint, alpha, 1.0F, v0);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, currentTip, tint, alpha, 1.0F, v1);
        addVertex(vertices, matrix, inverseMatrix, cameraPosition, currentBase, tint, alpha, 0.0F, v1);
    }

    /** 色を白へ寄せる（加算合成の発光を強めるため）。 */
    private static int mixWithWhite(int color, float amount) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        red = Mth.clamp(Math.round(red + (255 - red) * amount), 0, 255);
        green = Mth.clamp(Math.round(green + (255 - green) * amount), 0, 255);
        blue = Mth.clamp(Math.round(blue + (255 - blue) * amount), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    public static int saturateColor(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int gray = (red + green + blue) / 3;
        red = Mth.clamp((int) (gray + (red - gray) * 1.35F), 0, 255);
        green = Mth.clamp((int) (gray + (green - gray) * 1.35F), 0, 255);
        blue = Mth.clamp((int) (gray + (blue - gray) * 1.35F), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private static void addVertex(VertexConsumer vertices, Matrix4f matrix, Matrix4f inverseMatrix,
            Vec3 cameraPosition, Vec3 worldPosition, int color, float alpha, float u, float v) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        int vertexAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        Vec3 cameraRelative = worldPosition.subtract(cameraPosition);
        Vector3f localPosition = inverseMatrix.transformPosition(
                new Vector3f((float) cameraRelative.x, (float) cameraRelative.y, (float) cameraRelative.z));
        vertices.addVertex(matrix, localPosition.x(), localPosition.y(), localPosition.z())
                .setColor(red, green, blue, vertexAlpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(0.0F, 1.0F, 0.0F);
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
            NEXT_BLADE_SPARK_NANOS.clear();
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
