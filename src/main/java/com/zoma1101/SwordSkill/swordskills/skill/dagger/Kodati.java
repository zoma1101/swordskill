package com.zoma1101.swordskill.swordskills.skill.dagger;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Kodati implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 3) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 6) { // 2回目の斬撃
            performSlash(level, player, 1, 0.25F);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) { // 2回目の斬撃
            performSlash(level, player, 2, 0.75F);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.5));
        double damage = BaseDamage(player) * 0.35;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(4.5f, 3f, 2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = NomalSkillTexture();
        Vec3 Move = new Vec3(0,0,25);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Move);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-3, 0, 45);
            case 1 -> new Vec3(3, 0, 135);
            case 2 -> new Vec3(-3, 0, 5);
            default -> new Vec3(0, 0, 0);
        };
    }
}