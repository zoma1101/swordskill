package com.zoma1101.swordskill.effects;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

@Mod.EventBusSubscriber(modid = com.zoma1101.swordskill.SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NoFallDamageEffect extends MobEffect {

    protected NoFallDamageEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x808080);
    }


    // 落下ダメージを無効化するイベントリスナー
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(EffectRegistry.NO_FALL_DAMAGE.get())) {
            event.setCanceled(true);
        }
    }
}