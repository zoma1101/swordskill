package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

// サーバーからクライアントへアンロック情報を送るパケット
public record SyncUnlockedSkillsPayload(List<Integer> unlockedSkills) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    /** 受信側で許容する最大スキルID（ビット数）。不正な長さのパケットで巨大な配列を作らせないための上限。 */
    private static final int MAX_BITS = 4096;

    public static final Type<SyncUnlockedSkillsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_unlocked_skills"));

    /**
     * 解放済みスキルをビットセットで送る。
     *
     * <p>スキルIDは 0 からの連番（{@code SwordSkillRegistry} が保証）なので、
     * 以前の「1件あたり int 4 バイト」から「全体で (最大ID+1)/8 バイト」に縮む。
     * 85 件なら 340 バイト → 11 バイト。</p>
     */
    public static final StreamCodec<FriendlyByteBuf, SyncUnlockedSkillsPayload> STREAM_CODEC = StreamCodec.of(
            SyncUnlockedSkillsPayload::encode,
            SyncUnlockedSkillsPayload::decode);

    private static void encode(FriendlyByteBuf buffer, SyncUnlockedSkillsPayload payload) {
        BitSet bits = new BitSet();
        for (int skillId : payload.unlockedSkills()) {
            if (skillId >= 0 && skillId < MAX_BITS) {
                bits.set(skillId);
            }
        }
        // ビット数（= 最大ID+1）を先頭に置くことで、読み出し側はバイト数を自分で決められる
        buffer.writeVarInt(bits.length());
        buffer.writeBytes(bits.toByteArray());
    }

    private static SyncUnlockedSkillsPayload decode(FriendlyByteBuf buffer) {
        int bitCount = buffer.readVarInt();
        if (bitCount < 0 || bitCount > MAX_BITS) {
            throw new DecoderException("Invalid unlocked skill bit count: " + bitCount);
        }
        byte[] bytes = new byte[(bitCount + 7) / 8];
        buffer.readBytes(bytes);
        BitSet bits = BitSet.valueOf(bytes);
        List<Integer> skills = new ArrayList<>(bits.cardinality());
        for (int id = bits.nextSetBit(0); id >= 0 && id < bitCount; id = bits.nextSetBit(id + 1)) {
            skills.add(id);
        }
        return new SyncUnlockedSkillsPayload(skills);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // クライアントサイドハンドラー
    public static void handleClient(SyncUnlockedSkillsPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // グローバルなストアを更新
            com.zoma1101.swordskill.client.handler.ClientUnlockedSkillsHandler
                    .updateUnlockedSkills(msg.unlockedSkills());

            // 現在開いている画面があればそれも更新
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof SwordSkillSelectionScreen selectScreen) {
                selectScreen.unlockedSkills.clear();
                selectScreen.unlockedSkills.addAll(new HashSet<>(msg.unlockedSkills()));
                LOGGER.debug("Updated unlocked skills on screen: {}", msg.unlockedSkills().size());
            }
        });
    }
}