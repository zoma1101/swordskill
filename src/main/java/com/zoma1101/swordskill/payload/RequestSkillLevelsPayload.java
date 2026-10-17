package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** クライアントからスキルレベルの同期を要求するための空パケット。 */
public record RequestSkillLevelsPayload() implements CustomPacketPayload {

    public static final Type<RequestSkillLevelsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "request_skill_levels"));

    public static final StreamCodec<FriendlyByteBuf, RequestSkillLevelsPayload> STREAM_CODEC = StreamCodec
            .unit(new RequestSkillLevelsPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PacketDistributor.sendToPlayer(player, SkillLevelManager.buildSyncPayload(player));
            }
        });
    }
}
