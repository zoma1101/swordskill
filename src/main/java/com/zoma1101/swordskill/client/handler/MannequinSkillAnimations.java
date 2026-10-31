package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.io.Reader;

/**
 * 訓練用マネキンのソードスキルアニメーションを再生する。
 *
 * <p>プレイヤーは {@code playerAnimator} がモデルへ直接アニメーションを適用するが、
 * モブであるマネキンにはその仕組みが働かない。そこでスキル定義の
 * {@code player_animation} を {@link KeyframeAnimationPlayer} で自前評価し、
 * {@link PlayerModel} の腕・胴・頭へ反映する。</p>
 *
 * <p>サーバーは {@link SkillMannequinEntity#getSwingStart()} にスキル使用開始時刻だけを同期する。
 * クライアントはレベル時間との差から再生位置を求め、アニメーションが無いスキルでは
 * 簡易的な斬撃モーションを代わりに再生する。トレイルは
 * {@link WeaponTrailHandler} が実際に描画された剣の向きから生成する。</p>
 */
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MannequinSkillAnimations {
    /** アニメーションが解決できないスキルで代わりに斬撃モーションを出す長さ（tick）。 */
    private static final int FALLBACK_SWING_TICKS = 14;
    /** レベル時間との差がこれ以上なら、古い同期値として無視する。 */
    private static final long MAX_ELAPSED_TICKS = 400L;
    private static final int DEFAULT_COLOR = 0x54D8FF;

    private static final Map<Integer, ActiveAnimation> ACTIVE = new HashMap<>();
    private static ClientLevel observedLevel;

    private MannequinSkillAnimations() {
    }

    /**
     * モデルへアニメーションを適用する。再生中でなければ {@code false} を返す。
     * {@link #stateFor} が返す状態はこの呼び出しの中で最新化される。
     */
    public static boolean apply(SkillMannequinEntity entity, PlayerModel<?> model, float partialTick) {
        ActiveAnimation state = stateFor(entity);
        if (state == null) {
            return false;
        }
        if (state.player() != null) {
            state.player().setupAnim(partialTick);
            applyPart(model.rightArm, state.player(), "rightArm", partialTick);
            applyPart(model.leftArm, state.player(), "leftArm", partialTick);
            applyPart(model.body, state.player(), "body", partialTick);
            applyPart(model.head, state.player(), "head", partialTick);
            applyPart(model.rightLeg, state.player(), "rightLeg", partialTick);
            applyPart(model.leftLeg, state.player(), "leftLeg", partialTick);
        } else {
            applyFallbackSwing(model, state.progress(partialTick));
        }
        // 外層（帽子・袖・上着・ズボン）も本体に追従させる
        model.hat.copyFrom(model.head);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.jacket.copyFrom(model.body);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        return true;
    }

    /** トレイルを出してよいタイミングか（鍔迫り合いのポーズ中は出さない）。 */
    public static boolean isSwinging(SkillMannequinEntity entity) {
        return stateFor(entity) != null;
    }

    @Nullable
    private static ActiveAnimation stateFor(SkillMannequinEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || entity.isRemoved() || !entity.isUsingSkill() || entity.isClashPose()) {
            ACTIVE.remove(entity.getId());
            return null;
        }
        int start = entity.getSwingStart();
        if (start == 0) {
            ACTIVE.remove(entity.getId());
            return null;
        }
        // サーバー・クライアントとも int に切り詰めて差分を取る（gameTime が int を超えても破綻しない）
        int elapsedTicks = (int) level.getGameTime() - start;
        long elapsed = elapsedTicks;
        if (elapsed < 0L || elapsed > MAX_ELAPSED_TICKS) {
            ACTIVE.remove(entity.getId());
            return null;
        }

        ActiveAnimation state = ACTIVE.get(entity.getId());
        if (state == null || state.start() != start) {
            state = createState(entity, start);
            ACTIVE.put(entity.getId(), state);
        }

        KeyframeAnimationPlayer player = state.player();
        if (player != null) {
            // レンダーフレーム毎に呼ばれるため、ゲーム時間ぶんだけ進めて追従させる。
            while (player.isActive() && player.getTick() < elapsed) {
                player.tick();
            }
            if (!player.isActive()) {
                // 終了状態はキャッシュしたまま null を返す（毎フレーム作り直さない）
                return null;
            }
        } else if (elapsed >= FALLBACK_SWING_TICKS) {
            return null;
        }
        return state;
    }

    private static ActiveAnimation createState(SkillMannequinEntity entity, int start) {
        SkillData skill = findSkill(entity.getSkillId());
        String color = null;
        double trailLength = 1.5D;
        KeyframeAnimationPlayer player = null;

        if (skill != null) {
            if (skill.getDataDefinition() != null) {
                color = skill.getDataDefinition().color();
                trailLength = skill.getDataDefinition().trailLength();
            }
            String animationName = skill.getDataDefinition() != null
                    ? skill.getDataDefinition().animation()
                    : skill.getName();
            player = createAnimationPlayer(skill, animationName);
        }

        if (color != null) {
            WeaponTrailHandler.setSkillColor(entity.getId(), color);
            WeaponTrailHandler.setTrailLength(entity.getId(), trailLength);
        } else {
            WeaponTrailHandler.setSkillColor(entity.getId(), "#54D8FF");
            WeaponTrailHandler.setTrailLength(entity.getId(), trailLength);
        }

        int colorValue = DEFAULT_COLOR;
        if (color != null && color.startsWith("#")) {
            try {
                colorValue = Integer.parseInt(color.substring(1), 16);
            } catch (NumberFormatException ignored) {
                colorValue = DEFAULT_COLOR;
            }
        }
        return new ActiveAnimation(start, player, colorValue);
    }

    @Nullable
    private static KeyframeAnimationPlayer createAnimationPlayer(SkillData skill, String animationName) {
        if (animationName == null || animationName.isBlank()) {
            return null;
        }
        KeyframeAnimation animation = resolveAnimation(animationName);
        if (animation == null) {
            // ラッシュ系などは本体名ではなく .start / .move / .finish に分割されていることがある
            animation = resolveAnimation(animationName + ".start");
        }
        if (animation == null) {
            animation = resolveAnimation(animationName + ".move");
        }
        if (animation == null && skill.getType() == SkillData.SkillType.RUSH) {
            animation = resolveAnimation(animationName + ".finish");
        }
        if (animation == null) {
            // ファイル名と中のアニメーション名が一致しない定義（例: cascade）への保険。
            // スキル側のアニメーション指定はファイル名なので、ファイルを読んで中の名前を解決する。
            animation = resolveAnimationFromFile(animationName);
        }
        return animation == null ? null : new KeyframeAnimationPlayer(animation);
    }

    @Nullable
    private static KeyframeAnimation resolveAnimationFromFile(String animationName) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        for (String directory : new String[]{"player_animation", "player_animations"}) {
            ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
                    directory + "/" + animationName + ".json");
            var resource = resourceManager.getResource(file);
            if (resource.isEmpty()) {
                continue;
            }
            try (Reader reader = resource.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null || animations.isEmpty()) {
                    continue;
                }
                String internalName = animations.has(animationName)
                        ? animationName
                        : animations.keySet().iterator().next();
                return resolveAnimation(internalName);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private static KeyframeAnimation resolveAnimation(String name) {
        ResourceLocation id = name.contains(":")
                ? ResourceLocation.tryParse(name)
                : ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, name);
        if (id == null) {
            return null;
        }
        return PlayerAnimationRegistry.getAnimation(id) instanceof KeyframeAnimation animation ? animation : null;
    }

    @Nullable
    private static SkillData findSkill(String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return null;
        }
        for (SkillData skill : SwordSkillRegistry.SKILLS.values()) {
            if (skill.getName().equals(skillId)) {
                return skill;
            }
        }
        return null;
    }

    private static void applyPart(ModelPart part, KeyframeAnimationPlayer player, String bone, float partialTick) {
        Vec3f position = player.get3DTransform(bone, TransformType.POSITION, partialTick,
                new Vec3f(part.x, part.y, part.z));
        part.x = position.getX();
        part.y = position.getY();
        part.z = position.getZ();

        // playerAnimator と同じく、角度はラジアンかつ [-PI, PI] に丸めて渡す
        Vec3f rotation = player.get3DTransform(bone, TransformType.ROTATION, partialTick,
                new Vec3f(clampRadian(part.xRot), clampRadian(part.yRot), clampRadian(part.zRot)));
        part.setRotation(rotation.getX(), rotation.getY(), rotation.getZ());

        Vec3f scale = player.get3DTransform(bone, TransformType.SCALE, partialTick,
                new Vec3f(part.xScale, part.yScale, part.zScale));
        part.xScale = scale.getX();
        part.yScale = scale.getY();
        part.zScale = scale.getZ();
    }

    private static float clampRadian(float value) {
        return Mth.clamp(value, -(float) Math.PI, (float) Math.PI);
    }

    /** アニメーションが無いスキル用の簡易斬撃。右腕を横薙ぎに振り、胴を軽くひねる。 */
    private static void applyFallbackSwing(PlayerModel<?> model, float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        float arc = Mth.sin(t * Mth.PI);
        model.rightArm.xRot = -1.15F - 0.75F * arc;
        model.rightArm.yRot = Mth.lerp(t, -1.05F, 0.95F);
        model.rightArm.zRot = 0.15F;
        model.leftArm.xRot = -0.25F - 0.2F * arc;
        model.leftArm.yRot = 0.25F;
        model.body.yRot = Mth.lerp(t, 0.28F, -0.32F) * arc;
        model.head.yRot = -model.body.yRot * 0.6F;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE.clear();
            observedLevel = null;
            return;
        }
        if (observedLevel != minecraft.level) {
            ACTIVE.clear();
            observedLevel = minecraft.level;
            return;
        }
        Iterator<Map.Entry<Integer, ActiveAnimation>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ActiveAnimation> entry = iterator.next();
            if (!(minecraft.level.getEntity(entry.getKey()) instanceof SkillMannequinEntity mannequin)
                    || !mannequin.isUsingSkill()
                    || mannequin.isClashPose()
                    || mannequin.getSwingStart() != entry.getValue().start()) {
                iterator.remove();
            }
        }
    }

    private record ActiveAnimation(int start, @Nullable KeyframeAnimationPlayer player, int color) {
        float progress(float partialTick) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return 0.0F;
            }
            float elapsed = (int) level.getGameTime() - start + partialTick;
            return Mth.clamp(elapsed / FALLBACK_SWING_TICKS, 0.0F, 1.0F);
        }
    }
}
