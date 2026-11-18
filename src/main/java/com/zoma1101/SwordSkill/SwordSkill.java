package com.zoma1101.swordskill;

import com.zoma1101.swordskill.client.ClientConfigScreen;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDataLoader;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.data.SkillDataLoader;
import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.entity.SwordSkill_Entities;
import com.zoma1101.swordskill.item.SampleItemRegistry;
import com.zoma1101.swordskill.particle.SkillParticles;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.payload.SyncDataSkillsPayload;
import com.zoma1101.swordskill.payload.SyncSmithingDefsPayload;
import com.zoma1101.swordskill.smithing.SmithingDataLoader;
import com.zoma1101.swordskill.smithing.SmithingDefinitions;
import com.zoma1101.swordskill.smithing.SmithingRegistry;
import com.zoma1101.swordskill.sounds.SwordSkillSounds;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import com.zoma1101.swordskill.compat.PacketDistributor;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;


@Mod(SwordSkill.MOD_ID)
public class SwordSkill {
    public static final String MOD_ID = "swordskill";
    public static final String PROTOCOL_VERSION = "14";

    public SwordSkill(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModContainer container = context.getContainer();
        SwordSkill_Entities.register(modEventBus);
        modEventBus.addListener(this::setup);
        EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ServerConfig.SPEC);
        if (FMLEnvironment.dist.isClient()) {
            // Mods 一覧の「Config」から設定画面を開けるようにする（クライアント専用クラス）
            ClientConfigScreen.register(container);
        }
        SampleItemRegistry.register(modEventBus);
        SmithingRegistry.register(modEventBus);
        SwordSkillAttribute.register(modEventBus);
        EffectRegistry.register(modEventBus);
        SkillParticles.register(modEventBus);
        SwordSkillSounds.register(modEventBus);
        EVENT_BUS.addListener(this::onAddReloadListener);
    }

    private void setup(FMLCommonSetupEvent event) {
        com.zoma1101.swordskill.network.NetworkHandler.init();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (SkillDataLoader.consumeSyncPending()) {
            var payloads = SyncDataSkillsPayload.createBatch(SwordSkillRegistry.getDataDrivenDefinitions());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                for (SyncDataSkillsPayload payload : payloads) {
                    PacketDistributor.sendToPlayer(player, payload);
                }
            }
        }
        if (SmithingDefinitions.consumeSyncPending() && SwordSkillFeatures.SMITHING) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SyncSmithingDefsPayload.sendTo(player);
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SkillExecutionManager.handleSkillExecution(server.getLevel(player.level().dimension()), player);
        }
        // 鍔迫り合いの固定と勝敗確定
        com.zoma1101.swordskill.server.handler.SkillClashManager.tick(server);
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        WeaponTypeDataLoader dataLoader = new WeaponTypeDataLoader();
        event.addListener(dataLoader);
        WeaponTypeDetector.initialize(dataLoader);
        event.addListener(new SkillDataLoader());
        if (SwordSkillFeatures.SMITHING) {
            event.addListener(new SmithingDataLoader.Materials());
            event.addListener(new SmithingDataLoader.Parts());
            event.addListener(new SmithingDataLoader.Patterns());
        }
    }
}