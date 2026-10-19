package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 鍛冶システムのブロック・アイテム・メニュー登録。 */
public final class SmithingRegistry {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SwordSkill.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SwordSkill.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SwordSkill.MOD_ID);

    /** 鍛冶台。 */
    public static final DeferredBlock<Block> SMITHING_FORGE = BLOCKS.registerBlock("smithing_forge",
            SmithingForgeBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredItem<BlockItem> SMITHING_FORGE_ITEM = ITEMS.registerItem("smithing_forge",
            properties -> new BlockItem(SMITHING_FORGE.get(), properties));

    /** 溶解炉。鍛造武器をインゴットに戻す。 */
    public static final DeferredBlock<Block> MELTING_FORGE = BLOCKS.registerBlock("melting_forge",
            MeltingForgeBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredItem<BlockItem> MELTING_FORGE_ITEM = ITEMS.registerItem("melting_forge",
            properties -> new BlockItem(MELTING_FORGE.get(), properties));

    /** 鍛造武器。中身はコンポーネントで持ち、1 種類のアイテムで全パターンをまかなう。 */
    public static final DeferredItem<Item> FORGED_WEAPON = ITEMS.registerItem("forged_weapon",
            properties -> new ForgedWeaponItem(properties.rarity(Rarity.UNCOMMON).stacksTo(1)));

    /** 武器を溶かしてできたインゴット。武器記憶を保持し、再鍛造で引き継げる。 */
    public static final DeferredItem<Item> FORGED_INGOT = ITEMS.registerItem("forged_ingot",
            properties -> new ForgedIngotItem(properties.rarity(Rarity.UNCOMMON)));

    private static final DeferredHolder<MenuType<?>, MenuType<?>> SMITHING_FORGE_MENU_HOLDER =
            MENUS.register("smithing_forge",
                    () -> new MenuType<>(SmithingForgeMenu::new, FeatureFlags.VANILLA_SET));

    private static final DeferredHolder<MenuType<?>, MenuType<?>> MELTING_FORGE_MENU_HOLDER =
            MENUS.register("melting_forge",
                    () -> new MenuType<>(MeltingForgeMenu::new, FeatureFlags.VANILLA_SET));

    private SmithingRegistry() {
    }

    /** 鍛冶台のメニュー種別（型付き）。 */
    @SuppressWarnings("unchecked")
    public static MenuType<SmithingForgeMenu> forgeMenu() {
        return (MenuType<SmithingForgeMenu>) SMITHING_FORGE_MENU_HOLDER.get();
    }

    /** 溶解炉のメニュー種別（型付き）。 */
    @SuppressWarnings("unchecked")
    public static MenuType<MeltingForgeMenu> meltingMenu() {
        return (MenuType<MeltingForgeMenu>) MELTING_FORGE_MENU_HOLDER.get();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        MENUS.register(eventBus);
    }
}
