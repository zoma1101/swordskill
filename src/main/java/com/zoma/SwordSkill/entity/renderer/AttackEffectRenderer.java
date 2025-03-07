package com.zoma.SwordSkill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma.SwordSkill.entity.custom.AttackEffectEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.entity.renderer.skillRenderer.BlueEffectRenderer.renderEffect;

public class AttackEffectRenderer extends EntityRenderer<AttackEffectEntity> {

    public AttackEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F; // 影なし
    }

    @Override
    public void render(AttackEffectEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();


        // エンティティの最新の Rotation を取得
        Vec3 rotation = entity.getRotation();
        Vector3f radius = entity.getEffectRadius();
        Vector3f scale = radius;
                //new Vec3(radius * 2.4, radius, radius * 0.8); // Y軸方向は少し縮める
        String SkillParticle = entity.getSkillParticle();

        // 中心位置 (オフセット調整)
        Vec3 center = new Vec3(0, 0, 0);

        // エフェクトを描画
        renderEffect(poseStack, bufferSource, packedLight, scale, rotation, center, entity.tickCount,SkillParticle);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, 15728880);
    }

    @Override
    public ResourceLocation getTextureLocation(AttackEffectEntity entity) {
        return null;
    }
}
