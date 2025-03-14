package com.zoma1101.SwordSkill.swordskills.skill.katana;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class TumuziGuruma implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,0.8);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 4) { // 2回目の斬撃
            performSlash(level, player, 1, 0.1F,0.8);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 9) { // 2回目の斬撃
            performSlash(level, player, 0, 0.1F,0.8);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 13) { // 2回目の斬撃
            performSlash(level, player, 0, 0.75F,1.8);
            performSlash(level, player, 1, 0.75F,1.8);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, double Damage) {
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = AxeRedSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 5, 0); // 1回目の斬撃
            case 1 -> new Vec3(-10, 180, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

}
