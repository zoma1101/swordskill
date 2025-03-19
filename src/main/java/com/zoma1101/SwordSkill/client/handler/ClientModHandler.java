package com.zoma1101.SwordSkill.client.handler;


import com.zoma1101.SwordSkill.client.gui.SkillSlotDisplayOverlay;
import com.zoma1101.SwordSkill.client.screen.Keybindings;
import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.effects.SwordSkillAttribute;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
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
            event.register(Keybindings.INSTANCE.SwordSkill_HUD_Setting);
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

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get())) {
            event.add(EntityType.PLAYER, SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get());
        }
    }

}
