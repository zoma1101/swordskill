package com.zoma1101.swordskill.mixin;

import com.zoma1101.swordskill.client.renderer.layer.SwordTrailManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getAttackAnim", at = @At("HEAD"), cancellable = true)
    private void swordskill$cancelSwingDuringSkill(float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof LocalPlayer player) {
            if (SwordTrailManager.getSession(player.getUUID()).isActiveAnimation()) {
                cir.setReturnValue(0.0f);
            }
        }
    }
}
