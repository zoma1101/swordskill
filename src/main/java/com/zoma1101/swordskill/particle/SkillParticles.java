package com.zoma1101.swordskill.particle;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SkillParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, SwordSkill.MOD_ID);

    /** スキルエフェクト用の光る粒子。 */
    public static final DeferredHolder<ParticleType<?>, ParticleType<SkillSparkParticleOptions>> SKILL_SPARK =
            PARTICLE_TYPES.register("skill_spark", SkillSparkParticleType::new);

    private SkillParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
