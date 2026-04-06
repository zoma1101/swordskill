package com.zoma1101.swordskill.event;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.capability.PlayerSkills;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ModCapabilityEvents {

    // ★修正: Mod Event Bus 用のクラス (static inner class)
    @Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PlayerSkills.class);
        }
    }

    // ★修正: Forge Event Bus 用のクラス (static inner class)
    @Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                if (!event.getObject().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).isPresent()) {
                    event.addCapability(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "player_skills"), new PlayerSkillsProvider());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerCloned(PlayerEvent.Clone event) {
            event.getOriginal().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        }
    }
}