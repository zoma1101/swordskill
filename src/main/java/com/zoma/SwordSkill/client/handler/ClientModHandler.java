package com.zoma.SwordSkill.client.handler;


import com.zoma.SwordSkill.client.gui.SkillSlotDisplayOverlay;
import com.zoma.SwordSkill.client.screen.Keybindings;
import com.zoma.SwordSkill.main.SwordSkill;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {
        @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybindings.INSTANCE.SwordSkill_Selector_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_QuickSelect_Key);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_0);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_1);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_2);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_3);
            event.register(Keybindings.INSTANCE.SwordSkill_Use_Key_4);
    }

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SwordSkill.MOD_ID);

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud_skill_slot", SkillSlotDisplayOverlay.HUD_SKILL_SLOT);
    }


}
