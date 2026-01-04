package com.zoma1101.swordskill.swordskills.skill.sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Vertical_square implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 1, 0.1F,0.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 3) {
            performSlash(level, player, 3, 0.1F,0.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) {
            performSlash(level, player, 0, 0.75F,3.5f);
            performSlash(level, player, 1, 0.75F,3.5f);
            performSlash(level, player, 2, 0.75F,3.5f);
            performSlash(level, player, 3, 0.75F,3.5f);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle(); // プレイヤーの視線方向ベクトルを取得
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(3.7f, 1.5f, 1f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 relativePos = lookVec.scale(2);
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize().scale(0.8); // 右方向ベクトル
        Vec3 Hight = switch (slashIndex){
            case 0 -> new Vec3(0,player.getEyeHeight() * 1.3,0).add(rightVec);
            case 1 -> new Vec3(0,player.getEyeHeight() * 0.5,0).add(rightVec);
            case 2 -> new Vec3(0,player.getEyeHeight() * 1.3,0).subtract(rightVec);
            case 3 -> new Vec3(0,player.getEyeHeight() * 0.5,0).subtract(rightVec);
            default -> new Vec3(0,0,0);
        };

        return player.position().add(relativePos).add(Hight); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0, 3 -> new Vec3(-6, 0, 45);
            case 1, 2 -> new Vec3(-6, 0, -45);
            default -> new Vec3(0, 0, 0);
        };
    }

}