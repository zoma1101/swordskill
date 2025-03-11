package com.zoma1101.SwordSkill.swordskills.skill.katana;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Zekkuu implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
            double damage = BaseDamage(player) * 3.5f;
            double knockbackForce = BaseKnowBack(player) * 0.5;
            Vector3f size = new Vector3f(6f, 3f, 0.5f);
            int duration = 12;
            Vec3 Rotation = new Vec3(-6, 0, -135);
            String skill_particle = NomalSkillTexture();
            SimpleSkillSound(level, spawnPos);
            spawnAttackEffect(level, spawnPos, Rotation, size, player, damage, knockbackForce, duration, skill_particle);
            Vec3 moveVec = lookVec.scale(4);
            player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
            player.hurtMarked = true;
        }
        if (FinalTick == 2) {
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
    }
}
// 他のスキルクラスも同様に修正