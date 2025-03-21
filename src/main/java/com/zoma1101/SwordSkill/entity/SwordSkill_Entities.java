package com.zoma1101.SwordSkill.entity;

import com.zoma1101.SwordSkill.entity.custom.AttackEffectEntity;
import com.zoma1101.SwordSkill.SwordSkill;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SwordSkill_Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SwordSkill.MOD_ID);

    public static final RegistryObject<EntityType<AttackEffectEntity>> ATTACK_EFFECT =
            ENTITY_TYPES.register("attack_effect",()->
                    EntityType.Builder.of(AttackEffectEntity::new, MobCategory.MISC)
                            .sized(1,1)
                            .build("attack_effect"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }


}
