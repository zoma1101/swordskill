package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap; // 追加
import java.util.List;
import java.util.Map; // 追加

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class SkillData {
    private final int id; // スキルID
    private final String name;
    private final ResourceLocation iconTexture;
    private final int cooldown; // クールダウン (tick)
    private final SkillType type; // スキルタイプ
    private final Class<? extends ISkill> skillClass; // 実行するスキルクラス
    private final List<WeaponType> availableWeaponTypes;
    private final boolean isHide;
    private final int final_tick;
    private final int transform_limit_tick;
    private final double spCost;

    // トレイル設定
    private final int trailColor;
    private final ResourceLocation trailTexture;
    private final int trailMaxLength;
    private final float trailBaseOffset; // ★追加: 根元のオフセット
    private final float trailTipOffset; // ★追加: 先端のオフセット
    private final float trailArcAngle; // ★追加: 円弧の角度
    private final int trailPointCount; // ★追加: 分割数

    // ★追加: 外部データを保存するためのマップ
    private final Map<String, Object> additionalData = new HashMap<>();

    public SkillData(int id, String name, int cooldown, double spCost, SkillType type,
            Class<? extends ISkill> skillClass, List<WeaponType> availableWeaponTypes, boolean isHide, int final_tick,
            int TransformLimitTick, int trailColor, String trailTexture, int trailMaxLength, float trailBaseOffset,
            float trailTipOffset, float trailArcAngle, int trailPointCount) {
        this.id = id;
        this.name = name;
        this.iconTexture = fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/gui/" + this.name + ".png");
        this.cooldown = cooldown;
        this.spCost = spCost;
        this.type = type;
        this.skillClass = skillClass;
        this.availableWeaponTypes = availableWeaponTypes;
        this.isHide = isHide;
        this.final_tick = final_tick;
        this.transform_limit_tick = TransformLimitTick;
        this.trailColor = trailColor;
        this.trailTexture = fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + trailTexture + ".png");
        this.trailMaxLength = trailMaxLength;
        this.trailBaseOffset = trailBaseOffset;
        this.trailTipOffset = trailTipOffset;
        this.trailArcAngle = trailArcAngle;
        this.trailPointCount = trailPointCount;
    }

    public SkillData(int id, String name, int cooldown, double spCost, SkillType type,
            Class<? extends ISkill> skillClass, List<WeaponType> availableWeaponTypes, boolean isHide, int final_tick,
            int TransformLimitTick, int trailColor, String trailTexture, int trailMaxLength, float trailBaseOffset,
            float trailTipOffset) {
        this(id, name, cooldown, spCost, type, skillClass, availableWeaponTypes, isHide, final_tick, TransformLimitTick,
                trailColor, trailTexture, trailMaxLength, trailBaseOffset, trailTipOffset, 0f, 2);
    }

    // デフォルト設定用コンストラクタ
    public SkillData(int id, String name, int cooldown, double spCost, SkillType type,
            Class<? extends ISkill> skillClass, List<WeaponType> availableWeaponTypes, boolean isHide, int final_tick,
            int TransformLimitTick) {
        this(id, name, cooldown, spCost, type, skillClass, availableWeaponTypes, isHide, final_tick, TransformLimitTick,
                0xFF33AAFF, "simple_2", 10, 1.6f, 3.2f);
    }

    // ゲッターメソッド
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getIconTexture() {
        return iconTexture;
    }

    public int getCooldown() {
        return (int) (cooldown * ServerConfig.cooldownMultiplier.get());
    }

    public List<WeaponType> getAvailableWeaponTypes() {
        return availableWeaponTypes;
    }

    public double getSpCost() {
        return spCost;
    }

    public boolean isHide() {
        return isHide;
    }

    public int getFinalTick() {
        return final_tick;
    }

    public int getTransformLimitTick() {
        return transform_limit_tick;
    }

    public int getTrailColor() {
        return trailColor;
    }

    public ResourceLocation getTrailTexture() {
        return trailTexture;
    }

    public int getTrailMaxLength() {
        return trailMaxLength;
    }

    public float getTrailBaseOffset() {
        return trailBaseOffset;
    }

    public float getTrailTipOffset() {
        return trailTipOffset;
    }

    public float getTrailArcAngle() {
        return trailArcAngle;
    }

    public int getTrailPointCount() {
        return trailPointCount;
    }

    // ★追加: 外部データへのアクセサ
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    // ★追加: 熟練度設定用のヘルパーメソッド（あると便利）
    public void setRequiredProficiency(String weaponType, int amount) {
        // "proficiency_req" というキーの中に、さらに "WeaponType -> Amount" のマップを作る構造
        Map<String, Integer> reqs = (Map<String, Integer>) additionalData.computeIfAbsent("proficiency_req",
                k -> new HashMap<String, Integer>());
        reqs.put(weaponType, amount);
    }

    public int getRequiredProficiency(String weaponType) {
        if (additionalData.containsKey("proficiency_req")) {
            Map<String, Integer> reqs = (Map<String, Integer>) additionalData.get("proficiency_req");
            return reqs.getOrDefault(weaponType, 0);
        }
        return 0;
    }

    // --- トレイル追跡部位の設定 ---
    public SkillData setFollowBone(FollowBone bone) {
        additionalData.put("follow_bone", bone);
        return this;
    }
 
    public FollowBone getFollowBone() {
        return (FollowBone) additionalData.getOrDefault("follow_bone", FollowBone.MAIN_HAND);
    }
 
    public enum FollowBone {
        MAIN_HAND,
        OFF_HAND,
        RIGHT_HAND,
        LEFT_HAND,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG,
        BOTH_LEGS,
        HEAD
    }
 
    public enum WeaponType {
        ONE_HANDED_SWORD, // 片手剣〇
        TWO_HANDED_SWORD, // 両手剣〇
        KATANA, // 刀〇
        AXE, // 斧〇
        RAPIER, // レイピア〇
        CLAW, // 片手爪〇
        SPEAR, // 槍〇
        MACE, // メイス〇
        SCYTHE, // 鎌〇
        DAGGER, // 短剣〇
        DUALSWORD, // 二刀流〇
        MARTIAL_ARTS, // 体術
        NONE // 設定しないでください
    }

    public SkillType getType() {
        return type;
    }

    public Class<? extends ISkill> getSkillClass() {
        return skillClass;
    }

    public enum SkillType {
        SIMPLE,
        TRANSFORM,
        TRANSFORM_FINISH,
        RUSH
    }
}