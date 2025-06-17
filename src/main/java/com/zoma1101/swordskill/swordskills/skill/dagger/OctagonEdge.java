package com.zoma1101.swordskill.swordskills.skill.dagger;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class OctagonEdge implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 3) { // 1回目の斬撃
            performSlash(level, player, 0, 0.1F,1.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 6) { // 2回目の斬撃
            performSlash(level, player, 1, 0.1F,1.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 10) { // 2回目の斬撃
            performSlash(level, player, 2, 0.1F,1.5f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 14) { // 2回目の斬撃
            performSlash(level, player, 3, 0.1F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 18) { // 2回目の斬撃
            performSlash(level, player, 4, 0.75F,3f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage,String Texture) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.5));
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(5f, 3f, 2f);
        if (Spia_ParticleType.contains(Texture)){
            size = new Vector3f(0.2f, 0.2f, 4f);
        }
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 45);
            case 1 -> new Vec3(6, -5, 135);
            case 2 -> new Vec3(-6, 5, 5);
            case 3 -> new Vec3(0, 5, 79);
            case 4 -> new Vec3(0, 5, -30);
            default -> new Vec3(0, 0, 0);
        };
    }
}
