package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {
    public AttackEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(AttackEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(AttackEffectEntity entity) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/" + NomalSkillTexture() + "/" + 1 + ".png");
    }
}
