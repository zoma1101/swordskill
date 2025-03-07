package com.zoma.SwordSkill.swordskills.skill.dual_sword;

import com.zoma.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma.SwordSkill.swordskills.SkillTexture.BlueRollTexture;
import static com.zoma.SwordSkill.swordskills.SkillUtils.*;

public class EndRevolver implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick ==3) {
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0); // 目の前2ブロック
            double damage = BaseDamage(player)*2.25f;
            double knockbackForce = BaseKnowBack(player)*1.5;
            Vector3f size = new Vector3f(8f, 2f, 8f);
            int duration = 12;
            Vec3 Rotation = new Vec3(0,0,0);
            String skill_particle = BlueRollTexture();
            SimpleSkillSound(level,spawnPos);
            spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle);
        }
    }
}
