package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientProficiencyHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * サーバーからクライアントへ熟練度情報を送る。値が 0 のカテゴリは含めない。
 * {@code pointsPerLevel} も送ることで、UI がサーバーの設定どおりの
 * 「次のレベルアップまでの進捗」を表示できるようにする。
 */
public record SyncProficiencyPayload(String[] categories, int[] proficiencies, int[] spent, int pointsPerLevel)
        implements CustomPacketPayload {

    private static final int MAX_ENTRIES = 64;

    public static final Type<SyncProficiencyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_proficiency"));

    public static final StreamCodec<FriendlyByteBuf, SyncProficiencyPayload> STREAM_CODEC = StreamCodec.of(
            SyncProficiencyPayload::encode, SyncProficiencyPayload::decode);

    public SyncProficiencyPayload {
        if (categories == null || proficiencies == null || spent == null
                || categories.length != proficiencies.length || categories.length != spent.length
                || categories.length > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid synchronized proficiency data");
        }
    }

    private static void encode(FriendlyByteBuf buffer, SyncProficiencyPayload payload) {
        buffer.writeVarInt(payload.categories().length);
        for (int index = 0; index < payload.categories().length; index++) {
            buffer.writeUtf(payload.categories()[index] == null ? "" : payload.categories()[index], 64);
            buffer.writeVarInt(Math.max(0, payload.proficiencies()[index]));
            buffer.writeVarInt(Math.max(0, payload.spent()[index]));
        }
        buffer.writeVarInt(Math.max(1, payload.pointsPerLevel()));
    }

    private static SyncProficiencyPayload decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid synchronized proficiency count: " + count);
        }
        String[] categories = new String[count];
        int[] proficiencies = new int[count];
        int[] spent = new int[count];
        for (int index = 0; index < count; index++) {
            categories[index] = buffer.readUtf(64);
            proficiencies[index] = buffer.readVarInt();
            spent[index] = buffer.readVarInt();
        }
        return new SyncProficiencyPayload(categories, proficiencies, spent, buffer.readVarInt());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncProficiencyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientProficiencyHandler.update(payload.categories(), payload.proficiencies(),
                payload.spent(), payload.pointsPerLevel()));
    }
}
