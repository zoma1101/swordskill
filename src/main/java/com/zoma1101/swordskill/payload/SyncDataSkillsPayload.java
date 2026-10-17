package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.SkillDataLoader;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncDataSkillsPayload(UUID batchId, int chunkIndex, int chunkCount, String definitionsJson)
        implements CustomPacketPayload {
    public static final int MAX_CHUNK_CHARS = 16_000;
    private static final int MAX_CHUNKS_PER_BATCH = 256;
    private static final int MAX_PENDING_BATCHES = 4;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<UUID, SyncBatch> PENDING_BATCHES = new LinkedHashMap<>();
    public static final Type<SyncDataSkillsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_data_skills"));
    public static final StreamCodec<FriendlyByteBuf, SyncDataSkillsPayload> STREAM_CODEC = StreamCodec.of(
            SyncDataSkillsPayload::encode, SyncDataSkillsPayload::decode);

    public SyncDataSkillsPayload {
        if (batchId == null || chunkCount < 1 || chunkCount > MAX_CHUNKS_PER_BATCH
                || chunkIndex < 0 || chunkIndex >= chunkCount
                || definitionsJson == null || definitionsJson.length() > MAX_CHUNK_CHARS) {
            throw new IllegalArgumentException("Invalid synchronized skill-data chunk");
        }
    }

    public static List<SyncDataSkillsPayload> createBatch(List<DataSkillDefinition> definitions) {
        List<String> definitionJson = definitions.stream().map(SkillDataLoader::toSyncJson).toList();
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder("[");
        for (String json : definitionJson) {
            int requiredLength = json.length() + (currentChunk.length() > 1 ? 1 : 0);
            if (json.length() + 2 > MAX_CHUNK_CHARS) {
                throw new IllegalStateException("A single skill definition exceeds the sync payload limit");
            }
            if (currentChunk.length() + requiredLength + 1 > MAX_CHUNK_CHARS) {
                currentChunk.append(']');
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder("[");
            }
            if (currentChunk.length() > 1) {
                currentChunk.append(',');
            }
            currentChunk.append(json);
        }
        currentChunk.append(']');
        chunks.add(currentChunk.toString());
        if (chunks.size() > MAX_CHUNKS_PER_BATCH) {
            throw new IllegalStateException("Skill definitions require too many sync payload chunks: "
                    + chunks.size());
        }

        UUID batchId = UUID.randomUUID();
        List<SyncDataSkillsPayload> payloads = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            payloads.add(new SyncDataSkillsPayload(batchId, index, chunks.size(), chunks.get(index)));
        }
        return List.copyOf(payloads);
    }

    private static void encode(FriendlyByteBuf buffer, SyncDataSkillsPayload payload) {
        buffer.writeUUID(payload.batchId());
        buffer.writeVarInt(payload.chunkIndex());
        buffer.writeVarInt(payload.chunkCount());
        buffer.writeUtf(payload.definitionsJson(), MAX_CHUNK_CHARS);
    }

    private static SyncDataSkillsPayload decode(FriendlyByteBuf buffer) {
        return new SyncDataSkillsPayload(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(MAX_CHUNK_CHARS));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleClient(SyncDataSkillsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> acceptChunk(payload));
    }

    private static void acceptChunk(SyncDataSkillsPayload payload) {
        SyncBatch batch = PENDING_BATCHES.get(payload.batchId());
        if (batch == null) {
            while (PENDING_BATCHES.size() >= MAX_PENDING_BATCHES) {
                UUID oldestBatch = PENDING_BATCHES.keySet().iterator().next();
                PENDING_BATCHES.remove(oldestBatch);
                LOGGER.warn("Discarded incomplete synchronized SwordSkill batch {}", oldestBatch);
            }
            batch = new SyncBatch(payload.chunkCount());
            PENDING_BATCHES.put(payload.batchId(), batch);
        } else if (batch.chunks.length != payload.chunkCount()) {
            PENDING_BATCHES.remove(payload.batchId());
            LOGGER.error("Inconsistent chunk count for synchronized SwordSkill batch {}", payload.batchId());
            return;
        }

        if (batch.chunks[payload.chunkIndex()] == null) {
            batch.chunks[payload.chunkIndex()] = payload.definitionsJson();
            batch.receivedChunks++;
        }
        if (batch.receivedChunks != batch.chunks.length) {
            return;
        }

        PENDING_BATCHES.remove(payload.batchId());
        StringBuilder definitionsJson = new StringBuilder("[");
        for (String chunk : batch.chunks) {
            String content = chunk.substring(1, chunk.length() - 1);
            if (!content.isEmpty()) {
                if (definitionsJson.length() > 1) {
                    definitionsJson.append(',');
                }
                definitionsJson.append(content);
            }
        }
        definitionsJson.append(']');
        try {
            SwordSkillRegistry.replaceDataDrivenSkills(SkillDataLoader.parseSyncJson(definitionsJson.toString()));
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to apply synchronized SwordSkill definitions", exception);
        }
    }

    private static final class SyncBatch {
        private final String[] chunks;
        private int receivedChunks;

        private SyncBatch(int chunkCount) {
            chunks = new String[chunkCount];
        }
    }
}
