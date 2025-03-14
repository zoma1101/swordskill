package com.zoma1101.SwordSkill.swordskills.skill.sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Objects;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class HowlingOctave implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 3) {
            performSlash(level, player, 1, 0.1F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) {
            performSlash(level, player, 2, 0.1F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 7) {
            performSlash(level, player, 3, 0.1F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 9) {
            performSlash(level, player, 4, 0.1F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }

        else if (FinalTick == 11) {
            performSlash(level, player, 5, 0.1F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 18) {
            performSlash(level, player, 6, 0.25F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,0.8f,0));
            player.hurtMarked = true;
        } else if (FinalTick == 25) {
            performSlash(level, player, 7, 1.75F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,-0.8f,0));
            player.hurtMarked = true;
        }

    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage, String Texture) {
        Vec3 spawnPos = calculateRelativePosition(player, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size;
        if (Objects.equals(Texture, Spia_Particle())){
            size = new Vector3f(0.5f, 0.5f, 5f);
        }
        else {
            size = new Vector3f(7.2f, 3f, 1.4f);
        }
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        double Yaw = switch (slashIndex){
            case 1 ->10;
            case 2 ->-10;
            case 3 ->5;
            case 4 ->-5;
            default -> 0;
        };
        Vec3 lookVec = rotateLookVec(player,Yaw,0);
        Vec3 relativePos = switch (slashIndex) {
            case 0,1,2,3,4 -> // ^2 ^ ^
                    lookVec.scale(2.5);
            case 5,6,7 -> // ^-2 ^ ^
                    lookVec.scale(2);
            default -> Vec3.ZERO;
        };


        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0,1,2,3,4 -> new Vec3(0, 0, 30);
            case 5 -> new Vec3(0, 0, 60);
            case 6 -> new Vec3(0, 15, -90);
            case 7 -> new Vec3(0, 15, 90);
            default -> new Vec3(0, 0, 0);
        };
    }

}
