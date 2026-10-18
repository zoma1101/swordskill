package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 鍛冶の素材定義。データパックの {@code data/<namespace>/smithing_material/<id>.json} から読み込む。
 *
 * <p>敵実装で素材アイテムを追加するときは、この JSON の {@code items} / {@code tags} に
 * 追加するだけでよく、コード側の変更は不要。</p>
 *
 * @param tier     素材の階級（高いほど強い武器になる）
 * @param power    武器の攻撃力に掛かる素材の強さ
 * @param weight   重さ。武器全体の重さと必要 STR の基準になる
 * @param colorMain メインカラー (0xRRGGBB)
 * @param colorSub  サブカラー (0xRRGGBB)
 * @param items    この素材として扱うアイテム ID
 * @param tags     この素材として扱うアイテムタグ
 * @param special  ラストアタックボーナス等の特殊素材か
 */
public record SmithingMaterial(
                ResourceLocation id,
                int tier,
                double power,
                double weight,
                int colorMain,
                int colorSub,
                List<ResourceLocation> items,
                List<ResourceLocation> tags,
                boolean special) {

    /** 表示名の翻訳キー。 */
    public String translationKey() {
        return "material." + SwordSkill.MOD_ID + "." + id.getPath();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }
}
