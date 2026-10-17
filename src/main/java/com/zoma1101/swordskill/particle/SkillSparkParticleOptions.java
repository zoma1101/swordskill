package com.zoma1101.swordskill.particle;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * スキルエフェクト用の光る粒子。色はスキルのトレイル色、サイズは倍率で指定する。
 */
public record SkillSparkParticleOptions(int color, float scale) implements ParticleOptions {
    public static final MapCodec<SkillSparkParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    com.mojang.serialization.Codec.INT.fieldOf("color").forGetter(SkillSparkParticleOptions::color),
                    com.mojang.serialization.Codec.FLOAT
                            .validate(SkillSparkParticleOptions::validateScale)
                            .fieldOf("scale")
                            .forGetter(SkillSparkParticleOptions::scale))
                    .apply(instance, SkillSparkParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillSparkParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SkillSparkParticleOptions::color,
                    ByteBufCodecs.FLOAT, SkillSparkParticleOptions::scale,
                    SkillSparkParticleOptions::new);

    public SkillSparkParticleOptions {
        color &= 0xFFFFFF;
        scale = Mth.clamp(scale, 0.01F, 4.0F);
    }

    private static DataResult<Float> validateScale(float value) {
        return value >= 0.01F && value <= 4.0F
                ? DataResult.success(value)
                : DataResult.error(() -> "Value must be within range [0.01;4.0]: " + value);
    }

    @Override
    public ParticleType<?> getType() {
        return SkillParticles.SKILL_SPARK.get();
    }
}
