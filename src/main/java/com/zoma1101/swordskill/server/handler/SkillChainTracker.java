package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.swordskills.SkillChains;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * サーバー側で派生チェーンの受付時間を追跡する。
 *
 * <p>クライアントと同じく、直前に実行したスキルから一定 tick 以内の再入力だけを
 * 次の派生段として受け付ける。スキル自体の実行期間（duration）が短いスキルでも
 * コンボが成立するように、実行中かどうかとは別に時刻を記録する。</p>
 */
public final class SkillChainTracker {

    /** 通信遅延を見込んだ受付時間の猶予（tick）。 */
    private static final int TOLERANCE_TICKS = 20;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private SkillChainTracker() {
    }

    private record State(int skillId, long tick) {
    }

    /** スキルを使用したことを記録する。 */
    public static void record(ServerPlayer player, int skillId) {
        STATES.put(player.getUUID(), new State(skillId, player.level().getGameTime()));
    }

    /** 直前のスキルから、指定した派生先へ進めるか。 */
    public static boolean canAdvance(ServerPlayer player, int nextSkillId) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return false;
        }
        SkillData previous = SwordSkillRegistry.SKILLS.get(state.skillId());
        if (previous == null || previous.getType() != SkillData.SkillType.TRANSFORM) {
            return false;
        }
        if (SkillChains.next(state.skillId()) != nextSkillId) {
            return false;
        }
        long elapsed = player.level().getGameTime() - state.tick();
        return elapsed >= 0 && elapsed <= previous.getTransformLimitTick() + TOLERANCE_TICKS;
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }
}
