package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.toClient.SyncSPPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.network.PacketDistributor;

public class SPManager {

    public static void handleSPRegen(ServerPlayer player) {
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            skills.tickCombatTimer(); // 戦闘タイマーを進める

            AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP.get());
            AttributeInstance regenSpAttr = player.getAttribute(SwordSkillAttribute.SP_REGEN.get());

            if (maxSpAttr != null && regenSpAttr != null) {
                double maxSP = maxSpAttr.getValue();
                double regenSP = regenSpAttr.getValue();

                // 非戦闘時は回復量を3倍にする
                double multiplier = skills.isInCombat() ? 1.0 : 3.0;
                double regenPerTick = (regenSP * multiplier) / 20.0;

                double previousSP = skills.getCurrentSP();
                skills.addSP(regenPerTick, maxSP);

                // HUD更新頻度を抑えるため、整数値が変わった時、または5回に1回同期するなどの工夫も可能だが、
                // 今回はシンプルに値が変わっていれば送る（regenPerTickが小さいので毎Tick送るのとほぼ同等）
                if (skills.getCurrentSP() != previousSP) {
                    NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                            new SyncSPPacket(skills.getCurrentSP()));
                }
            }
        });
    }

    public static void onAttack(ServerPlayer player) {
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP.get());
            double maxSP = maxSpAttr != null ? maxSpAttr.getValue() : 100.0;

            // 攻撃時に最大SPの2%を回復
            skills.recoverSPOnAttack(2.0, maxSP);

            // クライアントに同期
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncSPPacket(skills.getCurrentSP()));
        });
    }

    public static double getCurrentSP(ServerPlayer player) {
        return player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).map(skills -> skills.getCurrentSP())
                .orElse(0.0);
    }

    public static void consumeSP(ServerPlayer player, double amount) {
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP.get());
            double maxSP = maxSpAttr != null ? maxSpAttr.getValue() : 100.0;
            skills.addSP(-amount, maxSP);

            // クライアントに同期
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncSPPacket(skills.getCurrentSP()));
        });
    }
}
