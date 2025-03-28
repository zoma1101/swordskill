package com.zoma1101.swordskill.swordskills.skill.dagger;

import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillTexture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class FadEdge implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 4) {
            performSlash(level, player, 0, 0.25F,2f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 8) {
            performSlash(level, player, 1, 0.25F,1.25f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,0.8,0));
            player.hurtMarked = true;
        } else if (FinalTick == 10) {
            player.setDeltaMovement(new Vec3(0,-1,0));
            player.hurtMarked = true;
        } else if (FinalTick == 14) {
            performSlash(level, player, 2, 0.35F,3f,NomalSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 17) {
            performSlash(level, player, 3, 0.75F,3f,NomalSkillTexture());
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
            case 0 -> new Vec3(-5, 0, 150);
            case 1 -> new Vec3(-3, 10, -80);
            case 2 -> new Vec3(-6, 5, 150);
            case 3 -> new Vec3(-12, 5, -30);
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        Vec2 relativePos = switch (slashIndex) {
            case 2 -> new Vec2(4, 0);
            case 3 -> new Vec2(-4, 0);
            default -> new Vec2(0, 0);
        };
        Vec3 LockVec = rotateLookVec(player,relativePos.x,relativePos.y);

        return player.position().add(0, player.getEyeHeight() * 0.65, 0).add(LockVec.scale(1.5));
    }



    private Vector3f Setsize(String Texture){
        Vector3f size = new Vector3f();
        if (SkillTexture.Simple_ParticleType.contains(Texture)){
            size = new Vector3f(4.2f, 3f, 1.7f);
        }
        else if (SkillTexture.Spia_ParticleType.contains(Texture)){
            size = new Vector3f(0.25f, 0.25f, 3.5f);
        }
        else if (SkillTexture.Axe_ParticleType.contains(Texture)){
            size = new Vector3f(4.2f, 2.5f, 4.2f);
        }
        return size;
    }

}