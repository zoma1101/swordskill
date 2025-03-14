package com.zoma1101.SwordSkill.swordskills.skill.two_handed_sword;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.*;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class StanHazard implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.1F,1.8 ,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 7) {
            performSlash(level, player, 1, 0.75F,1.2 ,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) {
            performSlash(level, player, 2, 0.75F,1.2 ,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 13) {
            performSlash(level, player, 3, 0.75F,2 ,Spia_Particle_SoftRed());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 21) {
            performSlash(level, player, 4, 0.75F,3 ,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, double Damage, String Texture) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = calculateScale(slashIndex);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-11, 5, 45);
            case 1 -> new Vec3(-7, 5, 150);
            case 2 -> new Vec3(-7, 5, 30);
            case 3 -> new Vec3(6, 5, -30);
            case 4 -> new Vec3(6, 5, 90);
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vector3f calculateScale(int slashIndex) {
        return switch (slashIndex) {
            case 3 -> new Vector3f(0.75f, 0.75f, 4f);
            default -> new Vector3f(6.4f, 3f, 6.4f);
        };
    }
}
