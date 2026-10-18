package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.smithing.SmithingForgeMenu;
import com.zoma1101.swordskill.smithing.SmithingMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 鍛冶台 GUI の動作モードを切り替えるリクエスト。 */
public record SetSmithingModePayload(int modeId) implements CustomPacketPayload {

    public static final Type<SetSmithingModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "set_smithing_mode"));

    public static final StreamCodec<FriendlyByteBuf, SetSmithingModePayload> STREAM_CODEC = StreamCodec.of(
            SetSmithingModePayload::encode, SetSmithingModePayload::decode);

    private static void encode(FriendlyByteBuf buffer, SetSmithingModePayload payload) {
        buffer.writeVarInt(payload.modeId());
    }

    private static SetSmithingModePayload decode(FriendlyByteBuf buffer) {
        return new SetSmithingModePayload(buffer.readVarInt());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.containerMenu instanceof SmithingForgeMenu menu) {
                menu.setMode(SmithingMode.byId(modeId));
            }
        });
    }
}
