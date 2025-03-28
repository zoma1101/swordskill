package com.zoma1101.swordskill.swordskills.skill.mace;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.*;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class HammerDown implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 7) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(1.5)); // 目の前2ブロック
            double damage = BaseDamage(player)*4f;
            double knockbackForce = BaseKnowBack(player)*1.05f;
            Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
            int duration = 12;
            Vec3 Rotation = new Vec3(0,20,85);
            String skill_particle = MaceBlue_Texture();
            StrongSkillSound(level,spawnPos);
            GodSkillSound(level,spawnPos);
            spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        }
    }
}
