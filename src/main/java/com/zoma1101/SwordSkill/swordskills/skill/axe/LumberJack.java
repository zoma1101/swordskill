package com.zoma1101.swordskill.swordskills.skill.axe;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.AxeGreenSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class LumberJack implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,2f);
            StrongSkillSound(level,player.position());
        } else if (FinalTick == 11) { // 2回目の斬撃
            performSlash(level, player, 1, 0.5f,0.75f);
            StrongSkillSound(level,player.position());
        } else if (FinalTick == 17) { // 2回目の斬撃
            performSlash(level, player, 2, 2.75F,0.75f);
            StrongSkillSound(level,player.position());
        }else if (FinalTick == 24) { // 2回目の斬撃
            performSlash(level, player, 1, 2.75F,0.75f);
            StrongSkillSound(level,player.position());
        }else if (FinalTick == 29) { // 2回目の斬撃
            performSlash(level, player, 2, 2.75F,0.75f);
            StrongSkillSound(level,player.position());
        }


    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.65, 0).add(lookVec.scale(1.0)); // 目の前2ブロック
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = switch (slashIndex){
            case 0,1 -> new Vector3f(7.2f, 3f, 7.2f);
            case 2 -> new Vector3f(9f, 2f, 9f);
            default -> new Vector3f().zero();
        };

        int duration = 12;
        Vec3 rotation = calculateRotation(slashIndex);
        String skill_particle = AxeGreenSkillTexture();
        spawnAttackEffect(level, spawnPos, rotation,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 25, 45); // 1回目の斬撃
            case 1 -> new Vec3(-10, 45, 0); // 2回目の斬撃
            case 2 -> new Vec3(-10, -45, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }
}
