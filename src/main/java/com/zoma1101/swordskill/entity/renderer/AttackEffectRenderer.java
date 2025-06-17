package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.entity.renderer.skillRenderer.BlueEffectRenderer.renderEffect;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {
    private static final Logger LOGGER = LogManager.getLogger();

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

        try {
            renderEffect(entity.getRotationVector(), poseStack, bufferSource, packedLight, scale, rotationZ, entity.tickCount, SkillParticle);
        } catch (Exception e) {
            LOGGER.error("Render failed for AttackEffectEntity: {}", entity, e);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(AttackEffectEntity entity) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + NomalSkillTexture() + "/" + 1 + ".png");
    }
}
