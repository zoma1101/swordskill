package com.zoma1101.swordskill.swordskills;

import java.util.Set;

/**
 * 別カテゴリのスキルを「現在装備中の武器で使える」場合の熟練度ルール。
 *
 * <p>スキルのカテゴリと装備武器が一致しないとき、そのスキルが装備武器でも使えるなら
 * 装備武器側のカテゴリの熟練度を使い、解放条件とレベルアップ消費を 1.5 倍にする。</p>
 */
public final class ProficiencyRules {

    /** 別カテゴリで使うときの熟練度倍率。 */
    public static final double CROSS_CATEGORY_MULTIPLIER = 1.5D;

    private ProficiencyRules() {
    }

    /** スキルの実効カテゴリ。別カテゴリなら装備武器側のカテゴリを返す。 */
    public static SkillData.WeaponType effectiveCategory(SkillData skill, Set<SkillData.WeaponType> equipped) {
        SkillData.WeaponType base = skill == null ? SkillData.WeaponType.NONE : skill.getProficiencyCategory();
        if (equipped == null || equipped.isEmpty() || base == SkillData.WeaponType.NONE) {
            return base;
        }
        if (equipped.contains(base)) {
            return base;
        }
        // スキル側の定義順で、装備武器と一致するものを優先する
        for (SkillData.WeaponType type : skill.getAvailableWeaponTypes()) {
            if (equipped.contains(type)) {
                return type;
            }
        }
        return base;
    }

    /** 別カテゴリ扱い（1.5 倍ルールを適用する）かどうか。 */
    public static boolean isCrossCategory(SkillData skill, Set<SkillData.WeaponType> equipped) {
        if (skill == null || equipped == null || equipped.isEmpty()) {
            return false;
        }
        SkillData.WeaponType base = skill.getProficiencyCategory();
        return base != SkillData.WeaponType.NONE && effectiveCategory(skill, equipped) != base;
    }

    /** 1.5 倍（端数切り上げ）。 */
    public static int scale(int value) {
        return (int) Math.ceil(value * CROSS_CATEGORY_MULTIPLIER);
    }
}
