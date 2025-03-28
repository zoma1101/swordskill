package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class WorldNova implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) {
            performSlash(level, player, 0, 0.25F,1f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 5) {
            performSlash(level, player, 1, 0.25F,1f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 8) {
            performSlash(level, player, 2, 0.25F,1f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 13) {
            performSlash(level, player, 3, 0.25F,2f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 18) {
            performSlash(level, player, 4, 0.25F,1f);
            SimpleSkillSound(level,player.position());
        }
        else if (FinalTick == 19) {
            performSlash(level, player, 5, 0.25F,1f);
            SimpleSkillSound(level,player.position());
        }

        else if (FinalTick == 22) {
            performSlash(level, player, 6, 0.1F,2.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 25) {
            performThrust(level, player);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 spawnPos = calculateRelativePosition(player, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private void performThrust(Level level, ServerPlayer player) {
        Vec3 spawnPos = calculateRelativePosition(player, 7);
        double damage = BaseDamage(player) * 2f;
        double knockbackForce = BaseKnowBack(player)* 0.25;
        Vector3f size = new Vector3f(0.5f, 0.5f, 5f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(7);
        String skill_particle = Spia_Particle();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }



    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0,3 -> upVec.scale(1).add(lookVec.scale(2));
            case 1,4,6,7 -> lookVec.scale(2);
            case 2,5 -> upVec.scale(-1).add(lookVec.scale(2));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(0, 5, 30);
            case 1 -> new Vec3(-6, 5, 30);
            case 2 -> new Vec3(0, 20, 90);
            case 3 -> new Vec3(0, 5, 45);
            case 4 -> new Vec3(-6, 5, -135);
            case 5 -> new Vec3(-12, 5, 40);
            case 6 -> new Vec3(-10, 0, 0);
            default -> new Vec3(0, 0, 30);
        };
    }

}
