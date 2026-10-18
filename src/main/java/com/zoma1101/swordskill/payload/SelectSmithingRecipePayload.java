package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.smithing.SmithingForgeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/** 鍛冶台 GUI でレシピ一覧から選んだ武器をサーバーへ伝える。 */
public record SelectSmithingRecipePayload(int index) implements CustomPacketPayload {

    public static final Type<SelectSmithingRecipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "select_smithing_recipe"));

    public static final StreamCodec<FriendlyByteBuf, SelectSmithingRecipePayload> STREAM_CODEC = StreamCodec.of(
            SelectSmithingRecipePayload::encode, SelectSmithingRecipePayload::decode);

    private static void encode(FriendlyByteBuf buffer, SelectSmithingRecipePayload payload) {
        buffer.writeVarInt(payload.index());
    }

    private static SelectSmithingRecipePayload decode(FriendlyByteBuf buffer) {
        return new SelectSmithingRecipePayload(buffer.readVarInt());
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
                menu.selectRecipe(index);
            }
        });
    }
}
