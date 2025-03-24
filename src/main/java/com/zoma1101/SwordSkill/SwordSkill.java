package com.zoma1101.SwordSkill;

import com.zoma1101.SwordSkill.config.ClientConfig;
import com.zoma1101.SwordSkill.config.ServerConfig;
import com.zoma1101.SwordSkill.data.WeaponTypeDataLoader;
import com.zoma1101.SwordSkill.data.WeaponTypeDetector;
import com.zoma1101.SwordSkill.effects.EffectRegistry;
import com.zoma1101.SwordSkill.effects.SwordSkillAttribute;
import com.zoma1101.SwordSkill.entity.SwordSkill_Entities;
import com.zoma1101.SwordSkill.item.SampleItemRegistry;
import com.zoma1101.SwordSkill.network.NetworkHandler;
import com.zoma1101.SwordSkill.server.handler.SkillExecutionManager;
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
    }

    private void setup(FMLCommonSetupEvent event) {
        NetworkHandler.register(event);
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