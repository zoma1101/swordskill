package com.zoma1101.swordskill;

import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDataLoader;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import com.zoma1101.swordskill.loot.ModLootModifiers;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


@Mod(SwordSkill.MOD_ID)
public class SwordSkill {
    public static final String MOD_ID = "swordskill";

    public SwordSkill(FMLJavaModLoadingContext ctx) {
        IEventBus modEventBus = ctx.getModEventBus();

        SwordSkill_Entities.register(modEventBus);
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ctx.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        SampleItemRegistry.register(modEventBus);
        SwordSkillAttribute.register(modEventBus);
        EffectRegistry.register(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        ModLootModifiers.register(modEventBus);
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                SkillExecutionManager.handleSkillExecution(event.getServer().getLevel(player.level().dimension()), player);
            }
        }
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        WeaponTypeDataLoader dataLoader = new WeaponTypeDataLoader();
        event.addListener(dataLoader);
        WeaponTypeDetector.initialize(dataLoader);
    }
}