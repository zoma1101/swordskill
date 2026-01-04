package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// データ不要のリクエストパケット
public record CheckSkillUnlockedPayload() implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<CheckSkillUnlockedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "check_skill_unlocked"));

    public static final StreamCodec<FriendlyByteBuf, CheckSkillUnlockedPayload> STREAM_CODEC = StreamCodec.unit(new CheckSkillUnlockedPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // サーバーサイドハンドラー
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }

            try {
                // サーバー側でアンロック済みスキルを取得
                int[] unlockedSkillsArray = SkillDataFetcher.getUnlockedSkills(player);
                // int配列をList<Integer>に変換
                List<Integer> unlockedSkillsList = Arrays.stream(unlockedSkillsArray).boxed().collect(Collectors.toList());
                PacketDistributor.sendToPlayer(player, new SyncUnlockedSkillsPayload(unlockedSkillsList));

            } catch (Exception e) {
                LOGGER.error("Failed to process CheckSkillUnlockedPacket for player {}", player.getName().getString(), e);
            }
        });
    }
}