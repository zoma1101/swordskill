package com.zoma1101.SwordSkill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.entity.custom.WhipAttackEffect;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalWhipTexture;

public class WhipEffectRenderer extends EntityRenderer<WhipAttackEffect> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, NomalWhipTexture());
    public WhipEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }


    @Override
    public void render(WhipAttackEffect entity, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose(); // 変換を保存

        float scale = entity.getSize();
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normalMatrix = poseStack.last().normal();
        // スケール適用
        matrix.scale(scale, scale, scale);
        Vector3f cameraDirection = new Vector3f(0, 1, 0);

        {
            for (int i=0; i<20 ; i++) {
                Vec3 Pos = entity.getmovePos(i);
                System.out.println("WhipEffectは"+Pos);
                matrix.translate(Pos.toVector3f());


                VertexConsumer builder = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
                builder.vertex(matrix, -0.5f, 0f, -0.5f)
                        .color(1, 1, 1, 1f)
                        .uv(0f, 1f)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                        .endVertex();

                builder.vertex(matrix, 0.5f, 0f, -0.5f)
                        .color(1, 1, 1, 1f)
                        .uv(1f, 1f)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                        .endVertex();

                builder.vertex(matrix, 0.5f, 0f, 0.5f)
                        .color(1, 1, 1, 1f)
                        .uv(1f, 0f)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                        .endVertex();

                builder.vertex(matrix, -0.5f, 0f, 0.5f)
                        .color(1, 1, 1, 1f)
                        .uv(0f, 0f)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, cameraDirection.x, cameraDirection.y, cameraDirection.z)
                        .endVertex();
                matrix.translate(Pos.toVector3f().mul(-1));
            }
        }
        poseStack.popPose(); // 変換を元に戻す
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull WhipAttackEffect entity) {
        return TEXTURE;
    }
}