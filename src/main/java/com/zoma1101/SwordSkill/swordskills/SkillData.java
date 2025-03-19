package com.zoma1101.SwordSkill.swordskills;

import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.config.ServerConfig;
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
    private final boolean isHide;
    private final int final_tick;
    private final int transform_limit_tick;

    public SkillData(int id, String name,int cooldown, SkillType type, Class<? extends ISkill > skillClass, List<WeaponType> availableWeaponTypes, boolean isHide,int final_tick, int TransformLimitTick) {
        this.id = id;
        this.name = name;
        this.iconTexture = fromNamespaceAndPath(SwordSkill.MOD_ID,"textures/gui/"+this.name+".png");
        this.cooldown = cooldown;
        this.type = type;
        this.skillClass = skillClass;
        this.availableWeaponTypes = availableWeaponTypes;
        this.isHide = isHide;
        this.final_tick = final_tick;
        this.transform_limit_tick = TransformLimitTick;
    }

    // ゲッターメソッド
    public int getId() { return id; }
    public String getName() { return name; }
    public ResourceLocation getIconTexture() { return iconTexture; }
    public int getCooldown() {
        return (int) (cooldown * ServerConfig.cooldownMultiplier.get());
    }
    public List<WeaponType> getAvailableWeaponTypes() { return availableWeaponTypes; }
    public boolean isHide() {
        return isHide;
    }
    public int getFinalTick(){
        return final_tick;
    }
    public int getTransformLimitTick(){
        return transform_limit_tick;
    }

    public enum WeaponType {
        ONE_HANDED_SWORD, // 片手剣〇
        TWO_HANDED_SWORD, // 両手剣〇
        KATANA, // 刀〇
        AXE, // 斧〇
        RAPIER, //レイピア〇
        CLAW, // 片手爪〇
        SPEAR, // 槍〇
        WHIP, // 鞭
        SCYTHE, //鎌〇
        DAGGER, //短剣〇
        DUALSWORD, //二刀流〇
        NONE //設定しないでください
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