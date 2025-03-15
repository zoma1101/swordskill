package com.zoma1101.SwordSkill.swordskills.skill.two_handed_sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.AxeRedSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class SlashBlade implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2){
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(0.5)); // 目の前2ブロック
            double damage = BaseDamage(player)*2.3f;
            double knockbackForce = BaseKnowBack(player)*0.5;
            Vector3f size = new Vector3f(8f, 2f, 8f);
            int duration = 12;
            Vec3 Rotation = new Vec3(0,0,20);
            String skill_particle = AxeRedSkillTexture();
            SimpleSkillSound(level,spawnPos);
            Vec3 Move = new Vec3(0,0,60);
            spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Move);
        }

    }
}