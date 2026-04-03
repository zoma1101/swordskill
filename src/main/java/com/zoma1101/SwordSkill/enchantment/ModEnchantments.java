package com.zoma1101.swordskill.enchantment;
 
import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
 
public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = 
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, SwordSkill.MOD_ID);
 
    // 炎刃 (Enma-jin)
    public static final RegistryObject<Enchantment> ENMA_JIN = ENCHANTMENTS.register("enma_jin", 
            () -> new ElementEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    // 水刃 (Suijin)
    public static final RegistryObject<Enchantment> SUI_JIN = ENCHANTMENTS.register("suijin", 
            () -> new ElementEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    // 風刃 (Fujin)
    public static final RegistryObject<Enchantment> FU_JIN = ENCHANTMENTS.register("fujin", 
            () -> new ElementEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    // 光刃 (Kojin)
    public static final RegistryObject<Enchantment> KO_JIN = ENCHANTMENTS.register("ko_jin", 
            () -> new ElementEnchantment(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    // 闇刃 (Anjin)
    public static final RegistryObject<Enchantment> AN_JIN = ENCHANTMENTS.register("an_jin", 
            () -> new ElementEnchantment(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    // 魂刃 (Konjin)
    public static final RegistryObject<Enchantment> KON_JIN = ENCHANTMENTS.register("kon_jin", 
            () -> new ElementEnchantment(Enchantment.Rarity.VERY_RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));
 
    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
 
    private static class ElementEnchantment extends Enchantment {
        protected ElementEnchantment(Rarity rarity, EnchantmentCategory category, EquipmentSlot... slots) {
            super(rarity, category, slots);
        }
 
        @Override
        public int getMaxLevel() {
            return 1;
        }
 
        @Override
        public int getMinCost(int level) {
            return 15;
        }
 
        @Override
        public int getMaxCost(int level) {
            return super.getMinCost(level) + 30;
        }
 
        @Override
        public boolean isTreasureOnly() {
            return true;
        }
 
        @Override
        protected boolean checkCompatibility(Enchantment other) {
            // 他の属性刃とは競合するようにする (排他仕様)
            return super.checkCompatibility(other) && !(other instanceof ElementEnchantment);
        }
    }
}
