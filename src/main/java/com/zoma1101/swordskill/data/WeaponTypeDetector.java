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

    /**
     * 武器種に対応する武器名（weapon_types の name）を返す。
     *
     * <p>鍛造武器はアイテムが 1 種類なので、武器種から既存の武器名を引いて
     * スキルスロットを共有するために使う。</p>
     */
    public static String findWeaponName(SkillData.WeaponType weaponType) {
        if (isReady() || weaponType == null || weaponType == SkillData.WeaponType.NONE) {
            return null;
        }
        return instance.findWeaponNameInternal(weaponType);
    }

    private String findWeaponNameInternal(SkillData.WeaponType weaponType) {
        String best = null;
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (!data.weaponTypes().contains(weaponType)) {
                continue;
            }
            String name = data.name();
            if (name == null || name.isBlank()) {
                continue;
            }
            // HashMap の順序に依存しないよう、常に同じ名前を選ぶ
            if (best == null || preferName(name, best)) {
                best = name;
            }
        }
        return best;
    }

    /** MOD 名付きでない素の名前を優先し、次に短いもの、最後は辞書順で決める。 */
    private static boolean preferName(String candidate, String current) {
        boolean candidatePlain = !candidate.contains(":");
        boolean currentPlain = !current.contains(":");
        if (candidatePlain != currentPlain) {
            return candidatePlain;
        }
        if (candidate.length() != current.length()) {
            return candidate.length() < current.length();
        }
        return candidate.compareTo(current) < 0;
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