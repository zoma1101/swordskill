package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.compat.CustomPacketPayload;
import com.zoma1101.swordskill.compat.IPayloadContext;
import com.zoma1101.swordskill.compat.PayloadRegistrar;
import com.zoma1101.swordskill.compat.RegisterPayloadHandlersEvent;
import com.zoma1101.swordskill.compat.StreamCodec;
import com.zoma1101.swordskill.payload.PayloadRegistryHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * Forge 1.20.1 用のネットワークエントリポイント。
 * 1.21 側のペイロード定義はそのままに、SimpleChannel へ橋渡しする。
 */
public final class NetworkHandler {

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "main"),
            () -> SwordSkill.PROTOCOL_VERSION,
            NetworkHandler::isAccepted,
            NetworkHandler::isAccepted);

    private NetworkHandler() {
    }

    private static boolean isAccepted(String version) {
        return SwordSkill.PROTOCOL_VERSION.equals(version);
    }

    public static void init() {
        RegisterPayloadHandlersEvent event = new RegisterPayloadHandlersEvent();
        PayloadRegistryHandler.registerPayloads(event);
        int id = 0;
        for (PayloadRegistrar.Registration<?> registration : event.registrations()) {
            register(id++, registration);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends CustomPacketPayload> void register(
            int id, PayloadRegistrar.Registration<T> registration) {
        StreamCodec<FriendlyByteBuf, T> codec = (StreamCodec<FriendlyByteBuf, T>) (StreamCodec) registration.codec;
        CHANNEL.messageBuilder(registration.messageClass, id, registration.direction)
                .encoder((T msg, FriendlyByteBuf buf) -> codec.encode(buf, msg))
                .decoder(codec::decode)
                .consumerNetworkThread((T msg, Supplier<NetworkEvent.Context> ctx) ->
                        registration.handler.accept(msg, new IPayloadContext(ctx)))
                .add();
    }
}
