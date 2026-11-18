package com.zoma1101.swordskill.item;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;


public class SampleItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SwordSkill.MOD_ID);
    public static final RegistryObject<Item> SAMPLE_KATANA = RegiSampleSword("sample_katana", 3, -2.4F);
    public static final RegistryObject<Item> SAMPLE_BREAD = RegiSampleSword("sample_great_sword", 5, -2.9F);
    public static final RegistryObject<Item> SAMPLE_RAPIER = RegiSampleSword("sample_rapier", 3, -2.0F);
    public static final RegistryObject<Item> SAMPLE_DAGGER = RegiSampleSword("sample_dagger", 2, -1.8F);
    public static final RegistryObject<Item> SAMPLE_SCYTHE = RegiSampleSword("sample_scythe", 4, -2.8F);
    public static final RegistryObject<Item> SAMPLE_WHIP = RegiSampleSword("sample_whip", 3, -2.6F);
    public static final RegistryObject<Item> SAMPLE_CLAW = RegiSampleSword("sample_claw", 3, -1.0F);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    private static RegistryObject<Item> RegiSampleSword(String item_name, int Damage, float AttackSpeed){
         return ITEMS.register(item_name, () -> new SampleWeapon(Tiers.IRON, Damage, AttackSpeed, new Item.Properties()));
    }
}
