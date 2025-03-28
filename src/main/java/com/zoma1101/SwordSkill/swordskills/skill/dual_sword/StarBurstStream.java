package com.zoma1101.swordskill.swordskills.skill.dual_sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class StarBurstStream implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 4) {
            performSlash(level, player, 0, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 10) {
            performSlash(level, player, 1, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 14) {
            performSlash(level, player, 2, 0.1F,1f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 16) {
            performSlash(level, player, 3, 0.1F,1f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 22) {
            performSlash(level, player, 4, 0.1F,2.5f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        }else if (FinalTick == 24) {
            performSlash(level, player, 5, 0.1F,2.5f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 32) {
            performSlash(level, player, 6, 0.1F,2f,NomalSkillTexture());
            performSlash(level, player, 7, 0.1F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 40) {
            performSlash(level, player, 8, 0.1F,1.5f,NomalSkillTexture());
            performSlash(level, player, 9, 0.1F,1.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 45) {
            performSlash(level, player, 10, 0.1F,2f,NomalSkillTexture());
            performSlash(level, player, 11, 0.1F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            Vec3 lookVec = player.getLookAngle();
            player.setDeltaMovement(lookVec.scale(2).x,lookVec.scale(2).y,lookVec.scale(2).z);
            player.hurtMarked = true;
        }
        else if (FinalTick == 50) {
            performSlash(level, player, 12, 0.1F,2f,NomalSkillTexture());
            performSlash(level, player, 13, 0.1F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 60) {
            performSlash(level, player, 14, 0.76F,3f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 68) {
            performSlash(level, player, 15, 1.25F,4f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }

    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage,String Texture) {
        Vec3 spawnPos = calculateRelativePosition(player, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size;
        if (Simple_ParticleType.contains(Texture)) {
            size = new Vector3f(7.2f, 2f, 3f);
        }
        else if (Axe_ParticleType.contains(Texture)) {
            size = new Vector3f(8f, 2f, 8f);
        }
        else {
            size = new Vector3f(0.23f, 0.23f, 5f);
        }
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        double Yaw = switch (slashIndex){
            case 4 ->-5;
            case 14 ->-10;
            default -> 0;
        };
        Vec3 lookVec = rotateLookVec(player,Yaw,0);
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0,1 -> lookVec.scale(2.5);
            case 14 -> lookVec.scale(2.5).subtract(rightVec.scale(0.5));
            case 15 -> lookVec.scale(2.5).add(rightVec.scale(0.5));
            case 3,5 -> upVec.scale(0.5f);
            case 6,7,10,11,12,13 -> lookVec.scale(2);
            case 8,9 -> lookVec.scale(2).add(upVec.scale(0.25f));
            default -> Vec3.ZERO;
        };


        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0,2,3 -> new Vec3(-10, 0, -20);
            case 1 -> new Vec3(-10, 0, -30);
            case 4,5 -> new Vec3(-10, 0, -10);
            case 6,10 -> new Vec3(-10, 0, 45);
            case 7,11 -> new Vec3(-10, 0, 135);
            case 8 -> new Vec3(-10, 0, -45);
            case 9 -> new Vec3(-10, 0, -135);
            case 12 -> new Vec3(-10, 0, 30); // 1回目の斬撃
            case 13 -> new Vec3(-10, 0, 150); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

}
