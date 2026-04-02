package com.zoma1101.swordskill.swordskills.skill.two_handed_sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SlashBlade extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        if (tickCount == 5) {
            Vector3f size = new Vector3f(3f, 2f, 2f);
            Vec3 rotation = new Vec3(0, 0, 0);
            Vec3 move = new Vec3(0, 0, 80); // 高速で飛翔する巨大な斬撃
            spawnMovingRelativeSlash(level, player, 1.0, 0.0, 0.0, rotation, size, 2.5, 1.0, SkillID, move);
            swingArm(player);
        }
    }
}