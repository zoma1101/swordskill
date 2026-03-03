package com.zoma1101.swordskill.event;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAttributes {

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SwordSkillAttribute.MAX_SP.get());
        event.add(EntityType.PLAYER, SwordSkillAttribute.SP_REGEN.get());
    }
}
