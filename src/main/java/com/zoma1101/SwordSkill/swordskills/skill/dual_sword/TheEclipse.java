package com.zoma1101.SwordSkill.swordskills.skill.dual_sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class TheEclipse implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F,0.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 4) {
            performSlash(level, player, 1, 0.1F,0.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,0.8f,0));
            player.hurtMarked = true;
        }
        else if (FinalTick == 10) {
            performSlash(level, player, 2, 0.1F,0.75f,NomalSkillTexture());
            performSlash(level, player, 3, 0.1F,0.75f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,-0.8f,0));
            player.hurtMarked = true;
        }
        else if (FinalTick == 16) {
            performSlash(level, player, 4, 0.1F,0.4f,AxeBlueSkillTexture());
            performSlash(level, player, 5, 0.1F,0.4f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,-0.8f,0));
            player.hurtMarked = true;
        }
        else if (FinalTick == 21) {
            performSlash(level, player, 6, 0.1F,0.4f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 24) {
            performSlash(level, player, 7, 0.1F,0.4f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 34) {
            performSlash(level, player, 6, 0.1F,0.4f,AxeBlueSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 37) {
            performSlash(level, player, 7, 0.1F,0.4f,AxeBlueSkillTexture()); //10連撃
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 41) {
            performSlash(level, player, 8, 0.1F,0.75f,NomalSkillTexture());
            performSlash(level, player, 9, 0.1F,0.75f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 46) {
            performSlash(level, player, 10, 0.1F,2f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 51) {
            performSlash(level, player, 11, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 56) {
            performSlash(level, player, 12, 0.1F,1f,NomalSkillTexture()); //15
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 62) {
            performSlash(level, player, 13, 0.1F,1f,NomalSkillTexture());
            performSlash(level, player, 14, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 68) {
            performSlash(level, player, 15, 0.1F,1f,NomalSkillTexture());
            performSlash(level, player, 16, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 72) {
            performSlash(level, player, 17, 0.1F,1f,NomalSkillTexture()); //20
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 74) {
            performSlash(level, player, 18, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 80) {
            performSlash(level, player, 19, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 81) {
            performSlash(level, player, 20, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 90) {
            performSlash(level, player, 2, 0.1F,1f,NomalSkillTexture());
            performSlash(level, player, 3, 0.1F,1f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 100) {
            performSlash(level, player, 10, 0.8F,3f,Spia_Particle());
            SimpleSkillSound(level,player.position());
            Vec3 lookVec = player.getLookAngle();
            player.setDeltaMovement(lookVec.scale(0.5).x,lookVec.scale(0.5).y,lookVec.scale(0.5).z);
            player.hurtMarked = true;
        }
        else if (FinalTick == 105) {
            performSlash(level, player, 10, 0.8F,3f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage,String Texture) {
        Vec3 spawnPos = calculateRelativePosition(player, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 4,5,6,7 -> new Vector3f(6f, 2f, 6f);
            case 10 -> new Vector3f(0.5f, 0.5f, 5f);
            default -> new Vector3f(7.2f, 3f, 1.4f);
        };
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
            case 0,11,12,17,18,19,20 -> lookVec.scale(2.5);
            case 1 -> lookVec.scale(2.5).add(upVec.scale(1.5f));
            case 2,13,16 -> lookVec.scale(2.5).add(rightVec.scale(0.25));
            case 3,14,15 -> lookVec.scale(2.5).add(rightVec.scale(-0.25));
            case 4,5 -> lookVec.scale(1.0);
            case 10 -> lookVec.scale(3);
            default -> Vec3.ZERO;
        };


        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0,1,11 -> new Vec3(-10, 7, -80);
            case 2,3 -> new Vec3(-6,20,95);
            case 4 -> new Vec3(-10, 0, -45);
            case 5 -> new Vec3(-10, 0, -135);
            case 6,7 -> new Vec3(-10, 7, 20);
            case 8 -> new Vec3(-10, 0, 45);
            case 9 -> new Vec3(-10, 0, 135);
            case 10 -> new Vec3(-10, 0, 30);
            case 12 -> new Vec3(-20,0,5);
            case 13,14 -> new Vec3(-6,0,45);
            case 15,16 -> new Vec3(-6,0,-45);
            case 17 -> new Vec3(-10, 0, 25); // 1回目の斬撃
            case 18 -> new Vec3(-10, 0, 155); // 2回目の斬撃
            case 19 -> new Vec3(-15, 0, 0); // 2回目の斬撃
            case 20 -> new Vec3(-15, 0, 180); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

}
