package com.zoma1101.swordskill.datagen;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import com.zoma1101.swordskill.loot.AddItemModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

import static net.minecraft.resources.ResourceLocation.parse;

public class ModGlovalLootModifiersProvider extends GlobalLootModifierProvider {
    public ModGlovalLootModifiersProvider(PackOutput output) {
        super(output, SwordSkill.MOD_ID);
    }

    @Override
    protected void start() {
        UnlockItem_Chests("village_mason", 0.1f);
        UnlockItem_Chests("village_weaponsmith", 0.1f);

        UnlockItem_Chests("simple_dungeon", 0.3f);
        UnlockItem_Chests("desert_pyramid", 0.3f);
        UnlockItem_Chests("igloo_chest", 0.3f);
        UnlockItem_Chests("shipwreck_treasure", 0.3f);
        UnlockItem_Chests("village_armorer", 0.3f);
        UnlockItem_Chests("shipwreck_treasure", 0.3f);
        UnlockItem_Chests("shipwreck_treasure", 0.3f);
        UnlockItem_Chests("simple_dungeon", 0.3f);
        UnlockItem_Chests("simple_dungeon", 0.3f);

        UnlockItem_Chests("ancient_city", 0.7f);
        UnlockItem_Chests("ancient_city_ice_box", 0.7f);
        UnlockItem_Chests("bastion_treasure", 0.7f);
        UnlockItem_Chests("buried_treasure", 0.7f);
        UnlockItem_Chests("end_city_treasure", 0.7f);
    }


    private void UnlockItem_Chests(String location, float chance){
        add("unlock_item_in_chests_"+location, new AddItemModifier( new LootItemCondition[] {
                new LootTableIdCondition.Builder(parse("chests/"+location)).build(),
                new LootTableIdCondition.Builder(parse("chests/"+location)).build(),
                LootItemRandomChanceCondition.randomChance(chance).build()}, SampleItemRegistry.UNLOCKITEM.get()
        ));
    }
}