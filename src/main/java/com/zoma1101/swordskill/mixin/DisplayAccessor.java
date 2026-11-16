package com.zoma1101.swordskill.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 表示エンティティの明るさ上書き・変換を設定するためのアクセサ。
 * 地面ブロックの中に置いたブロック表示が真っ暗にならないよう明るさを渡し、
 * 元ブロックを隠すために少し拡大・沈み込みを設定する。
 */
@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setBrightnessOverride")
    void swordskill$setBrightnessOverride(Brightness brightness);

    @Invoker("setTransformation")
    void swordskill$setTransformation(Transformation transformation);
}
