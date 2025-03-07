package com.zoma.SwordSkill.swordskills.skill.spear;

import com.zoma.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma.SwordSkill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma.SwordSkill.swordskills.SkillUtils.*;

public class Linear implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(2.5)); // 目の前2ブロック
        double damage = BaseDamage(player)*3f;
        double knockbackForce = BaseKnowBack(player)*0.5;
        Vector3f size = new Vector3f(0.75f, 0.75f, 6f);
        int duration = 12;
        Vec3 Rotation = new Vec3(0,0,40);
        String skill_particle = Spia_Particle();
        SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle);
    }
}
