package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static com.zoma1101.swordskill.data.SkillDataFetcher.isSkillUnlocked;


public record UseSkillPayload(int skillId, int finalTick) implements CustomPacketPayload {
    private static final Logger LOGGER = LogManager.getLogger();

    // 1. ペイロードIDを定義 (MOD_IDとペイロード名でResourceLocationを作成)
    public static final Type<UseSkillPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "use_skill"));

    // 2. StreamCodecを定義 (エンコード/デコードロジック)
    public static final StreamCodec<FriendlyByteBuf, UseSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, // skillIdのコーデック
            UseSkillPayload::skillId, // skillIdゲッター
            ByteBufCodecs.INT, // finalTickのコーデック
            UseSkillPayload::finalTick, // finalTickゲッター
            UseSkillPayload::new // コンストラクタ参照
    );

    // 3. CustomPacketPayloadインターフェースのメソッドを実装
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UseSkillPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            // IPayloadContextからプレイヤーを取得し、ServerPlayerか確認
            if (!(ctx.player() instanceof ServerPlayer player)) {
                // サーバーサイドハンドラなのでServerPlayerのはずだが念のため
                LOGGER.warn("Received UseSkillPayload from non-ServerPlayer: {}", ctx.player().getName().getString());
                return;
            }

            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId());
            // スキルが有効か、プレイヤーがアンロックしているか等のチェック
            if (skill != null && (isSkillUnlocked(player, msg.skillId()) || player.gameMode.isCreative() || !UnlockedSkill.get())) {
                if (msg.finalTick() == 0) {
                    // FinalTickが0の場合、一度だけ実行
                    executeSkill(player, skill);
                } else {
                    // スキル実行情報を保存
                    SkillExecutionManager.startSkillExecution(player, msg.skillId(), msg.finalTick());
                }
            } else if (skill == null) {
                LOGGER.warn("Skill with ID {} not found for player {}.", msg.skillId(), player.getName().getString());
            } else {
                LOGGER.warn("Player {} attempted to use locked skill ID {}.", player.getName().getString(), msg.skillId());
            }
        });
    }

    // 実行ロジックはそのまま
    private static void executeSkill(ServerPlayer player, SkillData skill) {
        try {
            ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
            skillInstance.execute(player.level(), player, 0, skill.getId());
            LOGGER.info("Executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(), player.getName().getString());
        } catch (Exception e) {
            LOGGER.error("Error executing skill {} (ID: {}) for player {}", skill.getName(), skill.getId(), player.getName().getString(), e);
        }
    }
}