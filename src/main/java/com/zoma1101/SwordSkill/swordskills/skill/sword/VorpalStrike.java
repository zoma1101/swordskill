package com.zoma1101.SwordSkill.swordskills.skill.sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.Spia_Particle_red;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class VorpalStrike implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 15) {
            Vec3 lookVec = player.getLookAngle();
            player.setDeltaMovement(lookVec.scale(2).x,lookVec.scale(2).y,lookVec.scale(2).z);
            player.hurtMarked = true;
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(10));
            double damage = RushDamage(player)*5.5f;
            double knockbackForce = BaseKnowBack(player)*1.25;
            Vector3f size = new Vector3f(1f, 1f, 20f);
            int duration = 12;
            Vec3 Rotation = new Vec3(0,0,40);
            String skill_particle = Spia_Particle_red();
            SimpleSkillSound(level,spawnPos);
            spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        }
    }
}
