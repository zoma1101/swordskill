package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// サーバーからクライアントへアンロック情報を送るパケット
public record SyncUnlockedSkillsPayload(List<Integer> unlockedSkills) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<SyncUnlockedSkillsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "sync_unlocked_skills"));

    // List<Integer> 用のコーデック
    public static final StreamCodec<FriendlyByteBuf, SyncUnlockedSkillsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.INT), // IntegerのList
            SyncUnlockedSkillsPayload::unlockedSkills,
            SyncUnlockedSkillsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // クライアントサイドハンドラー
    public static void handleClient(SyncUnlockedSkillsPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // 現在開いている画面を取得
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof SwordSkillSelectionScreen selectScreen) {
                selectScreen.unlockedSkills.clear();
                selectScreen.unlockedSkills.addAll(new HashSet<>(msg.unlockedSkills())); // ListをSetに変換して追加
                LOGGER.debug("Updated unlocked skills on screen: {}", msg.unlockedSkills().size());
            }
        });
    }
}