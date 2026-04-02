package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Horizontal_square extends BaseSkill {

    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        if (tickCount == 5) {
            spawnMovingRelativeSlash(level, player, -2.0, 0.0, 0.0, new Vec3(6, 0, -5), new Vector3f(7.2f, 3f, 2.4f), 1.25f,
                    0.75, SkillID, new Vec3(0, 0, -10)); // 向向きは正面(0)、移動は後方(-10)
            swingArm(player);
        }
        else if (tickCount == 12) {
            spawnRelativeSlash(level, player, 1.0, 1.0, 0.0, new Vec3(6, 120, -5), new Vector3f(6.0f, 3f, 6.0f), 1.25f,
                    0.75, 12, null);
            swingArm(player);
        }
        else if (tickCount == 17) {
            spawnMovingRelativeSlash(level, player, 1.0, 1.0, 0.0, new Vec3(0, 45, 0), new Vector3f(2.4f, 3f, 2.4f), 1.5f, 0.75,SkillID, new Vec3(0,0,10));
            spawnMovingRelativeSlash(level, player, 1.0, -1.0, 0.0, new Vec3(0, -45, 0), new Vector3f(2.4f, 3f, 2.4f), 1.5f, 0.75,SkillID, new Vec3(0,0,10));
            spawnMovingRelativeSlash(level, player, -1.0, 1.0, 0.0, new Vec3(0, 135, 0), new Vector3f(2.4f, 3f, 2.4f), 1.5f, 0.75,SkillID, new Vec3(0,0,10)); // 後方に飛ばす
            spawnMovingRelativeSlash(level, player, -1.0, -1.0, 0.0, new Vec3(0, -135, 0), new Vector3f(2.4f, 3f, 2.4f), 1.5f, 0.75,SkillID, new Vec3(0,0,10)); // 後方に飛ばす
            swingArm(player);
        }
    }
}