package com.zoma1101.swordskill.swordskills;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * 派生スキル（{@code TRANSFORM} → {@code TRANSFORM_FINISH}）のチェーン解決。
 *
 * <p>skilldata の仕様上、チェーンは ID が連続した {@code TRANSFORM}/{@code TRANSFORM_FINISH}
 * の並びで表現される（例: vertical → vertical_arc → vertical_square）。</p>
 */
public final class SkillChains {

    private SkillChains() {
    }

    public static boolean isChainMember(@Nullable SkillData skill) {
        return skill != null && (skill.getType() == SkillData.SkillType.TRANSFORM
                || skill.getType() == SkillData.SkillType.TRANSFORM_FINISH);
    }

    /** そのスキルが属するチェーンの先頭（元スキル）の ID。単発スキルなら自身の ID。 */
    public static int start(int skillId) {
        int start = Math.max(0, skillId);
        while (start > 0 && isChainMember(SwordSkillRegistry.SKILLS.get(start - 1))) {
            start--;
        }
        return start;
    }

    /** チェーンの終端 ID（{@code TRANSFORM_FINISH}。無ければ連続する最後のメンバー）。 */
    public static int end(int startId) {
        int end = startId;
        int size = SwordSkillRegistry.SKILLS.size();
        for (int id = startId; id < size; id++) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(id);
            if (!isChainMember(skill)) {
                break;
            }
            end = id;
            if (skill.getType() == SkillData.SkillType.TRANSFORM_FINISH) {
                break;
            }
        }
        return end;
    }

    /** チェーン全体の ID リスト。チェーンの成員でない（単発スキル）なら空リスト。 */
    public static List<Integer> chainOf(int skillId) {
        List<Integer> ids = new ArrayList<>();
        if (!isChainMember(SwordSkillRegistry.SKILLS.get(skillId))) {
            return ids;
        }
        int start = start(skillId);
        int end = end(start);
        if (end <= start) {
            return ids;
        }
        for (int id = start; id <= end; id++) {
            ids.add(id);
        }
        return ids;
    }

    /** そのスキルの1つ前段（派生元）の ID。元スキル・単発スキルなら -1。 */
    public static int parentOf(int skillId) {
        if (skillId <= 0) {
            return -1;
        }
        return start(skillId) == skillId ? -1 : skillId - 1;
    }

    /** チェーン先頭からの段数（元スキル=0、その派生=1...）。 */
    public static int depthOf(int skillId) {
        int depth = 0;
        int id = Math.max(0, skillId);
        while (id > 0 && isChainMember(SwordSkillRegistry.SKILLS.get(id - 1))) {
            depth++;
            id--;
        }
        return depth;
    }
}
