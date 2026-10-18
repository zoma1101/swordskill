package com.zoma1101.swordskill.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class SparkStreakParticleType extends ParticleType<SparkStreakParticleOptions> {
    public SparkStreakParticleType() {
        super(false);
    }

    @Override
    public MapCodec<SparkStreakParticleOptions> codec() {
        return SparkStreakParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, SparkStreakParticleOptions> streamCodec() {
        return SparkStreakParticleOptions.STREAM_CODEC;
    }
}
