package com.zoma1101.swordskill.item;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SampleItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SwordSkill.MOD_ID);

    public static final RegistryObject<Item> SAMPLE_KATANA = ITEMS.register("sample_katana", () -> new SampleWeapon(Tiers.IRON, 3, -2.4F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_BREAD = ITEMS.register("sample_great_sword", () -> new SampleWeapon(Tiers.IRON, 5, -2.9F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_RAPIER = ITEMS.register("sample_rapier", () -> new SampleWeapon(Tiers.IRON, 3, -2.0F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_DAGGER = ITEMS.register("sample_dagger", () -> new SampleWeapon(Tiers.IRON, 2, -1.8F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_SCYTHE = ITEMS.register("sample_scythe", () -> new SampleWeapon(Tiers.IRON, 4, -2.8F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_WHIP = ITEMS.register("sample_whip", () -> new SampleWeapon(Tiers.IRON, 3, -2.6F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_CLAW = ITEMS.register("sample_claw", () -> new SampleWeapon(Tiers.IRON, 3, -1F, new Item.Properties()));
    public static final RegistryObject<Item> SAMPLE_MACE = ITEMS.register("sample_mace", () -> new SampleWeapon(Tiers.IRON, 5, -3.1F, new Item.Properties()));
    public static final RegistryObject<Item> UNLOCKITEM = ITEMS.register("unlock_item", () -> new SkillUnlockItem(new Item.Properties().rarity(Rarity.EPIC), 2));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
