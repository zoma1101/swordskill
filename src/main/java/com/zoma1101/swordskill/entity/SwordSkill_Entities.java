package com.zoma1101.swordskill.entity;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class SwordSkill_Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SwordSkill.MOD_ID);

    public static final Supplier<EntityType<AttackEffectEntity>> ATTACK_EFFECT =
            ENTITY_TYPES.register("attack_effect",()->
                    EntityType.Builder.of(AttackEffectEntity::new, MobCategory.MISC)
                            .sized(1,1)
                            .build("attack_effect"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
