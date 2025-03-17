package com.zoma1101.SwordSkill.effects;
import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EffectRegistry {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SwordSkill.MOD_ID);

    public static final RegistryObject<MobEffect> NO_FALL_DAMAGE = EFFECTS.register("no_fall_damage", NoFallDamageEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}