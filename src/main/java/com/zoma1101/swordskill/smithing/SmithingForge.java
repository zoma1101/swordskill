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

import java.util.List;

/**
 * 作業台（5×5）に置いた素材と、下の補強スロットから鍛造武器を組み立てる。
 *
 * <p>武器種は一致した形（{@link WeaponPattern}）で決まり、刀身・鍔・柄の見た目は
 * 置いた素材（{@link SmithingMaterial#bladeFor} / {@link SmithingMaterial#guard} /
 * {@link SmithingMaterial#handle}）と形の既定値から決まる。</p>
 *
 * <p>強さは「刀身に使った素材の強さ」と「鍛冶スキル（武器カテゴリの熟練度）」で決まり、
 * 補強スロットに入れた素材で威力を底上げできる。必要 STR は重さから算出し、
 * 装備判定は {@link SmithingEquipmentHandler} が行う。</p>
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
        SmithingPart blade = data.bladePart();
        double powerFactor = blade == null ? 1.0D : blade.power();
        powerFactor *= partPower(data.guard());
        powerFactor *= partPower(data.handle());

        SmithingMaterial essence = SmithingDefinitions.material(data.essence());
        double essenceFactor = essence == null ? 1.0D : Math.max(0.0D, essence.power());

        double base = ServerConfig.forgeBaseDamage.get()
                + Math.max(0.0D, data.power()) * ServerConfig.forgeMaterialPowerScale.get();
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
    // 作成・溶解・強化
    // ------------------------------------------------------------------

    /**
     * 一致した形と補強素材から武器を作る。
     *
     * @param match         一致した形と、役割ごとに置かれていた素材
     * @param reinforcement 補強スロット（溶解インゴット / 特殊素材 / 底上げ素材）
     */
    public static ItemStack craft(ServerPlayer player, WeaponPattern.Match match, List<ItemStack> reinforcement) {
        if (match == null) {
            return ItemStack.EMPTY;
        }
        SmithingMaterial main = SmithingDefinitions.materialFor(match.mainStack());
        if (main == null) {
            return ItemStack.EMPTY;
        }
        return forge(player, match.pattern(), main, firstMaterial(match.guard()), firstMaterial(match.handle()),
                reinforcement);
    }

    /**
     * 素材と形を直接指定して武器を作る。
     *
     * <p>{@code guardMaterial} / {@code handleMaterial} が無いときは刀身の素材で代用する。</p>
     */
    public static ItemStack forge(ServerPlayer player, WeaponPattern pattern, SmithingMaterial main,
            SmithingMaterial guardMaterial, SmithingMaterial handleMaterial, List<ItemStack> reinforcement) {
        if (pattern == null || main == null) {
            return ItemStack.EMPTY;
        }
        SkillData.WeaponType weaponType = pattern.weapon();
        SmithingMaterial guardSource = guardMaterial == null ? main : guardMaterial;
        SmithingMaterial handleSource = handleMaterial == null ? main : handleMaterial;

        SmithingPart blade = resolveBlade(pattern, main, weaponType);
        SmithingPart handle = resolvePart(pattern.handle(), handleSource.handle(),
                SmithingDefinitions.defaultHandle());
        if (blade == null || handle == null) {
            return ItemStack.EMPTY;
        }
        SmithingPart guard = resolvePart(pattern.guard(), guardSource.guard(), SmithingDefinitions.defaultGuard());

        // 補強スロット: インゴット（武器記憶）/ 特殊素材（能力付与）/ それ以外（威力の底上げ）
        WeaponMemory memory = WeaponMemory.EMPTY;
        ResourceLocation essenceId = null;
        double bonusPower = 0.0D;
        double ratio = Math.max(0.0D, ServerConfig.reinforcementPowerRatio.get());
        for (ItemStack stack : reinforcement) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (stack.is(SmithingRegistry.FORGED_INGOT.get())) {
                ForgedWeaponData ingot = ForgedWeaponData.get(stack);
                if (ingot != null) {
                    memory = memory.merge(ingot.memory());
                }
                continue;
            }
            SmithingMaterial material = SmithingDefinitions.materialFor(stack);
            if (material == null) {
                continue;
            }
            if (material.special() && essenceId == null) {
                essenceId = material.id();
            } else {
                bonusPower += Math.max(0.0D, material.power()) * ratio;
            }
        }

        SmithingMaterial essence = SmithingDefinitions.material(essenceId);
        int colorMain = essence == null ? main.colorMain() : essence.colorMain();
        int colorSub = essence == null ? guardSource.colorSub() : essence.colorSub();

        ForgedWeaponData draft = new ForgedWeaponData(
                main.id(),
                blade.id(),
                guard == null ? null : guard.id(),
                handle.id(),
                essence == null ? null : essence.id(),
                colorMain,
                colorSub,
                weaponType,
                qualityFor(player, weaponType),
                Math.max(0.0D, main.power()) + bonusPower,
                0,
                0,
                player == null ? "" : player.getName().getString(),
                memory);

        ForgedWeaponData data = draft.withRequiredStr(requiredStr(draft));
        ItemStack stack = new ItemStack(SmithingRegistry.FORGED_WEAPON.get());
        applyToStack(stack, data);
        return stack;
    }

    /** 刀身を決める。素材 → 形 → 既定 の順で、武器種に対応するものを探す。 */
    private static SmithingPart resolveBlade(WeaponPattern pattern, SmithingMaterial main,
            SkillData.WeaponType weaponType) {
        SmithingPart part = SmithingDefinitions.part(main.bladeFor(weaponType));
        if (part == null || !part.supports(weaponType)) {
            part = SmithingDefinitions.part(pattern.blade());
        }
        if (part == null || !part.supports(weaponType)) {
            part = SmithingDefinitions.defaultBlade(weaponType);
        }
        return part;
    }

    /** 鍔・柄を決める。素材 → 形 → 既定 の順。 */
    private static SmithingPart resolvePart(ResourceLocation patternPart, ResourceLocation materialPart,
            SmithingPart fallback) {
        SmithingPart part = SmithingDefinitions.part(materialPart);
        if (part == null) {
            part = SmithingDefinitions.part(patternPart);
        }
        return part == null ? fallback : part;
    }

    private static SmithingMaterial firstMaterial(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            SmithingMaterial material = SmithingDefinitions.materialFor(stack);
            if (material != null) {
                return material;
            }
        }
        return null;
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
