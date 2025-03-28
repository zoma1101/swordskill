package com.zoma1101.swordskill.swordskills.skill.spear;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class SparkThrust implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 6) { // 1回目の斬撃
            performSlash(level, player, 0, 0.15F,1.6f);
        } else if (FinalTick == 10) { // 2回目の斬撃
            performSlash(level, player, 1, 0.15f,1.6f);
        } else if (FinalTick == 15) { // 2回目の斬撃
            performSlash(level, player, 2, 0.15F,1.6f);
        } else if (FinalTick == 22) { // 2回目の斬撃
            performSlash(level, player, 3, 1.5f,2.5f);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 spawnPos = calculateRelativePosition(player,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0,1,2 -> new Vector3f(0.5f, 0.5f, 7f);
            case 3 -> new Vector3f(7.2f, 3f, 7.2f);
            default -> new Vector3f().zero();
        };

        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex);
        String skill_particle = slashIndex == 3 ? AxeBlueSkillTexture() : Spia_Particle();
        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        SimpleSkillSound(level,player.position());
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0,1,2 -> new Vec3(0, 0, 30); // 1回目の斬撃
            case 3 -> new Vec3(0, 0, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        double Yaw = switch (slashIndex){
            case 1 ->10;
            case 2 ->-10;
            default -> 0;
        };
        Vec3 lookVec = rotateLookVec(player,0,Yaw);

        Vec3 relativePos = slashIndex==3 ? Vec3.ZERO : lookVec.scale(3.5);

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }



}
