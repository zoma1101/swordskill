package com.zoma.SwordSkill.swordskills.skill.sword;

import com.zoma.SwordSkill.swordskills.ISkill; // インターフェース名を変更
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma.SwordSkill.swordskills.SkillUtils.*;

public class Slant implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
        double damage = BaseDamage(player)*2f;
        double knockbackForce = BaseKnowBack(player)*0.5;
        Vector3f size = new Vector3f(6.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-6,0,45);
        String skill_particle = NomalSkillTexture();
        SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle);
    }
}

// 他のスキルクラスも同様に修正