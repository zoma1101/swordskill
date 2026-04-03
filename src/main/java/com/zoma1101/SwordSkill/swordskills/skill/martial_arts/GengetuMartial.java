package com.zoma1101.swordskill.swordskills.skill.martial_arts;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class GengetuMartial extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {

        // トレイルを有効化
        if (tickCount == 1) {
            setTrailActive(player, true);
        }
        
        if (tickCount == 5) {
            // 宙返りの頂点付近で強力な蹴り上げを発生させる
            Vector3f size = new Vector3f(1.0f, 6.0f, 3.0f);
            
            // 固定ダメージ: 15 (重攻撃)
            // 弦月らしく SONIC と SHAPE_ARC を使用
            spawnFixedRelativeSlash(level, player, 1.5, 0.0, 0.0, 0.5, new Vec3(90, 0, 0), size, 12.5, 2.0, 15,
                    List.of(), SkillID);
            
            swingArm(player);

            Vec3 backward = player.getLookAngle().reverse().scale(0.3).add(0, 0.2, 0);
            player.setDeltaMovement(backward.x, player.getDeltaMovement().y + 0.2, backward.z);
            player.hurtMarked = true;
        }
    }
}
