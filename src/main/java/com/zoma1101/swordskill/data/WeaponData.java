package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.swordskills.SkillData;

import java.util.Set;

public record WeaponData(Set<SkillData.WeaponType> weaponType, String weaponName) {
}