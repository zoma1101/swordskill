package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SharpNeil extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        Vector3f size = new Vector3f(7.2f, 3f, 1.4f);

        java.util.List<com.zoma1101.swordskill.swordskills.SkillTag> tags = java.util.List.of(SkillTag.BLOOD);


        if (tickCount == 1) setTrailActive(player, true);
        if (tickCount == 4) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 1.0, new Vec3(0, 5, -30), size, 2.0, 0.1, 12, tags, SkillID);
            swingArm(player);
        } else if (tickCount == 10) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 5, -30), size, 2.0, 0.1, 12, tags, SkillID);
            swingArm(player);
        } else if (tickCount == 17) {
            spawnRelativeSlash(level, player, 2.0, 0.0, -1.0, new Vec3(-12, 5, -30), size, 2.0, 0.75, 12, tags, SkillID);
            swingArm(player);
        }

        if (tickCount >= 25) setTrailActive(player, false);
    }
}
