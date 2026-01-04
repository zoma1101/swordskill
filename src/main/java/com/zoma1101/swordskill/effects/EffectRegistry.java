package com.zoma1101.swordskill.effects;
import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;



public class EffectRegistry {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, SwordSkill.MOD_ID);

    public static final Holder<MobEffect> NO_FALL_DAMAGE = EFFECTS.register("no_fall_damage", NoFallDamageEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}