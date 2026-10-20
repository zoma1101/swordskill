package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final DataSkillDefinition dataDefinition;

    // ★追加: 外部データを保存するためのマップ
    private final Map<String, Object> additionalData = new HashMap<>();

    public SkillData(int id, String name, int cooldown, double spCost, SkillType type,
            Class<? extends ISkill> skillClass, List<WeaponType> availableWeaponTypes, boolean isHide, int final_tick,
            int TransformLimitTick) {
        this(id, name, cooldown, spCost, type, skillClass, availableWeaponTypes, isHide, final_tick,
                TransformLimitTick, null);
    }

    public SkillData(int id, String name, int cooldown, double spCost, SkillType type,
            Class<? extends ISkill> skillClass, List<WeaponType> availableWeaponTypes, boolean isHide, int final_tick,
            int TransformLimitTick, DataSkillDefinition dataDefinition) {
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
        this.dataDefinition = dataDefinition;
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
        // 前のステップでの要求に基づき、ベースを50%短縮
        return (int) (cooldown * 0.5 * ServerConfig.cooldownMultiplier.get());
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

    // ★追加: 外部データへのアクセサ
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    /** 熟練度のカテゴリ。JSON 未指定のときは先頭の武器種を使う。 */
    public WeaponType getProficiencyCategory() {
        if (dataDefinition != null && dataDefinition.proficiencyCategory() != null) {
            return dataDefinition.proficiencyCategory();
        }
        return availableWeaponTypes.isEmpty() ? WeaponType.NONE : availableWeaponTypes.get(0);
    }

    /** 解放に必要な熟練度（0 なら熟練度不要）。 */
    public int getUnlockProficiency() {
        return dataDefinition == null ? 0 : dataDefinition.unlockProficiency();
    }

    /** 最初から解放されているスキルか（武器カテゴリごとの初期スキル）。 */
    public boolean isDefaultUnlocked() {
        return dataDefinition != null && dataDefinition.defaultUnlocked();
    }

    /** スロットにセットできるスキルか（説明用エントリなどは false）。 */
    public boolean isAssignable() {
        return dataDefinition == null || dataDefinition.assignable();
    }

    // ★追加: 熟練度設定用のヘルパーメソッド
    @SuppressWarnings("unchecked")
    public void setRequiredProficiency(String weaponType, int amount) {
        // "proficiency_req" というキーの中に、さらに "WeaponType -> Amount" のマップを作る構造
        Map<String, Integer> reqs = (Map<String, Integer>) additionalData.computeIfAbsent("proficiency_req",
                k -> new HashMap<String, Integer>());
        reqs.put(weaponType, amount);
    }

    @SuppressWarnings("unchecked")
    public int getRequiredProficiency(String weaponType) {
        if (additionalData.containsKey("proficiency_req")) {
            Map<String, Integer> reqs = (Map<String, Integer>) additionalData.get("proficiency_req");
            return reqs.getOrDefault(weaponType, 0);
        }
        return 0;
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
        NONE // 設定しないでください
        ;

        /** 武器種の表示名（lang: {@code swordskill.weapon.<type>}）。 */
        public net.minecraft.network.chat.Component displayName() {
            return net.minecraft.network.chat.Component.translatable(
                    SwordSkill.MOD_ID + ".weapon." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public SkillType getType() {
        return type;
    }

    public Class<? extends ISkill> getSkillClass() {
        return skillClass;
    }

    public DataSkillDefinition getDataDefinition() {
        return dataDefinition;
    }

    public enum SkillType {
        SIMPLE,
        TRANSFORM,
        TRANSFORM_FINISH,
        RUSH
    }
}