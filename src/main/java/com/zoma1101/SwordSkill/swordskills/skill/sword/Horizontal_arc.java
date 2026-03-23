package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Horizontal_arc extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        if (tickCount == 3) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 0, 25), new Vector3f(7.2f, 3f, 2.4f), 2,
                    1.0);
            swingArm(player);
        }
    }
}
