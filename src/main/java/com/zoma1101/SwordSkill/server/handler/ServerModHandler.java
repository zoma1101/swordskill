package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerModHandler {
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get())) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get());
        }
    }
}
