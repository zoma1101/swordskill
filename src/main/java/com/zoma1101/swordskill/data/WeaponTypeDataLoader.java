package com.zoma1101.swordskill.data;

import com.google.gson.*;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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
        Map<String, List<PendingWeaponTypeData>> pendingWeaponTypeDataMap = new HashMap<>();
        Map<String, WeaponTypeData> newWeaponTypeDataMap = new HashMap<>();
        boolean updated = false;

        // 一時的にデータを保持する
        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            if (resourceLocation.getPath().startsWith("_")) continue;

            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(entry.getValue(), "top element");
                currentJsonMap.put(resourceLocation, jsonObject);
                PendingWeaponTypeData pendingData = parsePendingWeaponTypeData(jsonObject);
                String key = generateKey(pendingData.name(), pendingData.weaponTypes());

                if (!pendingWeaponTypeDataMap.containsKey(key)) {
                    pendingWeaponTypeDataMap.put(key, new ArrayList<>());
                }
                pendingWeaponTypeDataMap.get(key).add(pendingData);

                if (!previousJsonMap.containsKey(resourceLocation) || !previousJsonMap.get(resourceLocation).equals(jsonObject)) {
                    updated = true;
                }

            } catch (JsonParseException e) {
                LOGGER.error("Parsing error loading weapon type data {}", resourceLocation, e);
            }
        }

        // 同じキーを持つデータを結合して WeaponTypeData を作成
        for (List<PendingWeaponTypeData> pendingList : pendingWeaponTypeDataMap.values()) {
            if (pendingList.isEmpty()) continue;

            PendingWeaponTypeData first = pendingList.getFirst();
            List<Item> combinedItems = new ArrayList<>();
            for (PendingWeaponTypeData pending : pendingList) {
                combinedItems.addAll(pending.items());
            }
            // アイテムの重複を削除
            List<Item> uniqueItems = combinedItems.stream().distinct().collect(Collectors.toList());

            WeaponTypeData weaponTypeData = new WeaponTypeData(first.name(), first.weaponTypes(), uniqueItems);
            newWeaponTypeDataMap.put(weaponTypeData.name(), weaponTypeData);
        }

        if (updated || weaponTypeDataMap.size() != currentJsonMap.size()) {
            weaponTypeDataMap.clear();
            weaponTypeDataMap.putAll(newWeaponTypeDataMap);
            previousJsonMap.clear();
            previousJsonMap.putAll(currentJsonMap);
            LOGGER.info("Weapon type data reloaded. Total files: {}", weaponTypeDataMap.size());

            WeaponTypeDetector.markDataLoaded();
        } else {
            LOGGER.info("Weapon type data not changed.");
        }
    }

    private PendingWeaponTypeData parsePendingWeaponTypeData(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();

        JsonArray weaponTypesJson = jsonObject.getAsJsonArray("weapontype");
        List<SkillData.WeaponType> weaponTypes = new ArrayList<>();
        weaponTypesJson.forEach(jsonElement -> weaponTypes.add(SkillData.WeaponType.valueOf(jsonElement.getAsString())));

        JsonArray itemIdsJson = jsonObject.getAsJsonArray("item");
        List<Item> items = new ArrayList<>();
        itemIdsJson.forEach(jsonElement -> {
            ResourceLocation itemId = parse(jsonElement.getAsString());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            items.add(item);
        });

        return new PendingWeaponTypeData(name, weaponTypes, items);
    }

    private String generateKey(String name, List<SkillData.WeaponType> weaponTypes) {
        return name + ":" + weaponTypes.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public Map<String, WeaponTypeData> getWeaponTypeDataMap() {
        return weaponTypeDataMap;
    }

    private record PendingWeaponTypeData(String name, List<SkillData.WeaponType> weaponTypes, List<Item> items) {
    }

    public record WeaponTypeData(String name, List<SkillData.WeaponType> weaponTypes, List<Item> items) {
    }
}