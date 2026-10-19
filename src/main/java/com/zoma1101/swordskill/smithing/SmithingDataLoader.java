package com.zoma1101.swordskill.smithing;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 鍛冶定義（素材・部品・武器の形）をデータパックから読み込むリロードリスナー。
 *
 * <ul>
 *   <li>{@code data/<namespace>/smithing_material/<id>.json}</li>
 *   <li>{@code data/<namespace>/smithing_part/<id>.json}</li>
 *   <li>{@code data/<namespace>/weapon_pattern/<id>.json}</li>
 * </ul>
 *
 * <p>解析は共通の関数にまとめてあり、サーバーからの同期
 * （{@link SmithingDefinitions#applyJson(JsonObject)}）でも同じ関数を使う。</p>
 */
public final class SmithingDataLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private SmithingDataLoader() {
    }

    /** {@code smithing_material} フォルダを読む。 */
    public static final class Materials extends SimpleJsonResourceReloadListener {

        public Materials() {
            super(GSON, "smithing_material");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, @NotNull ResourceManager resourceManager,
                @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, SmithingMaterial> loaded = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                ResourceLocation file = entry.getKey();
                if (file.getPath().startsWith("_")) {
                    continue;
                }
                try {
                    SmithingMaterial material = parseMaterial(file,
                            GsonHelper.convertToJsonObject(entry.getValue(), "smithing material"));
                    loaded.put(material.id(), material);
                } catch (Exception exception) {
                    LOGGER.error("Failed to parse smithing material {}", file, exception);
                }
            }
            SmithingDefinitions.setMaterials(loaded);
            SmithingDefinitions.markSyncPending();
            LOGGER.info("Loaded {} smithing materials", loaded.size());
        }
    }

    /** {@code smithing_part} フォルダを読む。 */
    public static final class Parts extends SimpleJsonResourceReloadListener {

        public Parts() {
            super(GSON, "smithing_part");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, @NotNull ResourceManager resourceManager,
                @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, SmithingPart> loaded = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                ResourceLocation file = entry.getKey();
                if (file.getPath().startsWith("_")) {
                    continue;
                }
                try {
                    SmithingPart part = parsePart(file,
                            GsonHelper.convertToJsonObject(entry.getValue(), "smithing part"));
                    loaded.put(part.id(), part);
                } catch (Exception exception) {
                    LOGGER.error("Failed to parse smithing part {}", file, exception);
                }
            }
            SmithingDefinitions.setParts(loaded);
            SmithingDefinitions.markSyncPending();
            LOGGER.info("Loaded {} smithing parts", loaded.size());
        }
    }

    /** {@code weapon_pattern} フォルダを読む。 */
    public static final class Patterns extends SimpleJsonResourceReloadListener {

        public Patterns() {
            super(GSON, "weapon_pattern");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> objects, @NotNull ResourceManager resourceManager,
                @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, WeaponPattern> loaded = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
                ResourceLocation file = entry.getKey();
                if (file.getPath().startsWith("_")) {
                    continue;
                }
                try {
                    WeaponPattern pattern = parsePattern(file,
                            GsonHelper.convertToJsonObject(entry.getValue(), "weapon pattern"));
                    loaded.put(pattern.id(), pattern);
                } catch (Exception exception) {
                    LOGGER.error("Failed to parse weapon pattern {}", file, exception);
                }
            }
            SmithingDefinitions.setPatterns(loaded);
            LOGGER.info("Loaded {} weapon patterns", loaded.size());
        }
    }

    // ------------------------------------------------------------------
    // 解析
    // ------------------------------------------------------------------

    public static SmithingMaterial parseMaterial(ResourceLocation file, JsonObject json) {
        ResourceLocation id = readId(json, "id", file);
        Map<SkillData.WeaponType, ResourceLocation> blades = new EnumMap<>(SkillData.WeaponType.class);
        if (json.has("blades")) {
            JsonObject map = GsonHelper.getAsJsonObject(json, "blades");
            for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                SkillData.WeaponType weapon = parseWeapon(entry.getKey());
                if (weapon == SkillData.WeaponType.NONE) {
                    LOGGER.warn("Unknown weapon type {} in {}", entry.getKey(), file);
                    continue;
                }
                blades.put(weapon, ResourceLocation.parse(entry.getValue().getAsString()));
            }
        }
        return new SmithingMaterial(
                id,
                GsonHelper.getAsInt(json, "tier", 0),
                GsonHelper.getAsDouble(json, "power", 1.0D),
                GsonHelper.getAsDouble(json, "weight", 10.0D),
                parseColor(GsonHelper.getAsString(json, "color_main", "#FFFFFF")),
                parseColor(GsonHelper.getAsString(json, "color_sub", "#808080")),
                readIds(json, "items"),
                readIds(json, "tags"),
                GsonHelper.getAsBoolean(json, "special", false),
                Map.copyOf(blades),
                readId(json, "guard", null),
                readId(json, "handle", null));
    }

    public static SmithingPart parsePart(ResourceLocation file, JsonObject json) {
        ResourceLocation id = readId(json, "id", file);
        SmithingPartKind kind = SmithingPartKind.byName(GsonHelper.getAsString(json, "kind", "BLADE"));
        if (kind == null) {
            throw new IllegalArgumentException("unknown smithing part kind");
        }
        List<SkillData.WeaponType> weapons = new ArrayList<>();
        if (json.has("weapons")) {
            JsonArray array = json.getAsJsonArray("weapons");
            for (JsonElement element : array) {
                try {
                    weapons.add(SkillData.WeaponType.valueOf(element.getAsString().trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    LOGGER.error("Unknown weapon type {} in {}", element, file);
                }
            }
        }
        return new SmithingPart(
                id,
                kind,
                List.copyOf(weapons),
                GsonHelper.getAsString(json, "texture", id.getPath()),
                GsonHelper.getAsDouble(json, "power", 1.0D),
                GsonHelper.getAsDouble(json, "weight", 1.0D),
                GsonHelper.getAsDouble(json, "attack_speed", -2.4D),
                SmithingColorSlot.byName(GsonHelper.getAsString(json, "color_slot", "MAIN")),
                GsonHelper.getAsInt(json, "tier", 0));
    }

    public static WeaponPattern parsePattern(ResourceLocation file, JsonObject json) {
        ResourceLocation id = readId(json, "id", file);
        List<String> rows = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "pattern")) {
            rows.add(element.getAsString());
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("weapon pattern needs a pattern");
        }
        Map<Character, WeaponPattern.Cell> key = new HashMap<>();
        JsonObject keyJson = GsonHelper.getAsJsonObject(json, "key");
        for (Map.Entry<String, JsonElement> entry : keyJson.entrySet()) {
            if (entry.getKey().isEmpty() || !entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject cellJson = entry.getValue().getAsJsonObject();
            SmithingRole role = SmithingRole.byName(GsonHelper.getAsString(cellJson, "role", ""));
            if (role == null) {
                throw new IllegalArgumentException("unknown role in weapon pattern " + file);
            }
            key.put(entry.getKey().charAt(0),
                    new WeaponPattern.Cell(role, readId(cellJson, "item", null), readId(cellJson, "tag", null)));
        }
        SkillData.WeaponType weapon = parseWeapon(GsonHelper.getAsString(json, "weapon", ""));
        if (weapon == SkillData.WeaponType.NONE) {
            throw new IllegalArgumentException("weapon pattern " + file + " needs a weapon type");
        }
        return new WeaponPattern(
                id,
                List.copyOf(rows),
                Map.copyOf(key),
                weapon,
                readId(json, "blade", null),
                readId(json, "guard", null),
                readId(json, "handle", null));
    }

    /** サーバーから配られた定義（{@link SmithingDefinitions#toJson()} の形）を読み込む。 */
    public static void applyDefinitions(JsonObject root) {
        Map<ResourceLocation, SmithingMaterial> materials = new HashMap<>();
        Map<ResourceLocation, SmithingPart> parts = new HashMap<>();
        if (root.has("materials")) {
            for (JsonElement element : root.getAsJsonArray("materials")) {
                try {
                    SmithingMaterial material = parseMaterial(null, element.getAsJsonObject());
                    if (material.id() != null) {
                        materials.put(material.id(), material);
                    }
                } catch (Exception exception) {
                    LOGGER.error("Failed to read synced smithing material", exception);
                }
            }
        }
        if (root.has("parts")) {
            for (JsonElement element : root.getAsJsonArray("parts")) {
                try {
                    SmithingPart part = parsePart(null, element.getAsJsonObject());
                    if (part.id() != null) {
                        parts.put(part.id(), part);
                    }
                } catch (Exception exception) {
                    LOGGER.error("Failed to read synced smithing part", exception);
                }
            }
        }
        SmithingDefinitions.setMaterials(materials);
        SmithingDefinitions.setParts(parts);
        LOGGER.info("Loaded synced smithing definitions ({} materials / {} parts)",
                materials.size(), parts.size());
    }

    private static ResourceLocation readId(JsonObject json, String key, ResourceLocation fallback) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return fallback;
        }
        String raw = json.get(key).getAsString();
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return ResourceLocation.tryParse(raw.trim());
    }

    private static SkillData.WeaponType parseWeapon(String name) {
        if (name == null || name.isBlank()) {
            return SkillData.WeaponType.NONE;
        }
        try {
            return SkillData.WeaponType.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SkillData.WeaponType.NONE;
        }
    }

    private static List<ResourceLocation> readIds(JsonObject json, String key) {
        List<ResourceLocation> ids = new ArrayList<>();
        if (json.has(key)) {
            JsonArray array = json.getAsJsonArray(key);
            for (JsonElement element : array) {
                String value = element.getAsString();
                if (value.startsWith("#")) {
                    value = value.substring(1);
                }
                ids.add(ResourceLocation.parse(value));
            }
        }
        return List.copyOf(ids);
    }

    /** {@code #RRGGBB} / {@code RRGGBB} / 10進整数 を受け付ける。 */
    public static int parseColor(String value) {
        if (value == null || value.isBlank()) {
            return 0xFFFFFF;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        try {
            if (trimmed.length() == 8) {
                // AARRGGBB のアルファは無視する
                return (int) Long.parseLong(trimmed.substring(2), 16);
            }
            return (int) Long.parseLong(trimmed, 16);
        } catch (NumberFormatException exception) {
            return 0xFFFFFF;
        }
    }
}
