package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WeaponTypeDetector {

    private static WeaponTypeDetector instance;
    private final WeaponTypeDataLoader weaponTypeDataLoader;
    private static volatile boolean dataLoaded = false;

    // ★独自タグ定義
    private static final TagKey<Item> TAG_GREATSWORDS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "greatswords"));
    private static final TagKey<Item> TAG_SPEARS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "spears"));
    private static final TagKey<Item> TAG_MACES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "maces"));
    private static final TagKey<Item> TAG_RAPIERS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "rapiers"));
    private static final TagKey<Item> TAG_DAGGERS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "daggers"));
    private static final TagKey<Item> TAG_KATANAS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "katanas"));
    private static final TagKey<Item> TAG_CLAWS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "claws"));
    private static final TagKey<Item> TAG_SCYTHES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "scythes"));
    private static final TagKey<Item> TAG_BATTLEAXES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("swordskill_extender", "battleaxes"));


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

        // 1. JSONファイルから武器種を判定 (最優先)
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (data.items().contains(item)) {
                weaponTypes.addAll(data.weaponTypes());
                return weaponTypes;
            }
        }

        // 2. 独自タグによる判定 (swordskill_extender)
        // ここで判定できた場合、バニラタグの判定は行われません
        if (heldItem.is(TAG_GREATSWORDS)) weaponTypes.add(SkillData.WeaponType.TWO_HANDED_SWORD);
        if (heldItem.is(TAG_SPEARS)) weaponTypes.add(SkillData.WeaponType.SPEAR);
        if (heldItem.is(TAG_MACES)) weaponTypes.add(SkillData.WeaponType.MACE);
        if (heldItem.is(TAG_RAPIERS)) weaponTypes.add(SkillData.WeaponType.RAPIER);
        if (heldItem.is(TAG_DAGGERS)) weaponTypes.add(SkillData.WeaponType.DAGGER);
        if (heldItem.is(TAG_KATANAS)) weaponTypes.add(SkillData.WeaponType.KATANA);
        if (heldItem.is(TAG_CLAWS)) weaponTypes.add(SkillData.WeaponType.CLAW);
        if (heldItem.is(TAG_SCYTHES)) weaponTypes.add(SkillData.WeaponType.SCYTHE);
        if (heldItem.is(TAG_BATTLEAXES)) weaponTypes.add(SkillData.WeaponType.AXE);

        // ★重要: 独自タグで1つでも判定できた場合はここでリターンし、バニラタグ判定をスキップします
        if (!weaponTypes.isEmpty()) {
            return weaponTypes;
        }

        // 3. バニラタグによる判定 (フォールバック / 最低優先度)
        // 独自タグを持たないアイテムのみ、ここで判定されます
        if (heldItem.is(ItemTags.SWORDS)) {
            weaponTypes.add(SkillData.WeaponType.ONE_HANDED_SWORD);
        }
        if (heldItem.is(ItemTags.AXES)) {
            weaponTypes.add(SkillData.WeaponType.AXE);
        }

        return weaponTypes;
    }

    public static String getWeaponName(ItemStack heldItem) {
        if (instance == null) {
            throw new IllegalStateException("WeaponTypeDetector has not been initialized.");
        }
        return instance.getWeaponNameInternal(heldItem);
    }

    private String getWeaponNameInternal(ItemStack heldItem) {
        Item item = heldItem.getItem();

        // 1. JSONデータ
        for (WeaponTypeDataLoader.WeaponTypeData data : weaponTypeDataLoader.getWeaponTypeDataMap().values()) {
            if (data.items().contains(item)) {
                return data.name();
            }
        }

        // 2. 独自タグ (優先)
        if (heldItem.is(TAG_GREATSWORDS)) return "Greatsword";
        if (heldItem.is(TAG_SPEARS)) return "Spear";
        if (heldItem.is(TAG_MACES)) return "Mace";
        if (heldItem.is(TAG_RAPIERS)) return "Rapier";
        if (heldItem.is(TAG_DAGGERS)) return "Dagger";
        if (heldItem.is(TAG_KATANAS)) return "Katana";
        if (heldItem.is(TAG_CLAWS)) return "Claw";
        if (heldItem.is(TAG_SCYTHES)) return "Scythe";
        if (heldItem.is(TAG_BATTLEAXES)) return "Battleaxe";

        // 3. バニラタグ (フォールバック)
        if (heldItem.is(ItemTags.SWORDS)) {
            return "Sword";
        }
        if (heldItem.is(ItemTags.AXES)) {
            return "Axe";
        }

        return null;
    }
}