package com.zoma1101.swordskill.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SkillSparkParticleType extends ParticleType<SkillSparkParticleOptions> {
    public SkillSparkParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SkillSparkParticleOptions> codec() {
        return SkillSparkParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SkillSparkParticleOptions> streamCodec() {
        return SkillSparkParticleOptions.STREAM_CODEC;
    }
}
