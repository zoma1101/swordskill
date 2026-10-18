package com.zoma1101.swordskill.payload;

import com.google.gson.JsonParser;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.smithing.SmithingDefinitions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * サーバーの鍛冶定義（素材・部品・レシピ）をクライアントへ配る。
 *
 * <p>レシピ一覧の表示や部品名の解決に必要なため、ログイン時と
 * データパック再読み込み時に送る。</p>
 */
public record SyncSmithingDefsPayload(String json) implements CustomPacketPayload {

    /** 1 パケットで送れる上限（文字数）。 */
    private static final int MAX_LENGTH = 1_048_576;

    public static final Type<SyncSmithingDefsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_smithing_defs"));

    public static final StreamCodec<FriendlyByteBuf, SyncSmithingDefsPayload> STREAM_CODEC = StreamCodec.of(
            SyncSmithingDefsPayload::encode, SyncSmithingDefsPayload::decode);

    private static void encode(FriendlyByteBuf buffer, SyncSmithingDefsPayload payload) {
        buffer.writeUtf(payload.json(), MAX_LENGTH);
    }

    private static SyncSmithingDefsPayload decode(FriendlyByteBuf buffer) {
        return new SyncSmithingDefsPayload(buffer.readUtf(MAX_LENGTH));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            try {
                SmithingDefinitions.applyJson(JsonParser.parseString(json).getAsJsonObject());
            } catch (RuntimeException exception) {
                // 壊れた定義は無視して既定（空）のままにする
            }
        });
    }

    /** サーバーの現在の定義を 1 人へ送る。 */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new SyncSmithingDefsPayload(SmithingDefinitions.toJson().toString()));
    }
}
