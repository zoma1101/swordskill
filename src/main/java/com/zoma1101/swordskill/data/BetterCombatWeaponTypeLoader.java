package com.zoma1101.swordskill.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Better Combat の {@code weapon_attributes} データパックを読み取り、
 * アイテムごとの {@code attributes.category} を Sword Skill の武器種へ変換する。
 *
 * <p>Better Combat は {@code data/<item_namespace>/weapon_attributes/<item_path>.json}
 * をアイテム ID（{@code <item_namespace>:<item_path>}）の設定として扱い、
 * {@code parent} を辿って属性を継承する。ここでも同じ規則で解決し、
 * {@code data/swordskill/weapon_types} に明示的な設定が無いアイテムだけを
 * フォールバックとして判定する。Better Combat が入っていない場合は
 * ファイルが存在しないため、この統合は何も変更しない。</p>
 */
public final class BetterCombatWeaponTypeLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private static final Map<ResourceLocation, Resolved> BY_ITEM = new HashMap<>();
    private static volatile boolean loaded = false;

    public BetterCombatWeaponTypeLoader() {
        super(GSON, "weapon_attributes");
    }

    /** Better Combat の設定から武器種を判定する（未対応カテゴリなら null）。 */
    public static SkillData.WeaponType detect(ItemStack stack) {
        if (!loaded || !ServerConfig.useBetterCombatWeaponTypes.get() || stack.isEmpty()) {
            return null;
        }
        Resolved resolved = BY_ITEM.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return resolved == null ? null : resolved.weaponType();
    }

    /** 判定結果に対応する武器名（スキルスロット共有用のキー）。 */
    public static String weaponName(ItemStack stack, SkillData.WeaponType weaponType) {
        if (weaponType == null) {
            return null;
        }
        // 明示設定の武器名があればそれに合わせ、無ければ武器種名を使う。
        String shared = WeaponTypeDetector.findWeaponName(weaponType);
        return shared != null ? shared : weaponType.name().toLowerCase(Locale.ROOT);
    }

    /** 読み込み済みの Better Combat 対応アイテム数（ログ・検証用）。 */
    public static int resolvedItemCount() {
        return BY_ITEM.size();
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> files,
            @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, JsonObject> containers = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            if (entry.getKey().getPath().startsWith("_")) {
                continue;
            }
            try {
                containers.put(entry.getKey(),
                        GsonHelper.convertToJsonObject(entry.getValue(), "weapon attributes"));
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to parse Better Combat weapon attributes {}", entry.getKey(), exception);
            }
        }

        Map<ResourceLocation, Resolved> resolvedItems = new HashMap<>();
        for (ResourceLocation id : containers.keySet()) {
            // Better Combat は実在するアイテム ID と一致するファイルだけを登録する
            if (!BuiltInRegistries.ITEM.containsKey(id)) {
                continue;
            }
            Resolved resolved = resolve(id, containers, new HashSet<>());
            if (resolved != null && resolved.weaponType() != null) {
                resolvedItems.put(id, resolved);
            }
        }

        BY_ITEM.clear();
        BY_ITEM.putAll(resolvedItems);
        loaded = true;
        if (!resolvedItems.isEmpty()) {
            LOGGER.info("Better Combat weapon attributes integrated for {} item(s).", resolvedItems.size());
        }
    }

    /**
     * {@code parent} を辿ってカテゴリと両手フラグを解決する。
     * 子の値が親より優先される（Better Combat の override と同じ規則）。
     */
    private static Resolved resolve(ResourceLocation id, Map<ResourceLocation, JsonObject> containers,
            Set<ResourceLocation> visiting) {
        if (!visiting.add(id)) {
            return null;
        }
        JsonObject json = containers.get(id);
        if (json == null) {
            return null;
        }

        String category = null;
        Boolean twoHanded = null;
        if (json.has("attributes") && json.get("attributes").isJsonObject()) {
            JsonObject attributes = json.getAsJsonObject("attributes");
            if (attributes.has("category") && attributes.get("category").isJsonPrimitive()) {
                String value = attributes.get("category").getAsString();
                if (!value.isBlank()) {
                    category = value.trim();
                }
            }
            if (attributes.has("two_handed") && attributes.get("two_handed").isJsonPrimitive()) {
                twoHanded = attributes.get("two_handed").getAsBoolean();
            }
        }

        if (json.has("parent") && json.get("parent").isJsonPrimitive()) {
            String parentValue = json.get("parent").getAsString();
            if (!parentValue.isBlank()) {
                Resolved parent = resolve(parentId(parentValue, id, containers), containers, visiting);
                if (parent != null) {
                    if (category == null) {
                        category = parent.category();
                    }
                    if (twoHanded == null) {
                        twoHanded = parent.twoHanded();
                    }
                }
            }
        }

        boolean isTwoHanded = Boolean.TRUE.equals(twoHanded);
        return new Resolved(category, isTwoHanded, mapCategory(category, isTwoHanded));
    }

    /**
     * 親 ID を解決する。名前空間が無い場合は Better Combat と同じく
     * {@code minecraft} を第一候補にし、存在しなければ {@code bettercombat}、
     * 最後に同じ名前空間を試す（データパックの書き方揺れへの保険）。
     */
    private static ResourceLocation parentId(String value, ResourceLocation child,
            Map<ResourceLocation, JsonObject> containers) {
        if (value.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse(value);
            if (parsed != null) {
                return parsed;
            }
        }
        ResourceLocation minecraft = ResourceLocation.tryParse("minecraft:" + value);
        if (minecraft != null && containers.containsKey(minecraft)) {
            return minecraft;
        }
        ResourceLocation betterCombat = ResourceLocation.tryParse("bettercombat:" + value);
        if (betterCombat != null && containers.containsKey(betterCombat)) {
            return betterCombat;
        }
        ResourceLocation sameNamespace = ResourceLocation.tryParse(child.getNamespace() + ":" + value);
        if (sameNamespace != null && containers.containsKey(sameNamespace)) {
            return sameNamespace;
        }
        return minecraft != null ? minecraft : child;
    }

    /**
     * Better Combat のカテゴリ名を Sword Skill の武器種へ対応付ける。
     * 未対応カテゴリは null（＝武器種を自動設定しない）。
     */
    private static SkillData.WeaponType mapCategory(String category, boolean twoHanded) {
        if (category == null) {
            return null;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "sword", "cutlass", "coral_blade" ->
                    twoHanded ? SkillData.WeaponType.TWO_HANDED_SWORD : SkillData.WeaponType.ONE_HANDED_SWORD;
            case "claymore", "greatsword", "great_sword", "longsword", "long_sword",
                    "broadsword", "zweihander" -> SkillData.WeaponType.TWO_HANDED_SWORD;
            case "katana", "odachi", "nodachi" -> SkillData.WeaponType.KATANA;
            case "rapier", "estoc" -> SkillData.WeaponType.RAPIER;
            case "twin_blade", "twinblade", "dual_sword", "dualsword", "dual_blade" ->
                    SkillData.WeaponType.DUALSWORD;
            case "axe", "double_axe", "heavy_axe", "battle_axe", "greataxe", "great_axe" ->
                    SkillData.WeaponType.AXE;
            case "claw", "sickle", "talon" -> SkillData.WeaponType.CLAW;
            case "spear", "lance", "trident", "glaive", "halberd", "pike", "partisan", "naginata" ->
                    SkillData.WeaponType.SPEAR;
            case "mace", "hammer", "vanilla_mace", "club", "warhammer", "maul" ->
                    SkillData.WeaponType.MACE;
            case "scythe", "reaper" -> SkillData.WeaponType.SCYTHE;
            case "dagger", "soul_knife", "knife", "shortsword", "short_sword" ->
                    SkillData.WeaponType.DAGGER;
            default -> null;
        };
    }

    private record Resolved(String category, boolean twoHanded, SkillData.WeaponType weaponType) {
    }
}
