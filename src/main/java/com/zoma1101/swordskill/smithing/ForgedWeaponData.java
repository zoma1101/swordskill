package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * 鍛造武器（および溶解してできたインゴット）の中身。
 *
 * <p>1.21 のデータコンポーネント {@link DataComponents#CUSTOM_DATA} に
 * {@value #ROOT_KEY} のサブタグとして保存する。スタックに乗るのでチェストや
 * アイテムフレームに入れても保持され、クライアントにも自動同期される。</p>
 *
 * @param material    主素材
 * @param blade       刀身の部品 ID
 * @param guard       鍔の部品 ID（無しは null）
 * @param handle      柄の部品 ID
 * @param essence     特殊素材（ラストアタックボーナス等。無しは null）
 * @param colorMain   メインカラー (0xRRGGBB)
 * @param colorSub    サブカラー (0xRRGGBB)
 * @param quality     鍛冶スキルに応じた品質 (0-100)
 * @param enhancement 強化値
 * @param requiredStr 装備に必要な STR
 * @param forgedBy    鍛えたプレイヤー名
 * @param memory      武器記憶
 */
public record ForgedWeaponData(
        ResourceLocation material,
        ResourceLocation blade,
        ResourceLocation guard,
        ResourceLocation handle,
        ResourceLocation essence,
        int colorMain,
        int colorSub,
        SkillData.WeaponType weaponType,
        int quality,
        int enhancement,
        int requiredStr,
        String forgedBy,
        WeaponMemory memory) {

    public static final String ROOT_KEY = "swordskill:forge";

    public ForgedWeaponData withMemory(WeaponMemory newMemory) {
        return new ForgedWeaponData(material, blade, guard, handle, essence, colorMain, colorSub, weaponType,
                quality, enhancement, requiredStr, forgedBy, newMemory);
    }

    public ForgedWeaponData withEnhancement(int level) {
        return new ForgedWeaponData(material, blade, guard, handle, essence, colorMain, colorSub, weaponType,
                quality, level, requiredStr, forgedBy, memory);
    }

    public ForgedWeaponData withRequiredStr(int value) {
        return new ForgedWeaponData(material, blade, guard, handle, essence, colorMain, colorSub, weaponType,
                quality, enhancement, value, forgedBy, memory);
    }

    /** 使われている部品 ID の一覧（鍔は省略可）。 */
    public List<ResourceLocation> partIds() {
        List<ResourceLocation> ids = new ArrayList<>(3);
        if (blade != null) {
            ids.add(blade);
        }
        if (guard != null) {
            ids.add(guard);
        }
        if (handle != null) {
            ids.add(handle);
        }
        return ids;
    }

    public SmithingPart bladePart() {
        return SmithingDefinitions.part(blade);
    }

    public SmithingMaterial mainMaterial() {
        return SmithingDefinitions.material(material);
    }

    /** 表示名（「+2 鉄の刀」のような形）。 */
    public Component displayName() {
        MutableComponent name;
        SmithingMaterial main = mainMaterial();
        SmithingPart bladePart = bladePart();
        if (main != null && bladePart != null) {
            name = Component.translatable("item." + SwordSkill.MOD_ID + ".forged_weapon.name",
                    main.displayName(), bladePart.displayName());
        } else if (main != null) {
            name = Component.translatable("item." + SwordSkill.MOD_ID + ".forged_weapon.name_simple",
                    main.displayName());
        } else {
            name = Component.translatable("item." + SwordSkill.MOD_ID + ".forged_weapon");
        }
        if (enhancement > 0) {
            name = Component.translatable("item." + SwordSkill.MOD_ID + ".forged_weapon.enhanced", name,
                    enhancement);
        }
        return name;
    }

    // ------------------------------------------------------------------
    // 読み書き
    // ------------------------------------------------------------------

    /** スタックが鍛造武器（または溶解インゴット）なら中身を返す。 */
    public static ForgedWeaponData get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(ROOT_KEY)) {
            return null;
        }
        try {
            return load(tag.getCompound(ROOT_KEY));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static void set(ItemStack stack, ForgedWeaponData data) {
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        tag.put(ROOT_KEY, save(data));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static CompoundTag save(ForgedWeaponData data) {
        CompoundTag tag = new CompoundTag();
        putId(tag, "material", data.material());
        putId(tag, "blade", data.blade());
        putId(tag, "guard", data.guard());
        putId(tag, "handle", data.handle());
        putId(tag, "essence", data.essence());
        tag.putInt("color_main", data.colorMain());
        tag.putInt("color_sub", data.colorSub());
        tag.putString("weapon", data.weaponType().name());
        tag.putInt("quality", data.quality());
        tag.putInt("enhancement", data.enhancement());
        tag.putInt("required_str", data.requiredStr());
        tag.putString("forged_by", data.forgedBy() == null ? "" : data.forgedBy());
        tag.put("memory", data.memory().save());
        return tag;
    }

    public static ForgedWeaponData load(CompoundTag tag) {
        return new ForgedWeaponData(
                readId(tag, "material"),
                readId(tag, "blade"),
                readId(tag, "guard"),
                readId(tag, "handle"),
                readId(tag, "essence"),
                tag.getInt("color_main"),
                tag.getInt("color_sub"),
                parseWeapon(tag.getString("weapon")),
                tag.getInt("quality"),
                tag.getInt("enhancement"),
                tag.getInt("required_str"),
                tag.getString("forged_by"),
                tag.contains("memory") ? WeaponMemory.load(tag.getCompound("memory")) : WeaponMemory.EMPTY);
    }

    private static void putId(CompoundTag tag, String key, ResourceLocation id) {
        tag.putString(key, id == null ? "" : id.toString());
    }

    private static ResourceLocation readId(CompoundTag tag, String key) {
        String value = tag.getString(key);
        return value.isEmpty() ? null : ResourceLocation.tryParse(value);
    }

    private static SkillData.WeaponType parseWeapon(String name) {
        try {
            return SkillData.WeaponType.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return SkillData.WeaponType.NONE;
        }
    }
}
