package com.zoma1101.swordskill.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.client.handler.WeaponTrailHandler;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD"))
    private void swordskill$captureHeldItemPose(LivingEntity entity, ItemStack itemStack,
            ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
            MultiBufferSource bufferSource, Level level, int packedLight, int packedOverlay, int seed,
            CallbackInfo callbackInfo) {
        WeaponTrailHandler.captureRenderedItem(entity, itemStack, displayContext, leftHand, poseStack, level, seed);
    }
}
