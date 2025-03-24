package com.zoma1101.SwordSkill.data;

import com.google.gson.*;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.resources.ResourceLocation.parse;

public class WeaponTypeDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private final Map<String, WeaponTypeData> weaponTypeDataMap = new HashMap<>();
    private final Map<ResourceLocation, JsonObject> previousJsonMap = new HashMap<>(); // 以前読み込んだ JSON を保存

    public WeaponTypeDataLoader() {
        super(GSON, "weapon_types");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn) {
        Map<ResourceLocation, JsonObject> currentJsonMap = new HashMap<>();
        Map<String, WeaponTypeData> newWeaponTypeDataMap = new HashMap<>();
        boolean updated = false;

        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            if (resourceLocation.getPath().startsWith("_")) continue;

            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), "top element");
                currentJsonMap.put(resourceLocation, jsonObject);

                if (!previousJsonMap.containsKey(resourceLocation) || !previousJsonMap.get(resourceLocation).equals(jsonObject)) {
                    // 新規ファイルまたは内容が変更されたファイル
                    WeaponTypeData weaponTypeData = parseWeaponTypeData(jsonObject);
                    newWeaponTypeDataMap.put(weaponTypeData.name(), weaponTypeData);
                    updated = true;
                } else {
                    // 内容が変わっていないファイル
                    newWeaponTypeDataMap.put(weaponTypeDataMap.get(resourceLocation.toString()).name(), weaponTypeDataMap.get(resourceLocation.toString()));
                }

            } catch (JsonParseException e) {
                LOGGER.error("Parsing error loading weapon type data {}", resourceLocation, e);
            }
        }

        if (updated || weaponTypeDataMap.size() != currentJsonMap.size()) {
            weaponTypeDataMap.clear();
            weaponTypeDataMap.putAll(newWeaponTypeDataMap);
            previousJsonMap.clear();
            previousJsonMap.putAll(currentJsonMap);
            LOGGER.info("Weapon type data reloaded. Total files: {}", weaponTypeDataMap.size());
            //WeaponTypeDetector.initialize(this);
            // 必要に応じて Mod のロジックに更新を通知する処理を追加
        } else {
            LOGGER.info("Weapon type data not changed.");
        }
    }

    private WeaponTypeData parseWeaponTypeData(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();

        JsonArray weaponTypesJson = jsonObject.getAsJsonArray("weapontype");
        List<SkillData.WeaponType> weaponTypes = new ArrayList<>();
        weaponTypesJson.forEach(jsonElement -> weaponTypes.add(SkillData.WeaponType.valueOf(jsonElement.getAsString())));

        JsonArray itemIdsJson = jsonObject.getAsJsonArray("item");
        List<Item> items = new ArrayList<>();
        itemIdsJson.forEach(jsonElement -> {
            ResourceLocation itemId = parse(jsonElement.getAsString());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                items.add(item);
            } else {
                LOGGER.info("Item not found: {}", itemId);
            }
        });

        return new WeaponTypeData(name, weaponTypes, items);
    }

    public Map<String, WeaponTypeData> getWeaponTypeDataMap() {
        return weaponTypeDataMap;
    }

    public record WeaponTypeData(String name, List<SkillData.WeaponType> weaponTypes, List<Item> items) {
    }
}
