package com.zoma1101.SwordSkill.swordskills.skill.axe;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class RyuukoSouhaku implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F,0.75f);
            StrongSkillSound(level,player.position());
        }
        else if (FinalTick == 6) {
            performSlash(level, player, 1, 0.1F,0.75f);
            StrongSkillSound(level,player.position());
        }
        else if (FinalTick == 11) {
            performSlash(level, player, 2, 0.1F,1.75f);
            StrongSkillSound(level,player.position());
        }
        else if (FinalTick == 15) {
            performSlash(level, player, 3, 0.1F,2f);
            StrongSkillSound(level,player.position());
        }
        else if (FinalTick == 20) {
            performSlash(level, player, 4, 2.0F,2f);
            StrongSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level,ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle(); // プレイヤーの視線方向ベクトルを取得
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;

        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0,1,4 -> new Vector3f(7.2f, 3f, 7.2f);
            case 2 -> new Vector3f(9f, 2f, 9f);
            case 3 -> new Vector3f(1f, 1f, 8f);
            default -> new Vector3f().zero();
        };
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = switch (slashIndex){
            case 0,1,2 -> AxeGreenSkillTexture();
            case 3 -> Spia_Particle_AxeGreen();
            case 4 -> AxeKingSkillTexture();
            default -> "nodata";
        };

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle);
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0,1 -> // ^2 ^ ^
                    lookVec.scale(2);
            case 2,4 -> // ^-2 ^ ^
                    lookVec.scale(2.5);
            case 3 -> // ^-2 ^ ^
                    lookVec.scale(2.5).add(0, -player.getEyeHeight() * 0.25, 0);
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 0, 35);
            case 1 -> new Vec3(-6, 0, -35);
            case 2 -> new Vec3(-20, 0, 5);
            case 3 -> new Vec3(0, 0, 40);
            case 4 -> new Vec3(-6,20,95);
            default -> new Vec3(0, 0, 0);
        };
    }
}
