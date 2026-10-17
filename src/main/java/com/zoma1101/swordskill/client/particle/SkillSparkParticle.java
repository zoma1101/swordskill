package com.zoma1101.swordskill.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.zoma1101.swordskill.particle.SkillSparkParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;

/**
 * スキルエフェクト用の光る粒子。加算合成で描画し、ワールドの明るさに関係なく発光して見える。
 * 色はスキルのトレイル色から、サイズは {@link SkillSparkParticleOptions} の倍率から決まる。
 */
public class SkillSparkParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    /** 加算合成で発光させるための専用レンダータイプ。 */
    private static final ParticleRenderType GLOW_RENDER_TYPE = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return "swordskill:skill_spark_glow";
        }
    };

    private final float baseSize;

    public SkillSparkParticle(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, SkillSparkParticleOptions options) {
        super(level, x, y, z);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.friction = 0.88F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 8 + this.random.nextInt(8);
        this.baseSize = 0.16F * options.scale() * (0.7F + this.random.nextFloat() * 0.6F);
        this.quadSize = this.baseSize;
        this.setColor(brighten((options.color() >> 16) & 0xFF),
                brighten((options.color() >> 8) & 0xFF),
                brighten(options.color() & 0xFF));
        this.alpha = 1.0F;
    }

    private static float brighten(int channel) {
        float value = channel / 255.0F;
        return Mth.clamp(value + (1.0F - value) * 0.35F, 0.0F, 1.0F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return GLOW_RENDER_TYPE;
    }

    @Override
    public void tick() {
        super.tick();
        float life = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);
        this.alpha = 1.0F - life * life;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        return this.baseSize * (1.0F - 0.45F * life);
    }
}
