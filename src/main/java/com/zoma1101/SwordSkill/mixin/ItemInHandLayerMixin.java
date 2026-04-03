package com.zoma1101.swordskill.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailManager;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    
    @Shadow
    @Final
    private ItemInHandRenderer itemInHandRenderer;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void swordskill$renderCustomThirdPersonItem(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, HumanoidArm arm, PoseStack poseStack, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (!stack.isEmpty() && entity instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            SwordTrailLayer.TrailSession session = SwordTrailManager.getSession(player.getUUID());
            if (session != null && session.active && session.animStartMs >= 0) {
                float t = (System.currentTimeMillis() - session.animStartMs) / 1000.0f;
                if (t <= session.animationLength) {
                    boolean isMainArm = arm == player.getMainArm();

                    if (isMainArm && stack == player.getMainHandItem()) {
                        org.joml.Vector3f itemRotDeg = session.itemRotTrack != null ? session.itemRotTrack.evaluate(t) : new org.joml.Vector3f();
                        org.joml.Vector3f itemPosPx = session.itemPosTrack != null ? session.itemPosTrack.evaluate(t) : new org.joml.Vector3f();

                        poseStack.pushPose();
                        
                        // 1. バニラの親モデルを使って、手首の位置へPoseStackを移動させる
                        Object layerObj = this;
                        if (layerObj instanceof net.minecraft.client.renderer.entity.layers.RenderLayer) {
                            Object modelObj = ((net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?>) layerObj).getParentModel();
                            if (modelObj instanceof ArmedModel armedModel) {
                                armedModel.translateToHand(arm, poseStack);
                            }
                        }
                        
                        // 2. アニメーションの "rightItem" ボーンのオフセットと回転を当てる
                        poseStack.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);
                        poseStack.mulPose(Axis.XP.rotationDegrees(-itemRotDeg.x - 90.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(-itemRotDeg.y + 180.0F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(itemRotDeg.z));

                        boolean flag = arm == HumanoidArm.LEFT;
                        // 3. バニラの武器を持つ手首からの微調整オフセットをそのまま適用
                        poseStack.translate((float)(flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);

                        // 4. バニラのRendererへ投げて描画
                        this.itemInHandRenderer.renderItem(entity, stack, displayContext, flag, poseStack, bufferSource, light);
                        
                        poseStack.popPose();
                        ci.cancel(); // 元のバニラ描画をキャンセル
                    }
                }
            }
        }
    }
}
