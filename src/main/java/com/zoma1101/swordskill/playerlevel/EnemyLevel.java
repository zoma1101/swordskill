package com.zoma1101.swordskill.playerlevel;

import net.minecraft.world.entity.Entity;

/**
 * 敵（モンスター）が持つレベル。
 *
 * <p>レベルを持つエンティティだけがレベル差補正の対象になる。レベルを設定していない
 * エンティティ（他 MOD のモブやバニラのモブ）は {@link #get(Entity)} が 0 を返し、
 * 補正は一切かからない。
 *
 * <p>値はエンティティの永続データに保存されるため、再ログインやチャンク再読み込みでも維持される。
 */
public final class EnemyLevel {

    private static final String KEY = "SwordSkillEnemyLevel";

    private EnemyLevel() {
    }

    /** レベル未設定なら 0。 */
    public static int get(Entity entity) {
        if (entity == null) {
            return 0;
        }
        return Math.max(0, entity.getPersistentData().getInt(KEY));
    }

    public static void set(Entity entity, int level) {
        if (entity == null) {
            return;
        }
        if (level <= 0) {
            entity.getPersistentData().remove(KEY);
        } else {
            entity.getPersistentData().putInt(KEY, level);
        }
    }

    public static void clear(Entity entity) {
        set(entity, 0);
    }
}
