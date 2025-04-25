package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            fromNamespaceAndPath(SwordSkill.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, UseSkillPacket.class,
                UseSkillPacket::encode,
                UseSkillPacket::new,
                UseSkillPacket::handle);
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
        INSTANCE.registerMessage(id++, SkillLoadSlotPacket.class,
                SkillLoadSlotPacket::encode,
                SkillLoadSlotPacket::new,
                SkillLoadSlotPacket::handle);
        INSTANCE.registerMessage(id++, SkillUnlockPacket.class,
                SkillUnlockPacket::encode,
                SkillUnlockPacket::new,
                SkillUnlockPacket::handle);
        INSTANCE.registerMessage(id++, CheckSkillUnlockedPacket.class,
                CheckSkillUnlockedPacket::encode,
                CheckSkillUnlockedPacket::new,
                CheckSkillUnlockedPacket::handle);
        INSTANCE.registerMessage(id++, ConsumeUnlockItemPacket.class,
                ConsumeUnlockItemPacket::encode,
                ConsumeUnlockItemPacket::new,
                ConsumeUnlockItemPacket::handle);
        INSTANCE.registerMessage(
                id++, // 次の利用可能なID
                SkillSyncPacket.class,
                SkillSyncPacket::encode,
                SkillSyncPacket::new,
                SkillSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT) // サーバーからクライアントへ
        );
        INSTANCE.registerMessage(
                id++, // 次の利用可能なID
                SkillRequestPacket.class,
                SkillRequestPacket::encode,
                SkillRequestPacket::new,
                (msg, ctx) -> SkillRequestPacket.handle(ctx),
                Optional.of(NetworkDirection.PLAY_TO_SERVER) // クライアントからサーバーへ
        );
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}