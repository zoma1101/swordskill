package com.zoma1101.swordskill.smithing;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 鍛造武器。見た目と性能はスタックのコンポーネント（{@link ForgedWeaponData}）で決まる。 */
public class ForgedWeaponItem extends Item {

    public ForgedWeaponItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        return data == null ? super.getName(stack) : data.displayName();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        return data != null && (data.enhancement() > 0 || data.quality() >= 80);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        if (data == null) {
            return;
        }
        SmithingTooltip.append(tooltip, data);
    }
}
