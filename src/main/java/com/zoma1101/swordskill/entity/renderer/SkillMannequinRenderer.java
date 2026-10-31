package com.zoma1101.swordskill.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.client.handler.MannequinSkillAnimations;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 訓練用マネキンをプレイヤーと同じモデル・スキンで描画する。
 * 鍔迫り合い中は両腕を突き出したポーズに、スキル使用中は対応する
 * player_animation（無ければ簡易斬撃）を再生する。
 */
public class SkillMannequinRenderer
        extends HumanoidMobRenderer<SkillMannequinEntity, PlayerModel<SkillMannequinEntity>> {

    private static final ResourceLocation SKIN =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final MannequinModel mannequinModel;

    public SkillMannequinRenderer(EntityRendererProvider.Context context) {
        super(context, new MannequinModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.mannequinModel = (MannequinModel) this.getModel();
    }

    @Override
    public void render(@NotNull SkillMannequinEntity entity, float entityYaw, float partialTicks,
            @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        // setupAnim には部分 tick が渡らないため、描画前にモデルへ渡しておく
        this.mannequinModel.partialTick = partialTicks;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SkillMannequinEntity entity) {
        return SKIN;
    }

    private static final class MannequinModel extends PlayerModel<SkillMannequinEntity> {
        private float partialTick;

        private MannequinModel(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(@NotNull SkillMannequinEntity entity, float limbSwing, float limbSwingAmount,
                float ageInTicks, float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.isClashPose()) {
                // 剣を押し合う姿勢（腕を前に出して軽く開く）
                this.rightArm.xRot = -1.15F;
                this.rightArm.yRot = -0.35F;
                this.leftArm.xRot = -1.15F;
                this.leftArm.yRot = 0.35F;
                this.body.xRot = 0.08F;
                // 外層（袖・上着）も腕と胴に追従させる
                this.rightSleeve.copyFrom(this.rightArm);
                this.leftSleeve.copyFrom(this.leftArm);
                this.jacket.copyFrom(this.body);
            } else if (MannequinSkillAnimations.apply(entity, this, this.partialTick)) {
                // スキル定義のアニメーション（または簡易斬撃）を適用済み
            } else if (entity.isUsingSkill()) {
                // 常時スキル使用中: 剣を軽く構えた姿勢
                this.rightArm.xRot = -0.85F;
                this.rightArm.yRot = -0.25F;
                this.rightArm.zRot = 0.15F;
                this.rightSleeve.copyFrom(this.rightArm);
            }
        }
    }
}
