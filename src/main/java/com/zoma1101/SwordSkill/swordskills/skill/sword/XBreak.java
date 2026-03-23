package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class XBreak extends BaseSkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 6) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 0, 135), new Vector3f(7.2f, 3f, 2.4f), 1.5,
                    0.1);
            swingArm(player);
        }
        else if (FinalTick == 10) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 0, 135), new Vector3f(7.2f, 3f, 2.4f), 1.5,
                    0.75);
            swingArm(player);
        }
    }
}
