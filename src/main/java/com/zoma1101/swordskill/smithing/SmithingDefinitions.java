package com.zoma1101.swordskill.smithing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 読み込み済みの鍛冶定義（素材・部品・レシピ）を保持する。
 *
 * <p>データパックのリロードで丸ごと差し替わる。マルチプレイではサーバーの定義を
 * JSON にしてクライアントへ配る（{@link #toJson()} / {@link #applyJson(JsonObject)}）ので、
 * クライアントでも同じレシピ一覧・部品名を扱える。</p>
 */
public final class SmithingDefinitions {

    private static volatile Map<ResourceLocation, SmithingMaterial> materials = Map.of();
    private static volatile Map<ResourceLocation, SmithingPart> parts = Map.of();
    private static volatile Map<ResourceLocation, SmithingRecipe> recipes = Map.of();
    private static volatile boolean syncPending;

    private SmithingDefinitions() {
    }

    // ------------------------------------------------------------------
    // 保持
    // ------------------------------------------------------------------

    public static void setMaterials(Map<ResourceLocation, SmithingMaterial> loaded) {
        materials = Map.copyOf(loaded);
    }

    public static void setParts(Map<ResourceLocation, SmithingPart> loaded) {
        parts = Map.copyOf(loaded);
    }

    public static void setRecipes(Map<ResourceLocation, SmithingRecipe> loaded) {
        recipes = Map.copyOf(loaded);
    }

    public static Collection<SmithingMaterial> materials() {
        return materials.values();
    }

    public static Collection<SmithingPart> parts() {
        return parts.values();
    }

    public static Collection<SmithingRecipe> recipes() {
        return recipes.values();
    }

    public static SmithingMaterial material(ResourceLocation id) {
        return id == null ? null : materials.get(id);
    }

    public static SmithingPart part(ResourceLocation id) {
        return id == null ? null : parts.get(id);
    }

    public static SmithingRecipe recipe(ResourceLocation id) {
        return id == null ? null : recipes.get(id);
    }

    // ------------------------------------------------------------------
    // 同期
    // ------------------------------------------------------------------

    /** 定義が変わったのでクライアントへ配り直す必要がある。 */
    public static void markSyncPending() {
        syncPending = true;
    }

    /** 配布が必要なら true を返し、フラグを下ろす。 */
    public static boolean consumeSyncPending() {
        boolean value = syncPending;
        syncPending = false;
        return value;
    }

    // ------------------------------------------------------------------
    // 検索
    // ------------------------------------------------------------------

    /** アイテムに対応する素材を返す（アイテム ID 一致を優先し、次にタグ一致）。 */
    public static SmithingMaterial materialFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // 定義の順番に依存しないよう ID 順に見る（サーバーとクライアントで同じ結果にする）
        for (SmithingMaterial material : sortedMaterials()) {
            if (material.items().contains(itemId)) {
                return material;
            }
        }
        for (SmithingMaterial material : sortedMaterials()) {
            for (ResourceLocation tagId : material.tags()) {
                if (stack.is(TagKey.create(Registries.ITEM, tagId))) {
                    return material;
                }
            }
        }
        return null;
    }

    /** 素材を ID 順に並べた一覧。 */
    private static List<SmithingMaterial> sortedMaterials() {
        List<SmithingMaterial> list = new ArrayList<>(materials.values());
        list.sort(Comparator.comparing(material -> material.id().toString()));
        return list;
    }

    /**
     * メイン素材を入れたときに選べるレシピを、ID 順で返す。
     *
     * <p>この並び順がサーバーとクライアントで一致することが、レシピ一覧の
     * 選択番号を共有する前提になっている。</p>
     */
    public static List<SmithingRecipe> recipesFor(SmithingMaterial material) {
        if (material == null) {
            return List.of();
        }
        List<SmithingRecipe> result = new ArrayList<>();
        for (SmithingRecipe recipe : recipes.values()) {
            if (recipe.availableFor(material)) {
                result.add(recipe);
            }
        }
        result.sort(Comparator.comparing(recipe -> recipe.id().toString()));
        return result;
    }

    /** 指定の種類・武器種で使える部品を、ID 順で返す。 */
    public static List<SmithingPart> partsOfKind(SmithingPartKind kind, SkillData.WeaponType weaponType) {
        List<SmithingPart> result = new ArrayList<>();
        for (SmithingPart part : parts.values()) {
            if (part.kind() == kind && part.supports(weaponType)) {
                result.add(part);
            }
        }
        result.sort(Comparator.comparing(part -> part.id().toString()));
        return result;
    }

    /** 指定の素材のアイテムかどうか。 */
    public static boolean isMaterialItem(SmithingMaterial material, ItemStack stack) {
        if (stack.isEmpty() || material == null) {
            return false;
        }
        if (material.items().contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return true;
        }
        for (ResourceLocation tagId : material.tags()) {
            if (stack.is(TagKey.create(Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }

    /** 表示用のアイテム名（ID のパスを整形しただけのフォールバック付き）。 */
    public static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    // ------------------------------------------------------------------
    // JSON 化（クライアント同期）
    // ------------------------------------------------------------------

    /** 素材・部品・レシピを丸ごと JSON にする。{@link #applyJson(JsonObject)} で戻せる。 */
    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray materialArray = new JsonArray();
        for (SmithingMaterial material : sorted(materials, m -> m.id())) {
            JsonObject json = new JsonObject();
            json.addProperty("id", material.id().toString());
            json.addProperty("tier", material.tier());
            json.addProperty("power", material.power());
            json.addProperty("weight", material.weight());
            json.addProperty("color_main", color(material.colorMain()));
            json.addProperty("color_sub", color(material.colorSub()));
            json.add("items", idArray(material.items()));
            json.add("tags", idArray(material.tags()));
            json.addProperty("special", material.special());
            materialArray.add(json);
        }
        root.add("materials", materialArray);

        JsonArray partArray = new JsonArray();
        for (SmithingPart part : sorted(parts, p -> p.id())) {
            JsonObject json = new JsonObject();
            json.addProperty("id", part.id().toString());
            json.addProperty("kind", part.kind().name());
            JsonArray weapons = new JsonArray();
            for (SkillData.WeaponType weapon : part.weapons()) {
                weapons.add(weapon.name());
            }
            json.add("weapons", weapons);
            json.addProperty("texture", part.texture());
            json.addProperty("power", part.power());
            json.addProperty("weight", part.weight());
            json.addProperty("attack_speed", part.attackSpeed());
            json.addProperty("color_slot", part.colorSlot().name());
            json.addProperty("tier", part.tier());
            partArray.add(json);
        }
        root.add("parts", partArray);

        JsonArray recipeArray = new JsonArray();
        for (SmithingRecipe recipe : sorted(recipes, r -> r.id())) {
            JsonObject json = new JsonObject();
            json.addProperty("id", recipe.id().toString());
            json.addProperty("blade", recipe.blade() == null ? "" : recipe.blade().toString());
            json.addProperty("guard", recipe.guard() == null ? "" : recipe.guard().toString());
            json.addProperty("handle", recipe.handle() == null ? "" : recipe.handle().toString());
            json.addProperty("weapon", recipe.weaponType().name());
            json.addProperty("count", recipe.count());
            json.addProperty("tier", recipe.tier());
            json.addProperty("essence", recipe.essence() == null ? "" : recipe.essence().toString());
            JsonArray ingredients = new JsonArray();
            for (SmithingIngredient ingredient : recipe.materials()) {
                JsonObject entry = new JsonObject();
                if (ingredient.item() != null) {
                    entry.addProperty("item", ingredient.item().toString());
                }
                if (ingredient.tag() != null) {
                    entry.addProperty("tag", ingredient.tag().toString());
                }
                entry.addProperty("count", ingredient.count());
                ingredients.add(entry);
            }
            json.add("materials", ingredients);
            recipeArray.add(json);
        }
        root.add("recipes", recipeArray);
        return root;
    }

    /** {@link #toJson()} の内容を読み込む（クライアント側）。 */
    public static void applyJson(JsonObject root) {
        SmithingDataLoader.applyDefinitions(root);
    }

    private static <T> List<T> sorted(Map<ResourceLocation, T> map, java.util.function.Function<T, ResourceLocation> id) {
        List<T> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing(value -> id.apply(value).toString()));
        return list;
    }

    private static JsonArray idArray(List<ResourceLocation> ids) {
        JsonArray array = new JsonArray();
        for (ResourceLocation id : ids) {
            array.add(id.toString());
        }
        return array;
    }

    private static String color(int value) {
        return String.format("#%06X", value & 0xFFFFFF);
    }
}
