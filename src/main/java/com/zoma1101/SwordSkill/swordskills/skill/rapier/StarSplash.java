package com.zoma1101.swordskill.swordskills.skill.rapier;

import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillTexture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class StarSplash implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 2) {
            performSlash(level, player, 0, 0.05F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 6) {
            performSlash(level, player, 1, 0.05F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 9) {
            performSlash(level, player, 2, 0.05F,1f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 12) {
            performSlash(level, player, 3, 0.05F,0.65f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 16) {
            performSlash(level, player, 4, 0.05F,0.65f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 20) {
            performSlash(level, player, 5, 0.25F,1.05f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 25) {
            performSlash(level, player, 6, 0.45F,1.5f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 31) {
            performSlash(level, player, 7, 0.75F,1.5f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage,String Texture) {
        Vec3 spawnPos = calculateRelativePosition(player,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = Setsize(Texture);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(0, 10, 30);
            case 1 -> new Vec3(0, 0, 30);
            case 2 -> new Vec3(0, -10, 30);
            case 3 -> new Vec3(-5, 0, 5);
            case 4 -> new Vec3(-5, 0, 175);
            case 5 -> new Vec3(-6,0,45);
            case 6 -> new Vec3(-10, 3, 30);
            case 7 -> new Vec3(-10, -3, 30);
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        Vec2 relativePos = switch (slashIndex) {
            case 0 -> new Vec2(0, 10);
            case 2 -> new Vec2(0, -10);
            case 6 -> new Vec2(-12, 3);
            case 7 -> new Vec2(-12, -3);
            default -> new Vec2(0, 0);
        };
        Vec3 LockVec = rotateLookVec(player,relativePos.x,relativePos.y);

        return player.position().add(0, player.getEyeHeight() * 0.65, 0).add(LockVec.scale(3));
    }



    private Vector3f Setsize(String Texture){
        Vector3f size = new Vector3f();
        if (SkillTexture.Simple_ParticleType.contains(Texture)){
            size = new Vector3f(6.2f, 3f, 2.4f);
        }
        else if (SkillTexture.Spia_ParticleType.contains(Texture)){
            size = new Vector3f(0.25f, 0.25f, 5f);
        }
        else if (SkillTexture.Axe_ParticleType.contains(Texture)){
            size = new Vector3f(7.2f, 3f, 7.2f);
        }
        return size;
    }


}
