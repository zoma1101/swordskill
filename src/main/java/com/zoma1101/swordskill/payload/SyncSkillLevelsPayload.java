package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillLevelHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * サーバーからクライアントへスキルレベル情報を送る。レベル1のスキルは含めない。
 * 併せて倍率カーブ（最大レベル・最小倍率・最大倍率）も送ることで、
 * UI がサーバーの設定どおりの実効倍率を表示できるようにする。
 */
public record SyncSkillLevelsPayload(int[] skillIds, int[] levels, int maxLevel, double minMultiplier,
        double maxMultiplier) implements CustomPacketPayload {

    private static final int MAX_ENTRIES = 4096;

    public static final Type<SyncSkillLevelsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_skill_levels"));

    public static final StreamCodec<FriendlyByteBuf, SyncSkillLevelsPayload> STREAM_CODEC = StreamCodec.of(
            SyncSkillLevelsPayload::encode, SyncSkillLevelsPayload::decode);

    public SyncSkillLevelsPayload {
        if (skillIds == null || levels == null || skillIds.length != levels.length
                || skillIds.length > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid synchronized skill level data");
        }
    }

    private static void encode(FriendlyByteBuf buffer, SyncSkillLevelsPayload payload) {
        buffer.writeVarInt(payload.skillIds().length);
        for (int index = 0; index < payload.skillIds().length; index++) {
            buffer.writeVarInt(payload.skillIds()[index]);
            buffer.writeVarInt(payload.levels()[index]);
        }
        buffer.writeVarInt(payload.maxLevel());
        buffer.writeDouble(payload.minMultiplier());
        buffer.writeDouble(payload.maxMultiplier());
    }

    private static SyncSkillLevelsPayload decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid synchronized skill level count: " + count);
        }
        int[] skillIds = new int[count];
        int[] levels = new int[count];
        for (int index = 0; index < count; index++) {
            skillIds[index] = buffer.readVarInt();
            levels[index] = buffer.readVarInt();
        }
        return new SyncSkillLevelsPayload(skillIds, levels, buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncSkillLevelsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSkillLevelHandler.update(payload.skillIds(), payload.levels(),
                payload.maxLevel(), payload.minMultiplier(), payload.maxMultiplier()));
    }
}
