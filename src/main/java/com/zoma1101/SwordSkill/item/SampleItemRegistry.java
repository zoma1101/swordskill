package com.zoma1101.SwordSkill.item;

import com.zoma1101.SwordSkill.main.SwordSkill;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SampleItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SwordSkill.MOD_ID);

    public static final RegistryObject<Item> SAMPLE_KATANA = ITEMS.register("sample_katana", () -> new SampleKatana(Tiers.IRON, 5, -2.4F, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
