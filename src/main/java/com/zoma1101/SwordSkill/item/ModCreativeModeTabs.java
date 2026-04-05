package com.zoma1101.swordskill.item;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SwordSkill.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SWORD_SKILL_TAB = CREATIVE_MODE_TABS.register("sword_skill_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(SampleItemRegistry.SAMPLE_KATANA.get()))
                    .title(Component.translatable("creativetab.sword_skill_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(SampleItemRegistry.UNLOCKITEM.get());
                        pOutput.accept(SampleItemRegistry.MARTIAL_ARTS_SCROLL.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_KATANA.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_GREAT_SWORD.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_RAPIER.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_DAGGER.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_SCYTHE.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_WHIP.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_CLAW.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_MACE.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_SWORD.get());
                        pOutput.accept(SampleItemRegistry.SAMPLE_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
