package com.zoma1101.swordskill.data;

import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SkillDataFetcher {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * プレイヤーが習得している全スキルのIDを取得します。
     * Capabilityからデータを読み込むため、ワールドごとに独立したデータが返されます。
     */
    public static int[] getUnlockedSkills(ServerPlayer player) {
        // ラムダ式内で値を更新するため AtomicReference を使用
        AtomicReference<int[]> result = new AtomicReference<>(new int[0]);

        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            // Set<Integer> を int[] に変換して格納
            result.set(skills.getUnlockedSkills().stream().mapToInt(i -> i).toArray());
        });

        return result.get();
    }

    /**
     * 指定したスキルIDをプレイヤーが習得しているか確認します。
     */
    public static boolean isSkillUnlocked(ServerPlayer player, int targetSkillId) {
        AtomicBoolean result = new AtomicBoolean(false);

        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            result.set(skills.isSkillUnlocked(targetSkillId));
        });

        return result.get();
    }
}