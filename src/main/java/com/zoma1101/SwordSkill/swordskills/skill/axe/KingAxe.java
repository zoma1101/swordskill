package com.zoma1101.SwordSkill.swordskills.skill.axe;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.GodSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.AxeKingSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class KingAxe implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(3.0)); // 目の前2ブロック
        double damage = BaseDamage(player)*7f;
        double knockbackForce = BaseKnowBack(player)*1.25f;
        Vector3f size = new Vector3f(16f, 3f, 16f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-20,0,5);
        String skill_particle = AxeKingSkillTexture();
        StrongSkillSound(level,player.position());
        GodSkillSound(level,player.position());

        spawnAttackEffect(level, spawnPos, Rotation,size, player, damage, knockbackForce, duration,skill_particle);
    }
}
