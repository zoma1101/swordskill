package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.entity.renderer.skillRenderer.BlueEffectRenderer.renderEffect;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {

    public AttackEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AttackEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // エンティティの最新の Rotation を取得
        float rotationZ = entity.getRotation();

        Vector3f scale = entity.getEffectRadius();

        String SkillParticle = entity.getSkillParticle();

        renderEffect(entity.getRotationVector(),poseStack, bufferSource, packedLight, scale, rotationZ, entity.tickCount,SkillParticle);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackEffectEntity entity) {
        return null;
    }
}
