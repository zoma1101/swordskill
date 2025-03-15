package com.zoma1101.SwordSkill.data;

import com.zoma1101.SwordSkill.swordskills.SkillData;

import java.util.Set;

public class WeaponData {
    private final Set<SkillData.WeaponType> weaponType;
    private final String weaponName;

    public WeaponData(Set<SkillData.WeaponType> weaponType, String weaponName) {
        this.weaponType = weaponType;
        this.weaponName = weaponName;
    }

    public Set<SkillData.WeaponType> getWeaponType() {
        return weaponType;
    }

    public String getWeaponName() {
        return weaponName;
    }
}