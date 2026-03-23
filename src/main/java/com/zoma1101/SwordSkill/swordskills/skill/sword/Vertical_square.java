package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Vertical_square extends BaseSkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 3) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 0, -45), new Vector3f(7.2f, 3f, 2.4f), 0.5f,
                    0.1f);
        } else if (FinalTick == 8) {
            spawnRelativeSlash(level, player, 2.0, 0.0, 0.0, new Vec3(-6, 0, 45), new Vector3f(7.2f, 3f, 2.4f), 0.5f,
                    0.1f);
        }
    }

}