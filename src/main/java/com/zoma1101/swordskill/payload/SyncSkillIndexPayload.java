package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncSkillIndexPayload(int selectedSkillIndex) implements CustomPacketPayload {
    public static final Type<SyncSkillIndexPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_skill_index"));

    // 2. StreamCodec (int型1つ)
    public static final StreamCodec<FriendlyByteBuf, SyncSkillIndexPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncSkillIndexPayload::selectedSkillIndex,
            SyncSkillIndexPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncSkillIndexPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientForgeHandler.setSelectedSkillIndex(msg.selectedSkillIndex());
        });
    }
}