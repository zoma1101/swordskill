package com.zoma1101.swordskill.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import com.zoma1101.swordskill.client.handler.WeaponTrailHandler;
import net.minecraft.client.Minecraft;
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

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD"))
    private void swordskill$beginChargeTint(LivingEntity entity, ItemStack itemStack,
            ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
            MultiBufferSource bufferSource, Level level, int packedLight, int packedOverlay, int seed,
            CallbackInfo callbackInfo) {
        if (ClientForgeHandler.chargingItemColor < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) {
            return;
        }
        if (leftHand) {
            return;
        }
        if (itemStack != mc.player.getMainHandItem()) {
            return;
        }
        int color = ClientForgeHandler.chargingItemColor;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r * 1.3F, g * 1.3F, b * 1.3F, 1.0F);
    }

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("TAIL"))
    private void swordskill$endChargeTint(LivingEntity entity, ItemStack itemStack,
            ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack,
            MultiBufferSource bufferSource, Level level, int packedLight, int packedOverlay, int seed,
            CallbackInfo callbackInfo) {
        if (ClientForgeHandler.chargingItemColor < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) {
            return;
        }
        if (leftHand) {
            return;
        }
        if (itemStack != mc.player.getMainHandItem()) {
            return;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
