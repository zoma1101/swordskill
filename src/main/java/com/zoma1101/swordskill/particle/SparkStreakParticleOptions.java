package com.zoma1101.swordskill.particle;

import com.mojang.serialization.Codec;
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
 * 速度方向に伸びる火花（光の筋）。色はスキルのトレイル色、サイズは倍率で指定する。
 * 丸い {@link SkillSparkParticleOptions} より速く鋭く飛び、重力で落ちて地面では跳ね返る。
 */
public record SparkStreakParticleOptions(int color, float scale) implements ParticleOptions {
    public static final MapCodec<SparkStreakParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.fieldOf("color").forGetter(SparkStreakParticleOptions::color),
                    Codec.FLOAT
                            .validate(SparkStreakParticleOptions::validateScale)
                            .fieldOf("scale")
                            .forGetter(SparkStreakParticleOptions::scale))
                    .apply(instance, SparkStreakParticleOptions::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SparkStreakParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SparkStreakParticleOptions::color,
                    ByteBufCodecs.FLOAT, SparkStreakParticleOptions::scale,
                    SparkStreakParticleOptions::new);

    public SparkStreakParticleOptions {
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
        return SkillParticles.SPARK_STREAK.get();
    }
}
