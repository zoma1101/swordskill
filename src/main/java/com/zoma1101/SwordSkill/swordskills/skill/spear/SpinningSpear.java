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
            performSlash(level, player, 0, 0.1F,3f);
        } else if (FinalTick == 7) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f);
        } else if (FinalTick == 10) { // 2回目の斬撃
            performSlash(level, player, 2, 1.75F,0.5f);
        } else if (FinalTick == 13) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f);
        } else if (FinalTick == 16) { // 2回目の斬撃
            performSlash(level, player, 2, 1.75F,0.5f);
        } else if (FinalTick == 20) { // 2回目の斬撃
            performSlash(level, player, 1, 1.75f,0.5f);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,lookVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0 -> new Vector3f(0.75f, 0.75f, 7f);
            case 1,2 -> new Vector3f(6f, 1.5f, 3f);
            default -> new Vector3f().zero();
        };

        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex,player);
        String skill_particle = switch (slashIndex){
            case 0 -> Spia_Particle();
            case 1,2-> NomalSkillTexture();
            default -> "nodata";
        };
        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle);
        SimpleSkillSound(level,player.position());
    }
    private Vec3 calculateRotation(int slashIndex, ServerPlayer player) {
        return switch (slashIndex) {
            case 0 -> new Vec3(0, 0, 40); // 1回目の斬撃
            case 1 -> new Vec3(-10-player.getXRot(), 70, 0); // 2回目の斬撃
            case 2 -> new Vec3(-10-player.getXRot(), -70, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> // ^2 ^ ^
                    lookVec.scale(2);
            case 1 -> // ^-2 ^ ^
                    rightVec.scale(-1.5);
            case 2 -> // ^1 ^ ^1
                    rightVec.scale(1.5);
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }



}
