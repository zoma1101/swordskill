package com.zoma1101.SwordSkill.swordskills;

import com.zoma1101.SwordSkill.main.SwordSkill;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class SkillData {
    private final int id; // スキルID
    private final String name;
    private final ResourceLocation iconTexture;
    private final int cooldown; // クールダウン (tick)
    private final SkillType type; // スキルタイプ
    private final Class<? extends ISkill > skillClass; // 実行するスキルクラス
    private final List<WeaponType> availableWeaponTypes;
    private boolean isHide;
    private final int final_tick;

    public SkillData(int id, String name,int cooldown, SkillType type, Class<? extends ISkill > skillClass, List<WeaponType> availableWeaponTypes, boolean isHide,int final_tick) {
        this.id = id;
        this.name = name;
        this.iconTexture = fromNamespaceAndPath(SwordSkill.MOD_ID,"textures/gui/"+this.name+".png");
        this.cooldown = cooldown;
        this.type = type;
        this.skillClass = skillClass;
        this.availableWeaponTypes = availableWeaponTypes;
        this.isHide = isHide;
        this.final_tick = final_tick;
    }

    // ゲッターメソッド
    public int getId() { return id; }
    public String getName() { return name; }
    public ResourceLocation getIconTexture() { return iconTexture; }
    public int getCooldown() { return cooldown; }
    public List<WeaponType> getAvailableWeaponTypes() { return availableWeaponTypes; }
    public boolean isHide() {
        return isHide;
    }
    public int getFinalTick(){
        return final_tick;
    }

    public enum WeaponType {
        ONE_HANDED_SWORD, // 片手剣
        TWO_HANDED_SWORD, // 両手剣
        KATANA, // 刀
        AXE, // 斧
        RAPIER,
        MACE, // 棍棒
        ONE_HANDED_CLAW, // 片手爪
        DUAL_HANDED_CLAW, // 両手爪
        SPEAR, // 槍
        WHIP, // 鞭
        DUAL_WIELDING, // 二刀流
        BOW, //弓
        CROSSBOW, //クロスボウ
        SHIELD, //盾
        SCYTHE, //鎌
        DAGGER, //短剣
        DUALSWORD //二刀流
    }

    public SkillType getType() { return type; }
    public Class<? extends ISkill > getSkillClass() { return skillClass; }

    public enum SkillType {
        SIMPLE,
        TRANSFORM,
        TRANSFORM_FINISH,
        RUSH
    }
}