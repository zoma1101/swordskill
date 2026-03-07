package com.zoma1101.swordskill.client.renderer.layer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

public class SwordTrailAnimationParser {
    public static class AnimationData {
        public final TreeMap<Float, Vector3f> rightArmRot = new TreeMap<>();
        public final TreeMap<Float, Vector3f> rightArmPos = new TreeMap<>();
        public final TreeMap<Float, Vector3f> rightItemRot = new TreeMap<>();
        public final TreeMap<Float, Vector3f> rightItemPos = new TreeMap<>();
        public final float length;

        public AnimationData(float length) {
            this.length = length;
        }
    }

    public static final Map<ResourceLocation, AnimationData> CACHE = new java.util.HashMap<>();

    public static AnimationData getAnimation(ResourceLocation animLocation) {
        if (CACHE.containsKey(animLocation)) {
            return CACHE.get(animLocation);
        }

        float animLength = 0.5f;
        AnimationData data = new AnimationData(animLength);
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            var resource = rm.getResource(ResourceLocation.fromNamespaceAndPath(animLocation.getNamespace(),
                    "player_animation/" + animLocation.getPath() + ".json"));
            if (resource.isPresent()) {
                try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject animations = root.getAsJsonObject("animations");
                    JsonObject animObj = animations.getAsJsonObject(animLocation.getPath());
                    if (animObj == null) {
                        animObj = animations.entrySet().iterator().next().getValue().getAsJsonObject();
                    }

                    if (animObj.has("animation_length")) {
                        animLength = animObj.get("animation_length").getAsFloat();
                    }
                    data = new AnimationData(animLength);

                    JsonObject bones = animObj.getAsJsonObject("bones");
                    if (bones != null) {
                        parseBoneProperty(bones.getAsJsonObject("right_arm"), "rotation", data.rightArmRot, true);
                        parseBoneProperty(bones.getAsJsonObject("right_arm"), "position", data.rightArmPos, false);
                        parseBoneProperty(bones.getAsJsonObject("rightItem"), "rotation", data.rightItemRot, true);
                        parseBoneProperty(bones.getAsJsonObject("rightItem"), "position", data.rightItemPos, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ensureDefault(data.rightArmRot);
        ensureDefault(data.rightArmPos);
        ensureDefault(data.rightItemRot);
        ensureDefault(data.rightItemPos);

        CACHE.put(animLocation, data);
        return data;
    }

    private static void ensureDefault(TreeMap<Float, Vector3f> map) {
        if (map.isEmpty()) {
            map.put(0f, new Vector3f(0, 0, 0));
        }
    }

    private static void parseBoneProperty(JsonObject bone, String property, TreeMap<Float, Vector3f> keyframes,
            boolean isRotation) {
        if (bone != null && bone.has(property)) {
            JsonElement propElement = bone.get(property);
            if (propElement.isJsonObject()) {
                JsonObject propObj = propElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : propObj.entrySet()) {
                    try {
                        float time = Float.parseFloat(entry.getKey());
                        JsonElement kf = entry.getValue();
                        if (kf.isJsonObject() && kf.getAsJsonObject().has("vector")) {
                            var vecArray = kf.getAsJsonObject().getAsJsonArray("vector");
                            keyframes.put(time, parseVector(vecArray, isRotation));
                        } else if (kf.isJsonArray()) {
                            var vecArray = kf.getAsJsonArray();
                            keyframes.put(time, parseVector(vecArray, isRotation));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (propElement.isJsonArray()) {
                var vecArray = propElement.getAsJsonArray();
                keyframes.put(0f, parseVector(vecArray, isRotation));
            }
        }
    }

    private static Vector3f parseVector(com.google.gson.JsonArray vecArray, boolean isRotation) {
        float x = vecArray.get(0).getAsFloat();
        float y = vecArray.get(1).getAsFloat();
        float z = vecArray.get(2).getAsFloat();
        if (isRotation) {
            return new Vector3f((float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
        }
        return new Vector3f(x, y, z);
    }

    public static Vector3f getInterpolatedVector(TreeMap<Float, Vector3f> keyframes, float timeInSeconds) {
        Map.Entry<Float, Vector3f> floor = keyframes.floorEntry(timeInSeconds);
        Map.Entry<Float, Vector3f> ceiling = keyframes.ceilingEntry(timeInSeconds);

        if (floor == null && ceiling == null)
            return new Vector3f(0, 0, 0);
        if (floor == null)
            return ceiling.getValue();
        if (ceiling == null)
            return floor.getValue();
        if (floor.getKey().equals(ceiling.getKey()))
            return floor.getValue();

        float t = (timeInSeconds - floor.getKey()) / (ceiling.getKey() - floor.getKey());
        Vector3f v1 = floor.getValue();
        Vector3f v2 = ceiling.getValue();

        return new Vector3f(
                v1.x + (v2.x - v1.x) * t,
                v1.y + (v2.y - v1.y) * t,
                v1.z + (v2.z - v1.z) * t);
    }
}
