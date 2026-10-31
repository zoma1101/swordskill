package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ServerModHandler {
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SwordSkill_Entities.SKILL_MANNEQUIN.get(), SkillMannequinEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE)) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE);
        }
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.MAX_SP)) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.MAX_SP);
        }
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.SP_REGEN)) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.SP_REGEN);
        }
    }
}
