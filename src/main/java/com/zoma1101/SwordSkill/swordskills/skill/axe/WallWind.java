package com.zoma1101.SwordSkill.swordskills.skill.axe;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.AxeGreenSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class WallWind implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,2f);
            StrongSkillSound(level,player.position());
        } else if (FinalTick == 6) { // 2回目の斬撃
            performSlash(level, player, 1, 2.75F,0.75f);
            StrongSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level,ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex);
        String skill_particle = AxeGreenSkillTexture();

        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle);
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> // ^ ^1 ^
                    upVec.scale(0.5).add(lookVec.scale(1.25));
            case 1 -> // ^ ^ ^
                    upVec.scale(0).add(lookVec.scale(1.25));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-40, 5, 310); // 1回目の斬撃
            case 1 -> new Vec3(-40, 5, 310); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }
}
