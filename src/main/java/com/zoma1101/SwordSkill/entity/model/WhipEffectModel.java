package com.zoma1101.SwordSkill.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class WhipEffectModel<T extends Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			fromNamespaceAndPath(SwordSkill.MOD_ID, "attack_effect_layer"), "SkillUtils");

	private final ModelPart root;

	public WhipEffectModel(ModelPart root) {
		this.root = root;
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshDefinition = new MeshDefinition();
		PartDefinition partDefinition = meshDefinition.getRoot();

		// 例として、1x1x1 の直方体を追加
		partDefinition.addOrReplaceChild("main", CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				PartPose.ZERO);

		return LayerDefinition.create(meshDefinition, 16, 16); // テクスチャサイズに合わせて変更
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		// エフェクト用の透明度を考慮（カスタム描画用）
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha * 0.8F);
	}
}