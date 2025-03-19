package com.zoma1101.SwordSkill.swordskills.skill.claw;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.GreenSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class TigerClaw implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick== 1){
            performNeil(level,player,0,0.05f,0.5f);
        }
        else if (FinalTick==4){
            performNeil(level,player,3,0.15f,1.0f);
            Vec3 moveVec = player.getLookAngle().scale(3.5);
            player.setDeltaMovement(moveVec);
            player.hurtMarked = true;
        }
        else if (FinalTick==10){
            performNeil(level,player,0,0.15f,1.5f);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
        else if (FinalTick==13){
            performNeil(level,player,6,0.45f,1.5f);
        }


    }

    private void performNeil(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage){
        performSlash(level, player, slashIndex, knockback,Damage);
        performSlash(level, player, slashIndex+1, knockback,Damage);
        performSlash(level, player, slashIndex+2, knockback,Damage);
        SimpleSkillSound(level,player.position());
    }


    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(4.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = GreenSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0,6 -> upVec.scale(0.5);
            case 1,7 -> upVec.scale(0);
            case 2,8 -> upVec.scale(-0.5);
            case 3 -> rightVec.scale(0.6);
            case 4 -> rightVec.scale(0);
            case 5 -> rightVec.scale(-0.6);
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.25)); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(6, 5, 30);
            case 1 -> new Vec3(-6, 5, 30);
            case 2 -> new Vec3(-12, 5, 30);
            case 3 -> new Vec3(6, 5, 110);
            case 4 -> new Vec3(-6, 0, 110);
            case 5 -> new Vec3(-12, -5, 110);
            case 6 -> new Vec3(6, 5, 150);
            case 7 -> new Vec3(-6, 5, 150);
            case 8 -> new Vec3(-12, 5, 150);
            default -> new Vec3(0, 0, 0);
        };
    }
}
