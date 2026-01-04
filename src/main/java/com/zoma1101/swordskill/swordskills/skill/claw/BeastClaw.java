package com.zoma1101.swordskill.swordskills.skill.claw;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.RedSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class BeastClaw implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 3) { // 1回目の斬撃
            performNeil(level,player,0);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 4) { // 2回目の斬撃
            performNeil(level,player,3);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 7) { // 2回目の斬撃
            performNeil(level,player,6);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 9) { // 2回目の斬撃
            performNeil(level,player,9);
            SimpleSkillSound(level,player.position());
            move(player);
        }
    }

    private void performNeil(Level level, ServerPlayer player,int slashIndex){
        performSlash(level, player, slashIndex);
        performSlash(level, player, slashIndex+1);
        performSlash(level, player, slashIndex+2);
        SimpleSkillSound(level,player.position());
    }


    private void performSlash(Level level, ServerPlayer player, int slashIndex) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * 0.8f;
        double knockbackForce = BaseKnowBack(player)* (float) 0.95;
        Vector3f size = new Vector3f(4.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = RedSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0,3 -> upVec.scale(0.5);
            case 1,4, 6 -> upVec.scale(0);
            case 2,5, 7 -> upVec.scale(-0.5);
            case 8 -> upVec.scale(-1);
            case 9 -> upVec.scale(0.7);
            case 10 -> upVec.scale(0.2);
            case 11 -> upVec.scale(-0.3);
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.4)); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(6, 5, 40);
            case 1 -> new Vec3(-6, 5, 40);
            case 2 -> new Vec3(-12, 5, 40);
            case 3 -> new Vec3(6, 5, 140);
            case 4 -> new Vec3(-6, 5, 140);
            case 5 -> new Vec3(-12, 5, 140);
            case 6 -> new Vec3(6, 5, 5);
            case 7 -> new Vec3(-6, 5, 5);
            case 8 -> new Vec3(-12, 5, 5);
            case 9 -> new Vec3(6, 5, 175);
            case 10 -> new Vec3(-6, 5, 175);
            case 11 -> new Vec3(-12, 5, 175);
            default -> new Vec3(0, 0, 0);
        };
    }

    private void move(ServerPlayer player){
        Vec3 moveVec = player.getLookAngle().scale(0.75);
        player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
        player.hurtMarked = true;
        player.invulnerableTime = 15;
    }

}
