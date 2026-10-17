package com.zoma1101.swordskill.swordskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class SkillHitEffectRegistry {
    private static final Map<String, BiConsumer<DataSkillDefinition.EffectContext, LivingEntity>> EFFECTS = new HashMap<>();

    static {
        // DataDrivenSkill.applyHit/applyEffect が組み込み処理するeffect種をローダーの
        // パース許可リストに登録する (no-op ハンドラ)。
        register("low_hp_bonus", (context, target) -> {
        });
        register("magic_bonus", (context, target) -> {
        });
    }

    private SkillHitEffectRegistry() {
    }

    public static void register(String effectId, BiConsumer<DataSkillDefinition.EffectContext, LivingEntity> handler) {
        if (effectId == null || effectId.isBlank() || handler == null) {
            throw new IllegalArgumentException("A skill effect needs an id and handler");
        }
        if (EFFECTS.putIfAbsent(effectId, handler) != null) {
            throw new IllegalArgumentException("Skill effect is already registered: " + effectId);
        }
    }

    public static boolean isRegistered(String effectId) {
        return EFFECTS.containsKey(effectId);
    }

    static void apply(DataSkillDefinition.Effect effect, ServerPlayer player, LivingEntity target) {
        BiConsumer<DataSkillDefinition.EffectContext, LivingEntity> handler = EFFECTS.get(effect.type());
        if (handler == null) {
            throw new IllegalStateException("Unregistered skill hit effect: " + effect.type());
        }
        handler.accept(new DataSkillDefinition.EffectContext(player, effect), target);
    }
}
