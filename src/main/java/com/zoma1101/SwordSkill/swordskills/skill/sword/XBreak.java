package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class XBreak implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) {
            performSlash(level, player, 1, 0.75F);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
        double damage = BaseDamage(player) * 1.5f;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 0, 135); // 1回目の斬撃
            case 1 -> new Vec3(-6, 0, 45); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }
}
