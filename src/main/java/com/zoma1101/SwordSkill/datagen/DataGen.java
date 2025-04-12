package com.zoma1101.swordskill.datagen;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event){
        DataGenerator generator = event.getGenerator();
        PackOutput packoutput = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new ModGlovalLootModifiersProvider(packoutput));
    }
}