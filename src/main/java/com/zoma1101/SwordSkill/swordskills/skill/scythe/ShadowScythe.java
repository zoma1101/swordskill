package com.zoma1101.swordskill.swordskills.skill.scythe;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ShadowScythe extends BaseSkill {

    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        if (tickCount == 3) {
            spawnMovingRelativeSlash(level, player, 1.0, 0, 0.0, new Vec3(0,0 , 135), new Vector3f(2.4f, 2f, 2.4f), 0.8f, 0.1,SkillID, new Vec3(0,0,70));
        } else if (tickCount == 7) {
            spawnMovingRelativeSlash(level, player, 1.0, 0, 0.0, new Vec3(0,0 , -15), new Vector3f(2.4f, 2f, 2.4f), 0.8f, 0.1,SkillID, new Vec3(0,0,60));
        }
    }

}
