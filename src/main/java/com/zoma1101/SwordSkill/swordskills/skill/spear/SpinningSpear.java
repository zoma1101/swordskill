package com.zoma1101.SwordSkill.swordskills.skill.spear;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class SpinningSpear implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,2f);
        } else if (FinalTick == 7) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.75f);
        } else if (FinalTick == 10) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.75f);
        } else if (FinalTick == 13) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.75f);
        } else if (FinalTick == 16) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.75f);
        } else if (FinalTick == 20) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.75f);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,lookVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0 -> new Vector3f(0.75f, 0.75f, 7f);
            case 1 -> new Vector3f(6f, 1.5f, 6f);
            default -> new Vector3f().zero();
        };

        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex);
        String skill_particle = slashIndex==1 ? AxeBlueSkillTexture() : Spia_Particle();


        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        SimpleSkillSound(level,player.position());
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(0, 0, 30); // 1回目の斬撃
            case 1 -> new Vec3(5, 0, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 relativePos = slashIndex==0 ? lookVec.scale(2) : new Vec3(0, 0, 0);
        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0);
    }



}
