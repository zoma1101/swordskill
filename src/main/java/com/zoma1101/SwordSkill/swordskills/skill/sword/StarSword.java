package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class StarSword implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F,0.25f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 6) {
            performSlash(level, player, 1, 0.1F,0.25f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 10) {
            performSlash(level, player, 2, 0.1F,0.25f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 15) {
            performSlash(level, player, 3, 0.1F,0.25f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 21) {
            performSlash(level, player, 4, 0.1F,0.25f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 25) {
            performSlash(level, player, 0, 1.25F,1f);
            performSlash(level, player, 1, 1.25F,1f);
            performSlash(level, player, 2, 1.25F,1f);
            performSlash(level, player, 3, 1.25F,1f);
            performSlash(level, player, 4, 1.25F,1f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 31) {
            performThrust(level, player, 5, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        }

    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = YellowSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private void performThrust(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(0.5f, 0.5f, 5f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = Spia_Particle_AxeGreen();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }



    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> lookVec.scale(2).add(0, player.getEyeHeight() * 0.75, 0);
            case 1,4 -> lookVec.scale(2).add(0, -player.getEyeHeight() * 0.25, 0);
            case 2 -> lookVec.scale(2).add(rightVec.scale(-0.7)).add(0, -player.getEyeHeight() * 0.25, 0);
            case 3 -> lookVec.scale(2).add(rightVec.scale(0.7)).add(0, -player.getEyeHeight() * 0.25, 0);
            case 5 -> lookVec.scale(2.5);

            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(10,0,2);
            case 1 -> new Vec3(-20,0,144);
            case 2 -> new Vec3(-20,0,-72);
            case 3 -> new Vec3(-20,0,72);
            case 4 -> new Vec3(-20,0,-144);
            case 5 -> new Vec3(0, 0, 30);
            default -> new Vec3(0, 0, 0);
        };
    }

}
