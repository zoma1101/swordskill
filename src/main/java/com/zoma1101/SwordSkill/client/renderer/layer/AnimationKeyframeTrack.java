package com.zoma1101.swordskill.client.renderer.layer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GeckoLib形式のJSONアニメーションキーフレームを保持し、
 * 任意の時刻 t（秒）における補間値を返すクラス。
 */
public class AnimationKeyframeTrack {

    public record Keyframe(float time, Vector3f value, String easing) {
    }

    private final List<Keyframe> frames = new ArrayList<>();

    public void addKeyframe(float time, Vector3f value, String easing) {
        int insertAt = frames.size();
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i).time() > time) {
                insertAt = i;
                break;
            }
        }
        frames.add(insertAt, new Keyframe(time, value, easing));
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public Vector3f evaluate(float t) {
        return evaluate(t, new Vector3f());
    }

    public Vector3f evaluate(float t, Vector3f dest) {
        if (frames.isEmpty()) {
            return dest.set(0, 0, 0);
        }
        if (frames.size() == 1) {
            return dest.set(frames.get(0).value());
        }
        if (t <= frames.get(0).time()) {
            return dest.set(frames.get(0).value());
        }
        if (t >= frames.get(frames.size() - 1).time()) {
            return dest.set(frames.get(frames.size() - 1).value());
        }

        // Binary Search
        int low = 0;
        int high = frames.size() - 2;
        int index = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (frames.get(mid).time() <= t && frames.get(mid + 1).time() >= t) {
                index = mid;
                break;
            } else if (frames.get(mid).time() > t) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        Keyframe prev = frames.get(index);
        Keyframe next = frames.get(index + 1);

        float span = next.time() - prev.time();
        if (span <= 0f) {
            return dest.set(prev.value());
        }

        float alpha = applyEasing((t - prev.time()) / span, prev.easing());
        return dest.set(prev.value()).lerp(next.value(), alpha);
    }

    private static float applyEasing(float t, String easing) {
        if (easing == null)
            return t;
        return switch (easing) {
            case "step" -> 0f; // ステップイージング：次のキーフレームまで値を維持する
            case "easeInQuad" -> t * t;
            case "easeOutQuad" -> t * (2f - t);
            case "easeInOutQuad" -> t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
            case "easeInCubic" -> t * t * t;
            case "easeOutCubic" -> {
                float f = t - 1f;
                yield f * f * f + 1f;
            }
            case "easeInQuart" -> t * t * t * t;
            case "easeOutQuart" -> {
                float f = t - 1f;
                yield 1f - f * f * f * f;
            }
            case "easeInSine" -> 1f - (float) Math.cos(t * Math.PI / 2f);
            case "easeOutSine" -> (float) Math.sin(t * Math.PI / 2f);
            default -> t;
        };
    }

    public record AnimationData(AnimationKeyframeTrack armRotTrack, AnimationKeyframeTrack armPosTrack,
            AnimationKeyframeTrack bodyRotTrack, AnimationKeyframeTrack bodyPosTrack,
            AnimationKeyframeTrack itemRotTrack, AnimationKeyframeTrack itemPosTrack,
            AnimationKeyframeTrack leftArmRotTrack, AnimationKeyframeTrack leftArmPosTrack,
            AnimationKeyframeTrack leftItemRotTrack, AnimationKeyframeTrack leftItemPosTrack,
            AnimationKeyframeTrack trailTrack, AnimationKeyframeTrack trailRotTrack,
            AnimationKeyframeTrack trailScaleTrack, float animationLength) {

        /**
         * このアニメーションデータを左右反転させた新しいインスタンスを生成して返す。
         *
         * @return 左右反転した AnimationData
         */
        public AnimationData getMirrored() {
            // 回転トラックはY軸とZ軸を反転
            AnimationKeyframeTrack mirroredArmRot = mirrorTrack(this.armRotTrack, true, false);
            AnimationKeyframeTrack mirroredBodyRot = mirrorTrack(this.bodyRotTrack, true, false);
            // 位置トラックはX軸を反転
            AnimationKeyframeTrack mirroredArmPos = mirrorTrack(this.armPosTrack, false, true);
            AnimationKeyframeTrack mirroredBodyPos = mirrorTrack(this.bodyPosTrack, false, true);
            AnimationKeyframeTrack mirroredItemRot = mirrorTrack(this.itemRotTrack, true, false);
            AnimationKeyframeTrack mirroredItemPos = mirrorTrack(this.itemPosTrack, false, true);
            AnimationKeyframeTrack mirroredTrail = mirrorTrack(this.trailTrack, false, false);
            AnimationKeyframeTrack mirroredTrailRot = mirrorTrack(this.trailRotTrack, false, false);
            AnimationKeyframeTrack mirroredTrailScale = mirrorTrack(this.trailScaleTrack, false, false);

            return new AnimationData(mirroredArmRot, mirroredArmPos, mirroredBodyRot, mirroredBodyPos,
                    mirroredItemRot, mirroredItemPos,
                    new AnimationKeyframeTrack(), new AnimationKeyframeTrack(),
                    new AnimationKeyframeTrack(), new AnimationKeyframeTrack(),
                    mirroredTrail, mirroredTrailRot, mirroredTrailScale, this.animationLength);
        }

        /**
         * 指定されたトラックのキーフレーム値を反転させた新しいトラックを返すヘルパーメソッド。
         *
         * @param original  元のトラック
         * @param invertRot Y軸とZ軸を反転させるか（回転用）
         * @param invertPos X軸を反転させるか（位置用）
         * @return 反転した新しい AnimationKeyframeTrack
         */
        private AnimationKeyframeTrack mirrorTrack(AnimationKeyframeTrack original, boolean invertRot,
                boolean invertPos) {
            AnimationKeyframeTrack mirrored = new AnimationKeyframeTrack();
            for (Keyframe kf : original.frames) {
                Vector3f mirroredValue = new Vector3f(kf.value());
                if (invertPos) {
                    mirroredValue.x = -mirroredValue.x;
                }
                if (invertRot) {
                    // Y軸とZ軸を反転させてミラートラックを生成
                    mirroredValue.y = -mirroredValue.y;
                    mirroredValue.z = -mirroredValue.z;
                }
                mirrored.addKeyframe(kf.time(), mirroredValue, kf.easing());
            }
            return mirrored;
        }

        public static AnimationData load(String animationName) {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "swordskill", "player_animation/" + animationName + ".json");
            try {
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                try (InputStream is = rm.getResourceOrThrow(loc).open();
                        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject animations = root.getAsJsonObject("animations");
                    Map.Entry<String, JsonElement> firstEntry = animations.entrySet().iterator().next();
                    JsonObject anim = firstEntry.getValue().getAsJsonObject();

                    float animLength = anim.has("animation_length")
                            ? anim.get("animation_length").getAsFloat()
                            : 1.0f;

                    JsonObject bones = anim.getAsJsonObject("bones");

                    AnimationKeyframeTrack armRot = parseTrack(bones, "right_arm", "rotation");
                    AnimationKeyframeTrack armPos = parseTrack(bones, "right_arm", "position");
                    AnimationKeyframeTrack bodyRot = parseTrack(bones, "body", "rotation");
                    AnimationKeyframeTrack bodyPos = parseTrack(bones, "body", "position");
                    AnimationKeyframeTrack itemRot = parseTrack(bones, "rightItem", "rotation");
                    AnimationKeyframeTrack itemPos = parseTrack(bones, "rightItem", "position");

                    AnimationKeyframeTrack leftArmRot = parseTrack(bones, "left_arm", "rotation");
                    AnimationKeyframeTrack leftArmPos = parseTrack(bones, "left_arm", "position");
                    AnimationKeyframeTrack leftItemRot = parseTrack(bones, "leftItem", "rotation");
                    AnimationKeyframeTrack leftItemPos = parseTrack(bones, "leftItem", "position");

                    AnimationKeyframeTrack trailToggle = parseTrack(bones, "trail", "position");
                    AnimationKeyframeTrack trailRot = parseTrack(bones, "trail", "rotation");
                    AnimationKeyframeTrack trailScale = parseTrack(bones, "trail", "scale");

                    return new AnimationData(armRot, armPos, bodyRot, bodyPos, itemRot, itemPos,
                            leftArmRot, leftArmPos, leftItemRot, leftItemPos,
                            trailToggle, trailRot, trailScale, animLength);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private static AnimationKeyframeTrack parseTrack(JsonObject bones,
                String boneName,
                String trackName) {
            AnimationKeyframeTrack track = new AnimationKeyframeTrack();
            if (!bones.has(boneName))
                return track;

            JsonObject bone = bones.getAsJsonObject(boneName);
            if (!bone.has(trackName))
                return track;

            JsonObject trackObj = bone.getAsJsonObject(trackName);

            if (trackObj.has("vector")) {
                Vector3f v = parseVector(trackObj.getAsJsonArray("vector"));
                track.addKeyframe(0f, v, "linear");
                return track;
            }

            for (Map.Entry<String, JsonElement> entry : trackObj.entrySet()) {
                try {
                    float time = Float.parseFloat(entry.getKey());
                    JsonElement kfElement = entry.getValue();

                    if (kfElement.isJsonArray()) {
                        Vector3f vec = parseVector(kfElement.getAsJsonArray());
                        track.addKeyframe(time, vec, "linear");
                    } else if (kfElement.isJsonObject()) {
                        JsonObject kf = kfElement.getAsJsonObject();
                        JsonArray vecArray = null;

                        if (kf.has("vector")) {
                            vecArray = kf.getAsJsonArray("vector");
                        } else if (kf.has("post")) {
                            JsonElement post = kf.get("post");
                            if (post.isJsonArray()) {
                                vecArray = post.getAsJsonArray();
                            } else if (post.isJsonObject() && post.getAsJsonObject().has("vector")) {
                                vecArray = post.getAsJsonObject().getAsJsonArray("vector");
                            }
                        }

                        if (vecArray != null) {
                            Vector3f vec = parseVector(vecArray);
                            String easing = kf.has("easing") ? kf.get("easing").getAsString() : "linear";
                            track.addKeyframe(time, vec, easing);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return track;
        }

        private static Vector3f parseVector(JsonArray arr) {
            return new Vector3f(
                    arr.get(0).getAsFloat(),
                    arr.get(1).getAsFloat(),
                    arr.get(2).getAsFloat());
        }
    }
}
