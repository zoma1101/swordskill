package com.zoma1101.SwordSkill.swordskills.skill.claw;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.GreenSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Hant implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        performNeil(level,player);
    }

    private void performNeil(Level level, ServerPlayer player){
        performSlash(level, player, 0);
        performSlash(level, player, 1);
        performSlash(level, player, 2);
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
        String skill_particle = GreenSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> rightVec.scale(0.5).add(lookVec.scale(1.25));
            case 1 -> rightVec.scale(0).add(lookVec.scale(1.25));
            case 2 -> rightVec.scale(-0.5).add(lookVec.scale(1.25));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(6, 5, 70);
            case 1 -> new Vec3(-6, 5, 70);
            case 2 -> new Vec3(-12, 5, 70);
            default -> new Vec3(0, 0, 0);
        };
    }
}
