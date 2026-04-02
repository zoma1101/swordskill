package com.zoma1101.swordskill.swordskills.skill.spear;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class SpinningSpear implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 0) {
            setTrailActive(player, true);
        }

        if (FinalTick == 5) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,3f, FinalTick);
        } else if (FinalTick == 12) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f, FinalTick);
        } else if (FinalTick == 15) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.5f, FinalTick);
        } else if (FinalTick == 18) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f, FinalTick);
        } else if (FinalTick == 21) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.5f, FinalTick);
        }
        else if (FinalTick == 24) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.5f, FinalTick);
        }
        else if (FinalTick == 27) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.5f, FinalTick);
        }
        else if (FinalTick == 30) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75F,0.5f, FinalTick);
        }
        else if (FinalTick == 32) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f, FinalTick);
            setTrailActive(player, false);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage, int finalTick) {
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
        Vec3 rotation = calculateRotation(slashIndex, finalTick);
        String skill_particle = slashIndex==1 ? AxeBlueSkillTexture() : Spia_Particle();


        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        SimpleSkillSound(level,player.position());
    }
    private Vec3 calculateRotation(int slashIndex, int finalTick) {
        if (slashIndex == 0) {
            return new Vec3(0, 0, 30); // 1回目の斬撃
        } else {
            // 2回目の斬撃以降は槍の回転に合わせてエフェクトも回転させる
            // FinalTick(12〜32) に応じてZ軸（ロール）を回転
            float rollAngle = (finalTick * 25.0f) % 360.0f;
            return new Vec3(5, 0, rollAngle); // ピッチ5度、ヨー0度、ロールで回転
        }
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 relativePos = slashIndex==0 ? lookVec.scale(2) : new Vec3(0, player.getEyeHeight() * 0.6, 0);
        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0);
    }



}
