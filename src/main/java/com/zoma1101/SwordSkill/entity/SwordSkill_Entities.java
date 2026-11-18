package com.zoma1101.swordskill.entity;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;


public class SwordSkill_Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SwordSkill.MOD_ID);

    public static final Supplier<EntityType<AttackEffectEntity>> ATTACK_EFFECT =
            ENTITY_TYPES.register("attack_effect",()->
                    EntityType.Builder.of(AttackEffectEntity::new, MobCategory.MISC)
                            .sized(1,1)
                            .build("attack_effect"));

    /** 鍔迫り合いの実装テスト用マネキン（プレイヤーと同じ体格）。 */
    public static final Supplier<EntityType<SkillMannequinEntity>> SKILL_MANNEQUIN =
            ENTITY_TYPES.register("skill_mannequin", () ->
                    EntityType.Builder.of(SkillMannequinEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build("skill_mannequin"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
