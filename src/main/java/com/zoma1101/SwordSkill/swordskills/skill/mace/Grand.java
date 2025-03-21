package com.zoma1101.SwordSkill.swordskills.skill.mace;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Grand implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
            double damage = BaseDamage(player)*1.5f;
            double knockbackForce = BaseKnowBack(player)*1.2f;
            Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
            int duration = 12;
            Vec3 Rotation = new Vec3(-20,0,175);
            String skill_particle = MaceBlue_Texture();
        StrongSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
}
