package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class SavageFulcrum implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,1f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) { // 2回目の斬撃
            performThrust(level, player);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) { // 2回目の斬撃
            performSlash(level, player, 2, 0.75F,3f);
            SimpleSkillSound(level,player.position());
        }

    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private void performThrust(Level level, ServerPlayer player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, 1); // 相対座標を計算
        double damage = BaseDamage(player) * (float) 1.5;
        double knockbackForce = BaseKnowBack(player)* (float) 0.1;
        Vector3f size = new Vector3f(0.5f, 0.5f, 5f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(1);
        String skill_particle = Spia_Particle();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }



    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 relativePos = slashIndex == 1 ? lookVec.scale(2).add(0, -player.getEyeHeight() * 0.25, 0) : lookVec.scale(2);
        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-15, 0, 9);
            case 1 -> new Vec3(0, 0, 30);
            case 2 -> new Vec3(0, 15, -90);
            default -> new Vec3(0, 0, 0);
        };
    }

}
