package com.zoma1101.swordskill.swordskills.skill.dual_sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class AscadeRefraction implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,0.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 4) { // 2回目の斬撃
            performSlash(level, player, 1, 0.75F,0.5f);
            SimpleSkillSound(level,player.position());
        }else if (FinalTick == 7) { // 2回目の斬撃
            performSlash(level, player, 2, 0.75F,1f);
            SimpleSkillSound(level,player.position());
        }else if (FinalTick == 9) { // 2回目の斬撃
            performSlash(level, player, 3, 0.75F,1f);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,lookVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 0, 25); // 1回目の斬撃
            case 1 -> new Vec3(-10, 0, 155); // 2回目の斬撃
            case 2 -> new Vec3(-15, 0, 0); // 2回目の斬撃
            case 3 -> new Vec3(-15, 0, 180); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0,1 -> upVec.scale(0.5).add(lookVec.scale(2));
            case 2 -> upVec.scale(0.2).add(lookVec.scale(2));
            case 3 -> upVec.scale(-0.2).add(lookVec.scale(2));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

}
