package com.zoma1101.swordskill.sounds;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * この MOD が追加するサウンドイベント。
 */
public final class SwordSkillSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, SwordSkill.MOD_ID);

    /** スキルチャージ中にループ再生するサウンド。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = SOUND_EVENTS.register(
            "charge", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "charge")));

    /** チャージ開始時に再生するビーコン activate の大きめ版。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_ACTIVATE = SOUND_EVENTS.register(
            "charge_activate", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "charge_activate")));

    /** 4秒以上チャージしたときに流し続ける高音アンビエント（beacon.ambient の継ぎ目加工版）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE_AMBIENT_LOOP = SOUND_EVENTS.register(
            "charge_ambient_loop", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "charge_ambient_loop")));

    private SwordSkillSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
