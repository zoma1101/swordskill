package com.zoma1101.swordskill;

import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDataLoader;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.effects.NoFallDamageEffect;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import com.zoma1101.swordskill.loot.ModLootModifiers;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;


@Mod(SwordSkill.MOD_ID)
public class SwordSkill {
    public static final String MOD_ID = "swordskill";
    public static final String PROTOCOL_VERSION = "1";

    public SwordSkill(IEventBus modEventBus, ModContainer container) {
        SwordSkill_Entities.register(modEventBus);
        modEventBus.addListener(this::setup);
        EVENT_BUS.register(this);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        SampleItemRegistry.register(modEventBus);
        SwordSkillAttribute.register(modEventBus);
        EffectRegistry.register(modEventBus);
        EVENT_BUS.addListener(this::onAddReloadListener);
        ModLootModifiers.register(modEventBus);
    }

    private void setup(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SkillExecutionManager.handleSkillExecution(event.getServer().getLevel(player.level().dimension()), player);
        }
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        WeaponTypeDataLoader dataLoader = new WeaponTypeDataLoader();
        event.addListener(dataLoader);
        WeaponTypeDetector.initialize(dataLoader);
    }
}