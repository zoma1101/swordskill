package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 鍛冶レシピ。データパックの {@code data/<namespace>/smithing_recipe/<id>.json} から読み込む。
 *
 * <p>レシピ 1 つが「武器 1 種」に対応する。見た目と性能は刀身・鍔・柄の組み合わせで決まり、
 * 素材（メイン素材・対応素材）はプレイヤーが鍛冶台に入れる。</p>
 *
 * @param blade      刀身の部品 ID
 * @param guard      鍔の部品 ID（省略可）
 * @param handle     柄の部品 ID
 * @param weaponType 武器種（{@code NONE} なら刀身の武器種を使う）
 * @param count      メイン素材の必要数
 * @param tier       要求する素材の階級
 * @param materials  対応素材（メイン素材以外に必要なもの）
 * @param essence    特殊素材（ラストアタックボーナス等。省略可）
 */
public record SmithingRecipe(
        ResourceLocation id,
        ResourceLocation blade,
        ResourceLocation guard,
        ResourceLocation handle,
        SkillData.WeaponType weaponType,
        int count,
        int tier,
        List<SmithingIngredient> materials,
        ResourceLocation essence) {

    /** 対応素材スロットの数。これより多い対応素材を書いたレシピは先頭だけ使う。 */
    public static final int MAX_INGREDIENTS = 4;

    /** 使う部品の一覧（鍔は省略可）。 */
    public List<ResourceLocation> partIds() {
        List<ResourceLocation> ids = new ArrayList<>(3);
        if (blade != null) {
            ids.add(blade);
        }
        if (guard != null) {
            ids.add(guard);
        }
        if (handle != null) {
            ids.add(handle);
        }
        return ids;
    }

    /** 見た目のモデル番号（{@link SmithingCombos}）。 */
    public int modelIndex() {
        return SmithingCombos.modelIndex(blade, guard, handle);
    }

    /** 武器種。レシピ指定が無ければ刀身の武器種。 */
    public SkillData.WeaponType resolvedWeaponType() {
        if (weaponType != null && weaponType != SkillData.WeaponType.NONE) {
            return weaponType;
        }
        SmithingPart bladePart = SmithingDefinitions.part(blade);
        return bladePart == null ? SkillData.WeaponType.ONE_HANDED_SWORD : bladePart.primaryWeaponType();
    }

    public SmithingPart bladePart() {
        return SmithingDefinitions.part(blade);
    }

    public SmithingPart guardPart() {
        return guard == null ? null : SmithingDefinitions.part(guard);
    }

    public SmithingPart handlePart() {
        return handle == null ? null : SmithingDefinitions.part(handle);
    }

    /** この素材で作れるレシピか（素材の階級が足りているか）。 */
    public boolean availableFor(SmithingMaterial material) {
        return material != null && material.tier() >= tier;
    }

    /** 表示名（「直刀身・素朴な鍔・巻き柄」のように部品名を並べる）。 */
    public Component displayName() {
        MutableComponent name = Component.empty();
        name.append(partName(blade));
        if (guard != null) {
            name.append(Component.literal("・")).append(partName(guard));
        }
        if (handle != null) {
            name.append(Component.literal("・")).append(partName(handle));
        }
        return name;
    }

    private static Component partName(ResourceLocation id) {
        SmithingPart part = SmithingDefinitions.part(id);
        return part == null ? Component.literal(String.valueOf(id)) : part.displayName();
    }
}
