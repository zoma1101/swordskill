package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Slant extends BaseSkill { 
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vector3f size = new Vector3f(6.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-6,0,135);
        
        spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, Rotation, size, 2.0, 0.5, duration, null, SkillID);
        swingArm(player);
    }
}
// 他のスキルクラスも同様に修正