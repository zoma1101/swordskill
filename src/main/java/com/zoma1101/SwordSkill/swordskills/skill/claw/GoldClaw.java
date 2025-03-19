package com.zoma1101.SwordSkill.swordskills.skill.claw;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.GoldSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class GoldClaw implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick==1){
            performNeil(level,player,0);
        }
        if (FinalTick==4){
            performNeil(level,player,3);
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
        double damage = BaseDamage(player) * 1f;
        double knockbackForce = BaseKnowBack(player)* (float) 0.5;
        Vector3f size = new Vector3f(4.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = GoldSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0,3 -> upVec.scale(0.5).add(lookVec.scale(1.25));
            case 1,4 -> upVec.scale(0).add(lookVec.scale(1.25));
            case 2,5 -> upVec.scale(-0.5).add(lookVec.scale(1.25));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.65, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(6, 5, 30);
            case 1 -> new Vec3(-6, 5, 30);
            case 2 -> new Vec3(-12, 5, 30);
            case 3 -> new Vec3(6, 5, 150);
            case 4 -> new Vec3(-6, 5, 150);
            case 5 -> new Vec3(-12, 5, 150);
            default -> new Vec3(0, 0, 0);
        };
    }
}
