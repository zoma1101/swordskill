package com.zoma1101.SwordSkill.swordskills.skill.scythe;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.RedSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Barrick implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.4)); // 目の前2ブロック
            double damage = BaseDamage(player)*1.75f;
            double knockbackForce = BaseKnowBack(player)*0.05f;
            Vector3f size = new Vector3f(9f, 3f, 3f);
            int duration = 12;
            Vec3 Rotation = new Vec3(-20,0,5);
            String skill_particle = RedSkillTexture();
            SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
}
