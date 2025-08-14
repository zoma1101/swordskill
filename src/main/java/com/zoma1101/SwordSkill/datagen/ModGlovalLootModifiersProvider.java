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
        // 低難易度の宝箱 (ドロップ率0.3f)
        addSkillOrbToChest("village_mason", 0.3f);
        addSkillOrbToChest("village_weaponsmith", 0.3f);
        addSkillOrbToChest("spawn_bonus_chest", 0.6f); // ボーナスチェストは低め

        // 中難易度の宝箱 (ドロップ率0.6f)
        addSkillOrbToChest("simple_dungeon", 0.6f);
        addSkillOrbToChest("desert_pyramid", 0.6f);
        addSkillOrbToChest("igloo_chest", 0.6f);
        addSkillOrbToChest("shipwreck_treasure", 0.6f);
        addSkillOrbToChest("village_armorer", 0.6f);
        addSkillOrbToChest("abandoned_mineshaft", 0.5f); // 廃坑
        addSkillOrbToChest("pillager_outpost", 0.5f);   // ピリジャーの前哨基地
        addSkillOrbToChest("ruined_portal", 0.4f);      // 荒廃したポータル
        addSkillOrbToChest("underwater_ruin_big", 0.5f); // 大きな水中遺跡
        addSkillOrbToChest("underwater_ruin_small", 0.4f); // 小さな水中遺跡
        addSkillOrbToChest("nether_bridge", 0.6f);      // ネザー要塞

        // 高難易度の宝箱 (ドロップ率0.7f〜0.99f)
        addSkillOrbToChest("ancient_city", 0.7f);
        addSkillOrbToChest("ancient_city_ice_box", 0.7f); // 古代都市の冷気ボックス
        addSkillOrbToChest("end_city_treasure", 0.7f);    // エンドシティの宝
        addSkillOrbToChest("bastion_treasure", 0.7f);     // 砦の遺跡の宝庫 (高確率)
        addSkillOrbToChest("buried_treasure", 0.7f);      // 埋蔵された宝 (高確率)
        addSkillOrbToChest("stronghold_library", 0.7f);  // 要塞の図書館
        addSkillOrbToChest("stronghold_crossing", 0.6f); // 要塞の交差点
        addSkillOrbToChest("stronghold_corridor", 0.6f); // 要塞の通路
        addSkillOrbToChest("woodland_mansion", 0.99f);    // 森の洋館
    }

    private void addSkillOrbToChest(String location, float chance){
        add("unlock_item_in_chests_"+location, new AddItemModifier( new LootItemCondition[] {
                new LootTableIdCondition.Builder(parse("chests/"+location)).build(),
                LootItemRandomChanceCondition.randomChance(chance).build()}, SampleItemRegistry.UNLOCKITEM.get()
        ));
    }
}