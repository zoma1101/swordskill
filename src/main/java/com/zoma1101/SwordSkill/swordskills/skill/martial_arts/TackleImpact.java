package com.zoma1101.swordskill.swordskills.skill.martial_arts;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;

public class TackleImpact extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        // トレイルを有効化（SwordSkillRegistryでBOTH_LEGSを指定したため、両側に出る）
        if (tickCount == 1) {
            setTrailActive(player, true);
        }

        if (tickCount == 3) {
            // 1. 回し蹴り：周囲を払う (固定ダメージ: 8, 低い位置から発生)
            Vector3f size = new Vector3f(5.0f, 1.5f, 5.0f);
            spawnFixedRelativeSlash(level, player, 1.5, 0.0, 0.0, 0.3, Vec3.ZERO, size, 8.0, 1.2, 8, Collections.emptyList(), SkillID);
            swingArm(player);

        } else if (tickCount >= 6 && tickCount <= 16) {
            // 2. タックル：突進しつつ連続攻撃 (固定ダメージ: 7)
            Vec3 move = player.getLookAngle().scale(0.6).add(0, 0.1, 0);
            player.setDeltaMovement(move.x, player.getDeltaMovement().y, move.z);
            player.hurtMarked = true;

            if (tickCount % 3 == 0) {
                Vector3f size = new Vector3f(2.5f, 2.5f, 2.5f);
                // 波動を表示するために RAY と SHAPE_THRUST を追加
                spawnFixedRelativeSlash(level, player, 1.0, 0.0, 0.0, 0.4, Vec3.ZERO, size, 3.0, 0.4, 5, List.of(SkillTag.RAY, SkillTag.SHAPE_THRUST), SkillID);
            }

        } else if (tickCount == 19) {
            // 3. 蹴り上げ：上方向に飛ばす (固定ダメージ: 10)
            Vector3f size = new Vector3f(2.5f, 6.0f, 2.5f);
            // 蹴り上げは少し高め(0.5)から発生させて上に飛ばす
            spawnFixedRelativeSlash(level, player, 1.5, 0.0, 0.0, 0.5, new Vec3(90, 0, 0), size, 10.0, 1.8, 12, Collections.emptyList(), SkillID);
            swingArm(player);
        }

        // 終了後にトレイルを無効化
        if (tickCount >= 30) {
            setTrailActive(player, false);
        }
    }
}
