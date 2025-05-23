package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WeaponTypeDetector {

    private static WeaponTypeDetector instance;
    private final WeaponTypeDataLoader weaponTypeDataLoader;
    private static volatile boolean dataLoaded = false;

    private WeaponTypeDetector(WeaponTypeDataLoader weaponTypeDataLoader) {
        this.weaponTypeDataLoader = weaponTypeDataLoader;
    }

    public static void initialize(WeaponTypeDataLoader weaponTypeDataLoader) {
        instance = new WeaponTypeDetector(weaponTypeDataLoader);
        dataLoaded = false;
    }

    public static void markDataLoaded() {
        dataLoaded = true;
    }

    public static boolean isReady() {
        return instance == null || !dataLoaded;
    }

    public static Set<SkillData.WeaponType> detectWeaponTypes(ItemStack heldItem) {
        if (isReady()) {
            return Collections.emptySet();
        }
        return instance.detectWeaponTypesInternal(heldItem);
    }

    private Set<SkillData.WeaponType> detectWeaponTypesInternal(ItemStack heldItem) {
        Set<SkillData.WeaponType> weaponTypes = new HashSet<>();
        Item item = heldItem.getItem();
        // JSONファイルから武器種を判定
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (data.items().contains(item)) {
                weaponTypes.addAll(data.weaponTypes());
                return weaponTypes;
            }
        }
        return weaponTypes;
    }

    // WeaponTypeDetector.java
    public static String getWeaponName(ItemStack heldItem) {
        if (instance == null) {
            throw new IllegalStateException("WeaponTypeDetector has not been initialized.");
        }
        return instance.getWeaponNameInternal(heldItem);
    }

    private String getWeaponNameInternal(ItemStack heldItem) {
        Item item = heldItem.getItem();
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (data.items().contains(item)) {
                return data.name();
            }
        }
        return null;
    }
}