package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.server.handler.CounterManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * カウンタースキルのチャージ開始/終了を知らせるクライアント→サーバーのペイロード。
 *
 * <p>チャージ中かどうかをサーバーが知ることで、被ダメージの軽減と蓄積を行える。</p>
 */
public record CounterChargePayload(int skillId, boolean charging) implements CustomPacketPayload {
    public static final Type<CounterChargePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "counter_charge"));

    public static final StreamCodec<FriendlyByteBuf, CounterChargePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CounterChargePayload::skillId,
            ByteBufCodecs.BOOL,
            CounterChargePayload::charging,
            CounterChargePayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CounterChargePayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            // カウンタースキル以外の ID で軽減状態を作れないようにする
            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId());
            if (skill == null || !skill.isCounter()) {
                return;
            }
            if (msg.charging()) {
                CounterManager.begin(player.getUUID(), msg.skillId());
            } else {
                CounterManager.end(player.getUUID(), msg.skillId());
            }
        });
    }
}
