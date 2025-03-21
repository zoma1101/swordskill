package com.zoma1101.SwordSkill.swordskills.skill;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalWhipTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Whip implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(0.2)); // 目の前2ブロック
        double damage = BaseDamage(player)*2f;
        double knockbackForce = BaseKnowBack(player)*0.5;
        Vector3f size = new Vector3f(3f, 3f, 3f);
        int duration = 15;
        Vec3 Rotation = new Vec3(0,0,0);
        String skill_particle = NomalWhipTexture();
        SimpleSkillSound(level,spawnPos);
        Vec3 Move = Vec3.ZERO;
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Move);
    }
}
