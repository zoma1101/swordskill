package com.zoma1101.swordskill.swordskills.skill.spear;

import com.zoma1101.swordskill.swordskills.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Thrusts extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(3)); // 目の前2ブロック
        double damage = BaseDamage(player)*3f;
        double knockbackForce = BaseKnowBack(player)*0.7;
        Vector3f size = new Vector3f(1.5f, 1.5f, 7f);
        int duration = 12;
        Vec3 Rotation = new Vec3(0,0,30);
        String skill_particle = Spia_Particle();
        SimpleSkillSound(level,spawnPos);
        
        java.util.List<SkillTag> tags = new java.util.ArrayList<>();
        tags.add(SkillTag.TRAIL);
        tags.add(SkillTag.RAY);
        tags.add(SkillTag.SHAPE_THRUST);
        
        SkillUtils.spawnAttackEffect(level, spawnPos, Rotation, size, player, damage, knockbackForce, duration, skill_particle, 
                tags, SwordSkillRegistry.SKILLS.get(SkillID).getTrailColor(), Vec3.ZERO, false);
        swingArm(player);
    }
}
