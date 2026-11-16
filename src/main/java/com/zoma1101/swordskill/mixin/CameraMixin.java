package com.zoma1101.swordskill.mixin;

import com.zoma1101.swordskill.client.handler.GroundImpactHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 着地衝撃の地面揺れをカメラ位置の上下動として適用する。
 * 回転だけでは地面が動いて見えにくいため、カメラ自体を少しだけ上下させる。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    public abstract Vec3 getPosition();

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @Inject(method = "setup", at = @At("TAIL"))
    private void swordskill$applyGroundImpactShake(BlockGetter level, Entity entity, boolean detached,
            boolean thirdPersonReverse, float partialTick, CallbackInfo callbackInfo) {
        double offset = GroundImpactHandler.getCameraYOffset(partialTick);
        if (offset != 0.0D) {
            this.setPosition(this.getPosition().add(0.0D, offset, 0.0D));
        }
    }
}
