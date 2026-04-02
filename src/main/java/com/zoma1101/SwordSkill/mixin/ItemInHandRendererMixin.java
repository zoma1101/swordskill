package com.zoma1101.swordskill.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 一人称の武器アニメーション同期処理は現在バニラの基本動作に戻すため、一時的に無効化しています。
// 詳細は FIRST_PERSON_ANIMATION_NOTES.md を参照してください。
/*
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void swordskill$applyFirstPersonAnimation(
            net.minecraft.world.entity.LivingEntity entity, ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
            boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource,
            int light, CallbackInfo ci) {
        if (displayContext == net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && entity instanceof AbstractClientPlayer player) {
            // 一人称の武器にアニメーションを適用
            // com.zoma1101.swordskill.AnimationUtils.applyFirstPersonAnimation(player, poseStack, net.minecraft.client.Minecraft.getInstance().getPartialTick());
        }
    }
}
*/
