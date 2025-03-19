package com.zoma1101.SwordSkill.effects;

import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SwordSkillAttribute {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, SwordSkill.MOD_ID);
    public static final RegistryObject<Attribute> COOLDOWN_ATTRIBUTE = ATTRIBUTES.register("cool_down_cut", () ->
            new RangedAttribute(SwordSkill.MOD_ID + ".player.cool_down_cut", 1.0D, 1.0D, 2048.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

}