package com.zoma1101.SwordSkill.item;

import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SampleItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SwordSkill.MOD_ID);

    public static final RegistryObject<Item> SAMPLE_KATANA = ITEMS.register("sample_katana", () -> new SampleKatana(Tiers.IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_BREAD = ITEMS.register("sample_great_sword", () -> new SampleKatana(Tiers.IRON, 5, -3.2F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_RAPIER = ITEMS.register("sample_rapier", () -> new SampleKatana(Tiers.IRON, 3, -2.0F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_DAGGER = ITEMS.register("sample_dagger", () -> new SampleKatana(Tiers.IRON, 2, -1.8F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_SCYTHE = ITEMS.register("sample_scythe", () -> new SampleKatana(Tiers.IRON, 4, -3.0F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_WHIP = ITEMS.register("sample_whip", () -> new SampleKatana(Tiers.IRON, 3, -2.6F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_CLAW = ITEMS.register("sample_claw", () -> new SampleKatana(Tiers.IRON, 3, -2.6F, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
