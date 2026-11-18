package com.zoma1101.swordskill.server.handler;
import net.minecraftforge.fml.common.Mod;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ServerModHandler {
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(SwordSkill_Entities.SKILL_MANNEQUIN.get(), SkillMannequinEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get())) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get());
        }
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.MAX_SP.get())) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.MAX_SP.get());
        }
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.SP_REGEN.get())) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.SP_REGEN.get());
        }
    }
}
