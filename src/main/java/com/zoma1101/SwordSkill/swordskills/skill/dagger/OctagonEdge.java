package com.zoma1101.SwordSkill.swordskills.skill.dagger;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class OctagonEdge implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,1.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) { // 2回目の斬撃
            performSlash(level, player, 1, 0.1F,1.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 8) { // 2回目の斬撃
            performSlash(level, player, 2, 0.1F,1.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) { // 2回目の斬撃
            performSlash(level, player, 3, 0.1F,1.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 14) { // 2回目の斬撃
            performSlash(level, player, 2, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.5));
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(5f, 3f, 2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 45);
            case 1 -> new Vec3(6, -5, 135);
            case 2 -> new Vec3(-6, 5, 5);
            case 3 -> new Vec3(0, 5, 79);
            default -> new Vec3(0, 0, 0);
        };
    }
}
