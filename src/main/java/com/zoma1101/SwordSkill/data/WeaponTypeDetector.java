package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.config.ServerConfig;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class WeaponTypeDetector {

    private static WeaponTypeDetector instance;
    private final WeaponTypeDataLoader weaponTypeDataLoader;

    private WeaponTypeDetector(WeaponTypeDataLoader weaponTypeDataLoader) {
        this.weaponTypeDataLoader = weaponTypeDataLoader;
    }

    public static void initialize(WeaponTypeDataLoader weaponTypeDataLoader) {
        if (instance == null) {
            instance = new WeaponTypeDetector(weaponTypeDataLoader);
        }
    }

    public static WeaponTypeDetector getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WeaponTypeDetector has not been initialized.");
        }
        return instance;
    }

    public static Set<SkillData.WeaponType> detectWeaponTypes(ItemStack heldItem) {
        if (instance == null) {
            throw new IllegalStateException("WeaponTypeDetector has not been initialized.");
        }
        return instance.detectWeaponTypesInternal(heldItem);
    }

    private Set<SkillData.WeaponType> detectWeaponTypesInternal(ItemStack heldItem) {
        Set<SkillData.WeaponType> weaponTypes = new HashSet<>();
        Item item = heldItem.getItem();
        // JSONファイルから武器種を判定
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (data.getItems().contains(item)) {
                weaponTypes.addAll(data.getWeaponTypes());
                return weaponTypes; // JSONに定義があれば、それを返す
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
            if (data.getItems().contains(item)) {
                return data.getName();
            }
        }
        return null;
    }
}