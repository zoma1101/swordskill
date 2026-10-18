package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 鍛造武器の部品定義。データパックの {@code data/<namespace>/smithing_part/<id>.json} から読み込む。
 *
 * @param kind        部品の種類
 * @param weapons     この部品で作れる武器種（空なら全武器種）
 * @param texture     テクスチャ番号（例: {@code blade_01}）。自動生成モデルが参照する
 * @param power       攻撃力に掛かる係数
 * @param weight      重さに掛かる係数
 * @param attackSpeed 攻撃速度（{@link SmithingPartKind#BLADE} のときだけ使う）
 * @param colorSlot   素材のどちらの色を乗せるか
 * @param tier        部品の階級（武器種の決定や見た目のランク付けに使う）
 */
public record SmithingPart(
                ResourceLocation id,
                SmithingPartKind kind,
                List<SkillData.WeaponType> weapons,
                String texture,
                double power,
                double weight,
                double attackSpeed,
                SmithingColorSlot colorSlot,
                int tier) {

    /** 表示名の翻訳キー。 */
    public String translationKey() {
        return "part." + SwordSkill.MOD_ID + "." + id.getPath();
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public boolean supports(SkillData.WeaponType weaponType) {
        return weapons.isEmpty() || weapons.contains(weaponType);
    }

    /** 部品が決める武器種（先頭の武器種）。空なら片手剣。 */
    public SkillData.WeaponType primaryWeaponType() {
        return weapons.isEmpty() ? SkillData.WeaponType.ONE_HANDED_SWORD : weapons.get(0);
    }
}
