package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
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

    /** 武器名（武器種名。武器種が無ければ刀身の部品名）。 */
    public Component weaponName() {
        SkillData.WeaponType type = resolvedWeaponType();
        if (type != null && type != SkillData.WeaponType.NONE) {
            return type.displayName();
        }
        SmithingPart bladePart = bladePart();
        return bladePart != null ? bladePart.displayName() : Component.literal(String.valueOf(blade));
    }

    /** 素材を添えた表示名（一覧用。素材が無ければ武器名だけ）。 */
    public Component displayName(SmithingMaterial material) {
        Component weapon = weaponName();
        if (material != null) {
            return Component.translatable("item." + SwordSkill.MOD_ID + ".forged_weapon.name",
                    material.displayName(), weapon);
        }
        return weapon;
    }

    /** 使う部品を並べた表示（「直刀身・素朴な鍔・巻き柄」）。 */
    public Component partsName() {
        MutableComponent parts = Component.empty();
        boolean first = true;
        for (ResourceLocation id : partIds()) {
            SmithingPart part = SmithingDefinitions.part(id);
            if (part == null) {
                continue;
            }
            if (!first) {
                parts.append(Component.literal("・"));
            }
            parts.append(part.displayName());
            first = false;
        }
        return parts;
    }

    /** 表示名（素材が分からない文脈では武器名のみ）。 */
    public Component displayName() {
        return displayName(null);
    }
}
