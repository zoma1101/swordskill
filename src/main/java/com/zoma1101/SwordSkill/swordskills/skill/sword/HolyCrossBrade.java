package com.zoma1101.SwordSkill.swordskills.skill.sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.YellowSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class HolyCrossBrade implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 6) { // 2回目の斬撃
            performSlash(level, player, 1, 0.75F);
            SpawnParticle(level,player);// パーティクル生成
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
        double damage = BaseDamage(player) * 1.5f;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 2.4f);
        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex);
        String skill_particle = YellowSkillTexture();

        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 90); // 1回目の斬撃
            case 1 -> new Vec3(-20, 5, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private void SpawnParticle(Level level, ServerPlayer player){
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
        RandomSource random = level.getRandom();
        for (int i = 0; i < 100; i++) { // 10個のパーティクルを生成
            double x = spawnPos.x + random.nextGaussian() * 0.5; // ランダムなX座標
            double y = spawnPos.y + random.nextGaussian() * 0.5; // ランダムなY座標
            double z = spawnPos.z + random.nextGaussian() * 0.5; // ランダムなZ座標
            double vx = random.nextGaussian() * 0.1; // ランダムなX速度
            double vy = random.nextGaussian() * 0.1; // ランダムなY速度
            double vz = random.nextGaussian() * 0.1; // ランダムなZ速度
            level.addParticle(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
        }
    }

}
