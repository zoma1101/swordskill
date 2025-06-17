package com.zoma1101.swordskill.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class NoFallDamageEffect extends MobEffect {

    protected NoFallDamageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x808080);
    }


    // 落下ダメージを無効化するイベントリスナー
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(EffectRegistry.NO_FALL_DAMAGE)) {
            event.setCanceled(true);
        }
    }
}