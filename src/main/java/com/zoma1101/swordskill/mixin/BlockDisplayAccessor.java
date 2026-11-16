package com.zoma1101.swordskill.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * ブロック表示エンティティへ任意のブロック状態を設定するためのアクセサ。
 * 着地衝撃で地面ブロックを一時的に揺らす演出に使用する。
 */
@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {
    @Invoker("setBlockState")
    void swordskill$setBlockState(BlockState state);
}
