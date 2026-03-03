package com.zoma1101.swordskill.effects;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SwordSkillAttribute {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE,
            SwordSkill.MOD_ID);
    public static final Holder<Attribute> COOLDOWN_ATTRIBUTE = ATTRIBUTES.register("cool_down_cut",
            () -> new RangedAttribute(SwordSkill.MOD_ID + ".player.cool_down_cut", 1.0D, 1.0D, 2048.0D)
                    .setSyncable(true));
    public static final Holder<Attribute> MAX_SP = ATTRIBUTES.register("max_sp",
            () -> new RangedAttribute(SwordSkill.MOD_ID + ".player.max_sp", 100.0D, 0.0D, 10000.0D).setSyncable(true));
    public static final Holder<Attribute> SP_REGEN = ATTRIBUTES.register("sp_regen",
            () -> new RangedAttribute(SwordSkill.MOD_ID + ".player.sp_regen", 0.1D, 0.0D, 100.0D).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }

}