package com.zoma1101.SwordSkill.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.minecraft.resources.ResourceLocation.parse;

public class WeaponTypeDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private final Map<String, WeaponTypeData> weaponTypeDataMap = new HashMap<>();

    public WeaponTypeDataLoader() {
        super(GSON, "weapon_types");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        weaponTypeDataMap.clear();

        objectIn.forEach((resourceLocation, jsonElement) -> {
            try {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                LOGGER.info("JsonObjectは" + jsonObject);
                WeaponTypeData weaponTypeData = parseWeaponTypeData(jsonObject);
                weaponTypeDataMap.put(weaponTypeData.getName(), weaponTypeData);

                // ここで WeaponTypeData の内容をログ出力
                System.out.println("Loaded WeaponTypeData:");
                System.out.println("Name:" + weaponTypeData.getName());
                System.out.println("WeaponTypes:" + weaponTypeData.getWeaponTypes());
                LOGGER.info("  Items: {}",
                        weaponTypeData.getItems().stream()
                                .map(item -> ForgeRegistries.ITEMS.getKey(item).toString())
                                .collect(Collectors.toList()));

            } catch (Exception e) {
                LOGGER.error("Failed to load weapon type data from {}", resourceLocation, e);
            }
        });
        LOGGER.info("Loaded {} weapon type data files", weaponTypeDataMap.size());
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

    public static class WeaponTypeData {
        private final String name;
        private final List<SkillData.WeaponType> weaponTypes;
        private final List<Item> items;

        public WeaponTypeData(String name, List<SkillData.WeaponType> weaponTypes, List<Item> items) {
            this.name = name;
            this.weaponTypes = weaponTypes;
            this.items = items;
        }

        public String getName() {
            return name;
        }

        public List<SkillData.WeaponType> getWeaponTypes() {
            return weaponTypes;
        }

        public List<Item> getItems() {
            return items;
        }
    }
}
