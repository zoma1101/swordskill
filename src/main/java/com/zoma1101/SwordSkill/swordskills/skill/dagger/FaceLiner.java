package com.zoma1101.SwordSkill.swordskills.skill.dagger;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import com.zoma1101.SwordSkill.swordskills.SkillTexture;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class FaceLiner implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.15F,2f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 3) {
            performSlash(level, player, 1, 0.15F,2f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) {
            performSlash(level, player, 2, 0.15F,2f,Spia_Particle());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 12) { //武器を逆手持ちに直し攻撃
            performSlash(level, player, 3, 0.75F,3f,Spia_Particle());
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
        Vec3 Move = new Vec3(0,0,1.5);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Move);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(0, 15, 30);
            case 2 -> new Vec3(0, -15, -30);
            case 3 -> new Vec3(5, 0, -30);
            default -> new Vec3(0, 0, -30);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, int slashIndex) {
        Vec2 relativePos = switch (slashIndex) {
            case 0 -> new Vec2(0, 15);
            case 2 -> new Vec2(0, -15);
            case 3 -> new Vec2(0, -6);
            default -> new Vec2(0, 0);
        };
        Vec3 LockVec = rotateLookVec(player,relativePos.x,relativePos.y);

        return player.position().add(0, player.getEyeHeight() * 0.65, 0).add(LockVec.scale(1));
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