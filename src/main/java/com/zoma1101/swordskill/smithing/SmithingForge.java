package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.proficiency.ProficiencyManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 部品と素材から鍛造武器を組み立てる。
 *
 * <p>強さは「素材の強さ」と「鍛冶スキル（武器カテゴリの熟練度）」で決まる。
 * 必要 STR は重さから算出し、装備判定は {@link SmithingEquipmentHandler} が行う。</p>
 */
public final class SmithingForge {

    private SmithingForge() {
    }

    // ------------------------------------------------------------------
    // 性能計算
    // ------------------------------------------------------------------

    /** 武器の重さ。素材の重さ × 部品の重さ係数。 */
    public static double weight(ForgedWeaponData data) {
        SmithingMaterial material = data.mainMaterial();
        double base = material == null ? 10.0D : material.weight();
        double factor = 0.0D;
        for (ResourceLocation id : data.partIds()) {
            SmithingPart part = SmithingDefinitions.part(id);
            factor += part == null ? 1.0D : part.weight();
        }
        return base * Math.max(0.1D, factor);
    }

    /** 装備に必要な STR。 */
    public static int requiredStr(ForgedWeaponData data) {
        return (int) Math.ceil(weight(data) * ServerConfig.strPerWeight.get());
    }

    /** 攻撃力（バニラの表示攻撃力に相当する値）。 */
    public static double attackDamage(ForgedWeaponData data) {
        SmithingMaterial material = data.mainMaterial();
        SmithingPart blade = data.bladePart();
        double powerFactor = blade == null ? 1.0D : blade.power();
        powerFactor *= partPower(data.guard());
        powerFactor *= partPower(data.handle());

        SmithingMaterial essence = SmithingDefinitions.material(data.essence());
        double essenceFactor = essence == null ? 1.0D : Math.max(0.0D, essence.power());

        double base = ServerConfig.forgeBaseDamage.get()
                + (material == null ? 0.0D : material.power()) * ServerConfig.forgeMaterialPowerScale.get();
        double qualityFactor = 1.0D + (data.quality() / (double) Math.max(1, ServerConfig.qualityMax.get()))
                * ServerConfig.forgeQualityDamageBonus.get();
        double enhanceFactor = 1.0D + data.enhancement() * ServerConfig.forgeEnhanceDamageBonus.get();
        return base * powerFactor * qualityFactor * enhanceFactor * essenceFactor;
    }

    /** 攻撃速度（バニラの攻撃速度に相当する値）。 */
    public static double attackSpeed(ForgedWeaponData data) {
        SmithingPart blade = data.bladePart();
        return blade == null ? -2.4D : blade.attackSpeed();
    }

    private static double partPower(ResourceLocation id) {
        SmithingPart part = SmithingDefinitions.part(id);
        return part == null ? 1.0D : part.power();
    }

    /** 鍛冶スキル（武器カテゴリの熟練度）から品質を求める。 */
    public static int qualityFor(ServerPlayer player, SkillData.WeaponType weaponType) {
        if (player == null) {
            return 0;
        }
        int proficiency = ProficiencyManager.getProficiency(player, weaponType);
        double perPoint = Math.max(0.01D, ServerConfig.qualityProficiencyPerPoint.get());
        int quality = (int) Math.floor(proficiency / perPoint);
        return Math.max(0, Math.min(ServerConfig.qualityMax.get(), quality));
    }

    public static int qualityFor(ServerPlayer player, ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        return data == null ? 0 : qualityFor(player, data.weaponType());
    }

    // ------------------------------------------------------------------
    // 作成・再鍛造・溶解・強化
    // ------------------------------------------------------------------

    /** 部品構成と素材から武器を作る。{@code inheritedMemory} に武器記憶を渡すと引き継ぐ（再鍛造）。 */
    public static ItemStack create(ServerPlayer player, SmithingMaterial material, SmithingRecipe recipe,
            WeaponMemory inheritedMemory) {
        return create(player, material, recipe, recipe == null ? null : recipe.essence(), inheritedMemory);
    }

    /** 特殊素材（{@code essenceId}）を明示して武器を作る。 */
    public static ItemStack create(ServerPlayer player, SmithingMaterial material, SmithingRecipe recipe,
            ResourceLocation essenceId, WeaponMemory inheritedMemory) {
        if (material == null || recipe == null) {
            return ItemStack.EMPTY;
        }
        SmithingPart blade = recipe.bladePart();
        SmithingPart guard = recipe.guardPart();
        SmithingPart handle = recipe.handlePart();
        if (blade == null || handle == null) {
            return ItemStack.EMPTY;
        }
        SkillData.WeaponType weaponType = recipe.resolvedWeaponType();
        SmithingMaterial essence = SmithingDefinitions.material(essenceId);

        int colorMain = essence == null ? material.colorMain() : essence.colorMain();
        int colorSub = essence == null ? material.colorSub() : essence.colorSub();

        ForgedWeaponData draft = new ForgedWeaponData(
                material.id(),
                blade.id(),
                guard == null ? null : guard.id(),
                handle.id(),
                essence == null ? null : essence.id(),
                colorMain,
                colorSub,
                weaponType,
                qualityFor(player, weaponType),
                0,
                0,
                player == null ? "" : player.getName().getString(),
                inheritedMemory == null ? WeaponMemory.EMPTY : inheritedMemory);

        ForgedWeaponData data = draft.withRequiredStr(requiredStr(draft));
        ItemStack stack = new ItemStack(SmithingRegistry.FORGED_WEAPON.get());
        applyToStack(stack, data);
        return stack;
    }

    /** インゴットと素材から鍛造し直す。武器記憶を引き継ぐ。 */
    public static ItemStack reforge(ServerPlayer player, ItemStack ingot, SmithingMaterial material,
            SmithingRecipe recipe) {
        ForgedWeaponData source = ForgedWeaponData.get(ingot);
        WeaponMemory memory = source == null ? WeaponMemory.EMPTY : source.memory();
        ResourceLocation essence = recipe != null && recipe.essence() != null ? recipe.essence()
                : (source == null ? null : source.essence());
        return create(player, material, recipe, essence, memory);
    }

    /** 武器を溶かしてインゴットにする。武器記憶はそのまま引き継がれる。 */
    public static ItemStack melt(ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return ItemStack.EMPTY;
        }
        ItemStack ingot = new ItemStack(SmithingRegistry.FORGED_INGOT.get());
        ForgedWeaponData.set(ingot, data);
        return ingot;
    }

    /**
     * 強化後の見た目だけを作る（GUI のプレビュー用）。素材は消費しない。
     *
     * @return 強化できないときは {@link ItemStack#EMPTY}
     */
    public static ItemStack previewEnhance(ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null || data.enhancement() >= ServerConfig.forgeEnhanceMax.get()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = weapon.copy();
        applyToStack(copy, data.withEnhancement(data.enhancement() + 1));
        return copy;
    }

    /** 強化素材の必要数。作成素材の {@code enhancementCostRatio} 分。 */
    public static int enhancementCost() {
        double raw = ServerConfig.creationMaterialCost.get() * ServerConfig.enhancementCostRatio.get();
        return Math.max(0, (int) Math.ceil(raw));
    }

    /**
     * 武器を 1 段階強化する。作成に使った素材の 1 割を消費する。
     *
     * @return 成功したら true
     */
    public static boolean enhance(ServerPlayer player, ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return false;
        }
        if (data.enhancement() >= ServerConfig.forgeEnhanceMax.get()) {
            return false;
        }
        if (player != null && !consumeMaterials(player, data, enhancementCost())) {
            return false;
        }
        applyToStack(weapon, data.withEnhancement(data.enhancement() + 1));
        return true;
    }

    private static boolean consumeMaterials(ServerPlayer player, ForgedWeaponData data, int cost) {
        if (cost <= 0 || player.isCreative()) {
            return true;
        }
        SmithingMaterial material = data.mainMaterial();
        if (material == null) {
            return true;
        }
        int available = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (SmithingDefinitions.isMaterialItem(material, stack)) {
                available += stack.getCount();
            }
        }
        if (available < cost) {
            return false;
        }
        int remaining = cost;
        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                break;
            }
            if (SmithingDefinitions.isMaterialItem(material, stack)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // スタックへの反映
    // ------------------------------------------------------------------

    /** コンポーネント（属性・レアリティ・見た目・中身）をまとめて書き込む。 */
    public static void applyToStack(ItemStack stack, ForgedWeaponData data) {
        ForgedWeaponData.set(stack, data);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, buildModifiers(data));
        stack.set(DataComponents.RARITY, rarityFor(data));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(SmithingCombos.modelIndex(data.blade(), data.guard(), data.handle())));
    }

    private static ItemAttributeModifiers buildModifiers(ForgedWeaponData data) {
        // 表示攻撃力は「プレイヤー素手 1.0 + モディファイア」なので、素手分を引いておく
        double damage = Math.max(0.0D, attackDamage(data) - 1.0D);
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed(data),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static Rarity rarityFor(ForgedWeaponData data) {
        int score = data.quality() + data.enhancement() * 5;
        if (data.essence() != null || score >= 90) {
            return Rarity.EPIC;
        }
        if (score >= 60) {
            return Rarity.RARE;
        }
        return Rarity.UNCOMMON;
    }
}
