package com.zoma.SwordSkill.swordskills.skill.axe;

import com.zoma.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma.SwordSkill.swordskills.SkillTexture.AxeGreenSkillTexture;
import static com.zoma.SwordSkill.swordskills.SkillUtils.*;

public class Excite implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
        double damage = BaseDamage(player);
        double knockbackForce = BaseKnowBack(player)*0.25f;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-20,0,5);
        String skill_particle = AxeGreenSkillTexture();
        StrongSkillSound(level,player.position());
        spawnAttackEffect(level, spawnPos, Rotation,size, player, damage, knockbackForce, duration,skill_particle);
    }
}
