package com.zoma1101.swordskill.smithing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 鍛冶レシピが要求する素材 1 種（対応素材）。
 *
 * <p>アイテム ID かアイテムタグのどちらかで指定する。素材アイテムを敵ドロップ等で
 * 後から追加するときは、データパックの JSON に ID を書くだけでよい。</p>
 *
 * @param item  要求するアイテム ID（タグ指定のときは null）
 * @param tag   要求するアイテムタグ（アイテム指定のときは null）
 * @param count 必要数
 */
public record SmithingIngredient(ResourceLocation item, ResourceLocation tag, int count) {

    public static SmithingIngredient ofItem(ResourceLocation item, int count) {
        return new SmithingIngredient(item, null, Math.max(1, count));
    }

    public static SmithingIngredient ofTag(ResourceLocation tag, int count) {
        return new SmithingIngredient(null, tag, Math.max(1, count));
    }

    /** スタックがこの素材を満たすか（数は見ない）。 */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (item != null) {
            Item value = BuiltInRegistries.ITEM.get(item);
            return value != null && stack.is(value);
        }
        if (tag != null) {
            return stack.is(TagKey.create(Registries.ITEM, tag));
        }
        return false;
    }

    /** 表示用のアイテム（タグのときは先頭の要素）。解決できなければ空。 */
    public ItemStack displayStack() {
        ItemStack byItem = itemStack();
        if (!byItem.isEmpty()) {
            return byItem;
        }
        if (tag != null) {
            return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tag))
                    .flatMap(set -> set.stream().findFirst())
                    .map(holder -> new ItemStack(holder.value()))
                    .orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack itemStack() {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        Item value = BuiltInRegistries.ITEM.get(item);
        return value == null ? ItemStack.EMPTY : new ItemStack(value);
    }
}
