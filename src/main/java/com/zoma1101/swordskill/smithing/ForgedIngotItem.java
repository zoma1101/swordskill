package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 溶解した武器のインゴット。素材と武器記憶を保持し、よい素材と組み合わせて
 * 鍛造し直す（再鍛造）ことで、育った隠しパラメータを引き継げる。
 */
public class ForgedIngotItem extends Item {

    public ForgedIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        if (data == null || data.mainMaterial() == null) {
            return super.getName(stack);
        }
        return Component.translatable("item." + SwordSkill.MOD_ID + ".forged_ingot.named",
                data.mainMaterial().displayName());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        return data != null && data.enhancement() > 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        if (data == null) {
            return;
        }
        tooltip.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".smithing.tooltip.ingot"));
    }
}
