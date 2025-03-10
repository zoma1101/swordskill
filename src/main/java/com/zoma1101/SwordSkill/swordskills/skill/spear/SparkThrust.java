package com.zoma1101.SwordSkill.swordskills.skill.spear;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class SparkThrust implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 1.25F,2f);
        } else if (FinalTick == 3) { // 2回目の斬撃
            performSlash(level, player, 1, 1.25f,2f);
        } else if (FinalTick == 5) { // 2回目の斬撃
            performSlash(level, player, 2, 1.25F,2f);
        } else if (FinalTick == 13) { // 2回目の斬撃
            performSlash(level, player, 3, 0.5f,0.5f);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = rotateLookVec(player,20);
        Vec3 spawnPos = calculateRelativePosition(player,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0,1,2 -> new Vector3f(0.75f, 0.75f, 7f);
            case 3 -> new Vector3f(7.2f, 3f, 2.4f);
            default -> new Vector3f().zero();
        };

        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex,player);
        String skill_particle = switch (slashIndex){
            case 0,1,2 -> Spia_Particle();
            case 3-> NomalSkillTexture();
            default -> "nodata";
        };
        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle);
        SimpleSkillSound(level,player.position());
    }
    private Vec3 calculateRotation(int slashIndex, ServerPlayer player) {
        return switch (slashIndex) {
            case 0,1,2 -> new Vec3(0, 0, 40); // 1回目の斬撃
            case 3 -> new Vec3(0, 0, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        double Yaw = switch (slashIndex){
            case 0 ->0;
            case 1 ->10;
            case 2 ->-10;
            default -> 0;
        };
        Vec3 lookVec = rotateLookVec(player,Yaw);
        Vec3 relativePos = switch (slashIndex) {
            case 0,1,2 -> // ^2 ^ ^
                    lookVec.scale(3);
            case 3 -> // ^-2 ^ ^
                    lookVec.scale(2);
            default -> Vec3.ZERO;
        };


        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }



}
