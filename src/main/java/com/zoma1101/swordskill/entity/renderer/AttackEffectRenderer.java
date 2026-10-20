package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {
    private static final RenderType SLASH_TRAIL = RenderType.create(
            "swordskill_slash_trail",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    public AttackEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AttackEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTicks;
        float sweepProgress = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        Vector3f size = entity.getEffectRadius();

        // Draw a persistent, emissive crescent behind the moving slash head.
        renderSlashTrail(entity, poseStack, bufferSource, size, age, sweepProgress);
    }

    private static void renderSlashTrail(AttackEffectEntity entity, PoseStack poseStack,
            MultiBufferSource buffers, Vector3f size, float age, float sweepProgress) {
        float fade = 1.0F - Mth.clamp((age - 7.0F) / 7.0F, 0.0F, 1.0F);
        if (fade <= 0.01F || sweepProgress <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        // Keep the trail's facing aligned with the attack-effect entity and its travel vector.
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getRotation()));
        if (!entity.isFlyingSlash()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRenderPitch()));
        }

        VertexConsumer vertices = buffers.getBuffer(SLASH_TRAIL);
        PoseStack.Pose pose = poseStack.last();
        int skillColor = entity.getSkillColor();
        float red = ((skillColor >> 16) & 0xFF) / 255.0F;
        float green = ((skillColor >> 8) & 0xFF) / 255.0F;
        float blue = (skillColor & 0xFF) / 255.0F;
        // 芯は白寄りにして発光感を出す
        float coreRed = red + (1.0F - red) * 0.75F;
        float coreGreen = green + (1.0F - green) * 0.75F;
        float coreBlue = blue + (1.0F - blue) * 0.75F;
        float radiusX = Math.max(0.35F, size.x * 0.5F);
        float radiusY = Math.max(0.25F, size.y * 0.5F);
        float thickness = Math.max(0.06F, Math.min(size.z * 0.035F, 0.2F));
        float z = Math.max(0.02F, size.z * 0.02F);
        int segments = Math.max(2, (int) (36 * sweepProgress));
        float endAngle = Mth.lerp(sweepProgress, -75.0F, 75.0F);

        for (int i = 0; i < segments; i++) {
            float from = i / (float) segments;
            float to = (i + 1) / (float) segments;
            float fromAngle = Mth.lerp(from, -75.0F, endAngle);
            float toAngle = Mth.lerp(to, -75.0F, endAngle);
            float intensity = fade * (0.35F + 0.65F * to);
            Vec3[] outer = arcStrip(radiusX, radiusY, thickness, z, fromAngle, toAngle);
            drawQuad(vertices, pose, outer, red, green, blue, intensity * 0.18F);
            Vec3[] core = arcStrip(radiusX, radiusY, thickness * 0.28F, z - 0.005F,
                    fromAngle, toAngle);
            drawQuad(vertices, pose, core, coreRed, coreGreen, coreBlue, intensity * 0.9F);
        }
        poseStack.popPose();
    }

    private static Vec3[] arcStrip(float radiusX, float radiusY, float halfWidth, float z,
            float fromDegrees, float toDegrees) {
        double from = Math.toRadians(fromDegrees);
        double to = Math.toRadians(toDegrees);
        // Build the crescent in the entity's local XZ plane. Its tip (+Z) then follows
        // the same yaw/pitch as the attack-effect entity instead of always pointing up (+Y).
        Vec3 fromCenter = new Vec3(radiusX * Math.sin(from), z, radiusY * Math.cos(from));
        Vec3 toCenter = new Vec3(radiusX * Math.sin(to), z, radiusY * Math.cos(to));
        Vec3 fromNormal = new Vec3(-Math.sin(from), 0, Math.cos(from)).scale(halfWidth);
        Vec3 toNormal = new Vec3(-Math.sin(to), 0, Math.cos(to)).scale(halfWidth);
        return new Vec3[] { fromCenter.add(fromNormal), fromCenter.subtract(fromNormal),
                toCenter.subtract(toNormal), toCenter.add(toNormal) };
    }

    private static void drawQuad(VertexConsumer vertices, PoseStack.Pose pose, Vec3[] corners,
            float red, float green, float blue, float alpha) {
        for (Vec3 corner : corners) {
            vertices.addVertex(pose, (float) corner.x, (float) corner.y, (float) corner.z)
                    .setColor(red, green, blue, alpha);
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(AttackEffectEntity entity) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + NomalSkillTexture() + "/" + 1 + ".png");
    }
}
