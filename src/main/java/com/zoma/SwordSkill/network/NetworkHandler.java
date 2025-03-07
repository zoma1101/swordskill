package com.zoma.SwordSkill.network;

import com.zoma.SwordSkill.main.SwordSkill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SwordSkill.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register(FMLCommonSetupEvent event) {
        INSTANCE.registerMessage(id++, UseSkillPacket.class,
                UseSkillPacket::encode,
                UseSkillPacket::new,
                UseSkillPacket::handle);
        // SkillRequestPacket を登録
        INSTANCE.registerMessage(id++, SkillRequestPacket.class,
                SkillRequestPacket::encode,
                SkillRequestPacket::new,
                SkillRequestPacket::handle);
        INSTANCE.registerMessage(id++, SkillSelectionPacket.class,
                SkillSelectionPacket::encode,
                SkillSelectionPacket::new,
                SkillSelectionPacket::handle);
        INSTANCE.registerMessage(id++, SkillSlotSelectionPacket.class,
                SkillSlotSelectionPacket::encode,
                SkillSlotSelectionPacket::new,
                SkillSlotSelectionPacket::handle);
        INSTANCE.registerMessage(id++, SkillSlotInfoPacket.class,
                SkillSlotInfoPacket::encode,
                SkillSlotInfoPacket::new,
                SkillSlotInfoPacket::handle);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}