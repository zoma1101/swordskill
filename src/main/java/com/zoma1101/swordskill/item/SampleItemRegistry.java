package com.zoma1101.swordskill.item;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


public class SampleItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SwordSkill.MOD_ID);
    public static final DeferredItem<Item> SAMPLE_KATANA = RegiSampleSword("sample_katana", 3, -2.4F);
    public static final DeferredItem<Item> SAMPLE_BREAD = RegiSampleSword("sample_great_sword", 5, -2.9F);
    public static final DeferredItem<Item> SAMPLE_RAPIER = RegiSampleSword("sample_rapier", 3, -2.0F);
    public static final DeferredItem<Item> SAMPLE_DAGGER = RegiSampleSword("sample_dagger", 2, -1.8F);
    public static final DeferredItem<Item> SAMPLE_SCYTHE = RegiSampleSword("sample_scythe", 4, -2.8F);
    public static final DeferredItem<Item> SAMPLE_WHIP = RegiSampleSword("sample_whip", 3, -2.6F);
    public static final DeferredItem<Item> SAMPLE_CLAW = RegiSampleSword("sample_claw", 3, -1.0F);
    public static final DeferredItem<Item> UNLOCKITEM = ITEMS.registerItem("unlock_item",Item::new, new Item.Properties().rarity(Rarity.EPIC) );


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    private static DeferredItem<Item> RegiSampleSword(String item_name, int Damage, float AttackSpeed){
         return ITEMS.registerItem(item_name, (properties) -> new SampleWeapon(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON,Damage, AttackSpeed))));
    }
}
