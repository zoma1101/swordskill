package com.zoma1101.SwordSkill.swordskills.skill.rapier;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import com.zoma1101.SwordSkill.swordskills.SkillTexture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.Spia_Particle_Purple;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class MothersRosario implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 3) { // 2回目の斬撃
            performSlash(level, player, 1, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) { // 2回目の斬撃
            performSlash(level, player, 2, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 8) { // 2回目の斬撃
            performSlash(level, player, 3, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) { // 2回目の斬撃
            performSlash(level, player, 4, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 14) { // 1回目の斬撃
            performSlash(level, player, 5, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 17) { // 2回目の斬撃
            performSlash(level, player, 6, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 19) { // 2回目の斬撃
            performSlash(level, player, 7, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 21) { // 2回目の斬撃
            performSlash(level, player, 8, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 23) { // 2回目の斬撃
            performSlash(level, player, 9, 0.05F,1.5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 28) { // 2回目の斬撃
            performSlash(level, player, 10, 0.75F,5f,Spia_Particle_Purple());
            SimpleSkillSound(level,player.position());
        }

    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, float Damage,String Texture) {
        Vec3 spawnPos = calculateRelativePosition(player,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = Setsize(Texture,slashIndex);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        Vec3 Move = new Vec3(0,0,0.5);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Move);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-24, -12, 30);
            case 1 -> new Vec3(-16, -6, 30);
            case 2, 7 -> new Vec3(-8, 0, 30);
            case 3 -> new Vec3(0, 6, 30);
            case 4 -> new Vec3(8, 12, 30);
            case 5 -> new Vec3(-24, 12, 30);
            case 6 -> new Vec3(-16, 6, 30);
            case 8 -> new Vec3(0, -6, 30);
            case 9 -> new Vec3(8, -12, 30);
            default -> new Vec3(0, 0, 20);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        Vec2 relativePos = switch (slashIndex) {
            case 0 -> new Vec2(-24, -12);
            case 1 -> new Vec2(-16, -6);
            case 2, 7 -> new Vec2(-8, 0);
            case 3 -> new Vec2(0, 6);
            case 4 -> new Vec2(8, 12);
            case 5 -> new Vec2(-24, 12);
            case 6 -> new Vec2(-16, 6);
            case 8 -> new Vec2(0, -6);
            case 9 -> new Vec2(8, -12);
            default -> new Vec2(0, 0);
        };
        Vec3 LockVec = rotateLookVec(player,relativePos.x,relativePos.y);

        return player.position().add(0, player.getEyeHeight() * 0.65, 0).add(LockVec.scale(3));
    }



    private Vector3f Setsize(String Texture, int slashIndex){
        Vector3f size = new Vector3f();
        if (SkillTexture.Simple_ParticleType.contains(Texture)){
            size = new Vector3f(7.2f, 3f, 2.4f);
        }
        else if (SkillTexture.Spia_ParticleType.contains(Texture)){
            if (slashIndex == 10){
                size = new Vector3f(0.75f, 0.75f, 8f);
            } else {
                size = new Vector3f(0.25f, 0.25f, 5.5f);
            }
        }
        else if (SkillTexture.Axe_ParticleType.contains(Texture)){
            size = new Vector3f(7.2f, 3f, 7.2f);
        }
        return size;
    }


}
