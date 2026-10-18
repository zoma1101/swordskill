package com.zoma1101.swordskill.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.swordskill.particle.SparkStreakParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 速度方向に伸びた光の筋として描画する火花。色はスキルのトレイル色と同じで、
 * 先端だけ白熱させて鋭く見せる。重力で落ち、地面では跳ね返る。
 * 丸い {@link SkillSparkParticle} より速く飛ぶ、火花の「流れ」を担当する。
 */
public class SparkStreakParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final float HOT_TIP = 0.6F;

    private final float baseSize;
    private final float streakLength;
    private final Vector3f headColor;
    private final Vector3f tailColor;

    public SparkStreakParticle(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, SparkStreakParticleOptions options) {
        super(level, x, y, z);
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.friction = 0.945F;
        this.gravity = 1.0F;
        this.hasPhysics = true;
        this.lifetime = 5 + this.random.nextInt(7);
        this.baseSize = 0.090F * options.scale() * (0.75F + this.random.nextFloat() * 0.6F);
        this.quadSize = this.baseSize;

        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
        this.streakLength = (float) Mth.clamp(speed * 3.4D, 2.6D, 12.0D);

        float red = brighten((options.color() >> 16) & 0xFF);
        float green = brighten((options.color() >> 8) & 0xFF);
        float blue = brighten(options.color() & 0xFF);
        this.tailColor = new Vector3f(red, green, blue);
        this.headColor = new Vector3f(
                Mth.lerp(HOT_TIP, red, 1.0F),
                Mth.lerp(HOT_TIP, green, 1.0F),
                Mth.lerp(HOT_TIP, blue, 1.0F));
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
        return SkillParticleRenderTypes.GLOW;
    }

    @Override
    public void tick() {
        super.tick();
        float life = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);
        this.alpha = 1.0F - life * life * life;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTick) {
        float extent = this.baseSize * this.streakLength + 0.25F;
        return new AABB(this.x - extent, this.y - extent, this.z - extent,
                this.x + extent, this.y + extent, this.z + extent);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vector3f cameraForward = new Vector3f(0.0F, 0.0F, 1.0F).rotate(camera.rotation());
        Vector3f cameraUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.rotation());

        // 火花が進む向き。速度が無ければ直前の移動量で代用する。
        Vector3f velocity = new Vector3f((float) this.xd, (float) this.yd, (float) this.zd);
        if (velocity.lengthSquared() < 1.0E-8F) {
            velocity.set((float) (this.x - this.xo), (float) (this.y - this.yo), (float) (this.z - this.zo));
        }
        float speed = velocity.length();
        if (speed < 1.0E-5F) {
            velocity.set(cameraUp);
        } else {
            velocity.div(speed);
        }

        // 画面上での火花の向き。視線と平行に近いときは縦に伸ばす。
        Vector3f along = new Vector3f(velocity).fma(-velocity.dot(cameraForward), cameraForward);
        float planar = along.length();
        if (planar < 1.0E-4F) {
            along.set(cameraUp);
            planar = 1.0F;
        } else {
            along.div(planar);
        }
        Vector3f side = new Vector3f(along).cross(cameraForward).normalize();

        float halfWidth = this.baseSize * 0.5F;
        float stretch = Math.max(planar, 0.35F);
        float halfLength = this.baseSize * this.streakLength * 0.5F * stretch;

        Vec3 cameraPosition = camera.getPosition();
        Vector3f center = new Vector3f(
                (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cameraPosition.x),
                (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cameraPosition.y),
                (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cameraPosition.z));
        Vector3f longOffset = new Vector3f(along).mul(halfLength);
        Vector3f sideOffset = new Vector3f(side).mul(halfWidth);

        Vector3f tailLeft = new Vector3f(center).sub(longOffset).sub(sideOffset);
        Vector3f tailRight = new Vector3f(center).sub(longOffset).add(sideOffset);
        Vector3f headRight = new Vector3f(center).add(longOffset).add(sideOffset);
        Vector3f headLeft = new Vector3f(center).add(longOffset).sub(sideOffset);

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        addVertex(buffer, tailLeft, u0, v0, this.tailColor, this.alpha * 0.8F);
        addVertex(buffer, tailRight, u0, v1, this.tailColor, this.alpha * 0.8F);
        addVertex(buffer, headRight, u1, v1, this.headColor, this.alpha);
        addVertex(buffer, headLeft, u1, v0, this.headColor, this.alpha);
    }

    private static void addVertex(VertexConsumer buffer, Vector3f position, float u, float v,
            Vector3f color, float alpha) {
        buffer.addVertex(position.x, position.y, position.z)
                .setUv(u, v)
                .setColor(color.x, color.y, color.z, alpha)
                .setLight(FULL_BRIGHT);
    }
}
