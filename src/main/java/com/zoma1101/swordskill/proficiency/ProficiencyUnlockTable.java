package com.zoma1101.swordskill.proficiency;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 武器カテゴリごとの「スキル修得に必要な熟練度」をデータパックから上書きする。
 *
 * <p>置き場所は {@code data/<namespace>/proficiency/<category>.json}。
 * ファイル名がカテゴリ（{@code one_handed_sword} など）になる。</p>
 *
 * <pre>{@code
 * {
 *   "skills": {
 *     "slant": 0,
 *     "horizontal": 100,
 *     "horizontal_arc": 200
 *   }
 * }
 * }</pre>
 *
 * <p>ここに書かなかったスキルは、skilldata の {@code unlock_proficiency} がそのまま使われる。
 * {@code /reload} で反映され、サーバーからクライアントへも同期される。</p>
 */
public final class ProficiencyUnlockTable {

    /** データパック内のディレクトリ名。 */
    public static final String DIRECTORY = "proficiency";
    /** skilldata 側と同じ上限。 */
    private static final int MAX_VALUE = 1_000_000;
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();

    /** カテゴリ → スキル名 → 必要熟練度。 */
    private static final Map<SkillData.WeaponType, Map<String, Integer>> OVERRIDES =
            new EnumMap<>(SkillData.WeaponType.class);

    private ProficiencyUnlockTable() {
    }

    /** データパックを読み直す（{@code /reload} ごとに呼ばれる）。 */
    public static void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> resources = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, DIRECTORY, GSON, resources);

        Map<SkillData.WeaponType, Map<String, Integer>> parsed = new EnumMap<>(SkillData.WeaponType.class);
        resources.forEach((id, json) -> {
            SkillData.WeaponType category = categoryOf(id);
            if (category == null) {
                LOGGER.warn("Unknown proficiency category in data pack: {}", id);
                return;
            }
            Map<String, Integer> values = parse(id, json);
            if (!values.isEmpty()) {
                parsed.put(category, values);
            }
        });

        OVERRIDES.clear();
        OVERRIDES.putAll(parsed);
    }

    /** 上書きが1件も無ければ {@code definitions} をそのまま返す。 */
    public static List<DataSkillDefinition> apply(List<DataSkillDefinition> definitions) {
        if (OVERRIDES.isEmpty()) {
            return definitions;
        }
        List<DataSkillDefinition> result = new ArrayList<>(definitions.size());
        Set<String> used = new HashSet<>();
        for (DataSkillDefinition definition : definitions) {
            SkillData.WeaponType category = categoryOf(definition);
            Map<String, Integer> values = category == SkillData.WeaponType.NONE ? null : OVERRIDES.get(category);
            Integer override = values == null ? null : values.get(definition.name());
            if (override == null) {
                result.add(definition);
                continue;
            }
            used.add(category.name() + "/" + definition.name());
            result.add(override == definition.unlockProficiency()
                    ? definition
                    : definition.withUnlockProficiency(override));
        }

        // タイプミスに気づけるように、どのスキルにも当たらなかった項目を警告する
        OVERRIDES.forEach((category, values) -> values.keySet().forEach(name -> {
            if (!used.contains(category.name() + "/" + name)) {
                LOGGER.warn("proficiency/{}.json: no skill named '{}' in category {}",
                        category.name().toLowerCase(Locale.ROOT), name, category);
            }
        }));
        return result;
    }

    /** そのスキルの実効的な必要熟練度（上書きが無ければ JSON の値）。 */
    public static int requirementOf(SkillData skill) {
        return skill == null ? 0 : skill.getUnlockProficiency();
    }

    public static SkillData.WeaponType categoryOf(DataSkillDefinition definition) {
        if (definition.proficiencyCategory() != null) {
            return definition.proficiencyCategory();
        }
        return definition.weapons().isEmpty() ? SkillData.WeaponType.NONE : definition.weapons().get(0);
    }

    private static SkillData.WeaponType categoryOf(ResourceLocation id) {
        String path = id.getPath();
        if (path.isEmpty() || path.indexOf('/') >= 0) {
            return null;
        }
        try {
            SkillData.WeaponType type = SkillData.WeaponType.valueOf(path.toUpperCase(Locale.ROOT));
            return type == SkillData.WeaponType.NONE ? null : type;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Map<String, Integer> parse(ResourceLocation id, JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            LOGGER.warn("proficiency file {} must contain a JSON object", id);
            return Map.of();
        }
        JsonObject root = json.getAsJsonObject();
        JsonObject skills = root.has("skills") && root.get("skills").isJsonObject()
                ? root.getAsJsonObject("skills")
                : root;

        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : skills.entrySet()) {
            String name = entry.getKey();
            if ("skills".equals(name) || !name.matches("[a-z0-9_]+")) {
                continue;
            }
            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                continue;
            }
            int requirement = value.getAsInt();
            if (requirement < 0 || requirement > MAX_VALUE) {
                LOGGER.warn("proficiency file {}: '{}' is outside 0..{}", id, name, MAX_VALUE);
                continue;
            }
            values.put(name, requirement);
        }
        return values;
    }
}
