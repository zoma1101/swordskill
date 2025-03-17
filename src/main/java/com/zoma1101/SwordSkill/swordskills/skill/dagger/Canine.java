package com.zoma1101.SwordSkill.swordskills.skill.dagger;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Canine implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(1.0)); // 目の前2ブロック
        double damage = BaseDamage(player)*3.5f;
        double knockbackForce = BaseKnowBack(player)*0.5;
        Vector3f size = new Vector3f(4.2f, 3f, 1.7f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-6,0,-45);
        String skill_particle = NomalSkillTexture();
        SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
}
// 他のスキルクラスも同様に修正