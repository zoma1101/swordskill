package com.zoma1101.swordskill.swordskills.skill.katana;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillSound.StrongSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Hiougi implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 6) { // 1回目の斬撃
            performSlash(level, player, 0, 0.2F,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
            move(player,0.3);
        } else if (FinalTick == 10) { // 2回目の斬撃
            performSlash(level, player, 1, 0.2F,AxeRedSkillTexture());
            SimpleSkillSound(level,player.position());
            move(player,0.3);
        } else if (FinalTick == 16) { // 2回目の斬撃
            performSlash(level, player, 2, 0.5F,Spia_Particle_SoftRed());
            StrongSkillSound(level,player.position());
            move(player,1);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, String particle) {
        Vec3 lookVec = player.getLookAngle();
        float Pos =slashIndex == 2 ? 3.5f : 2f;
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(Pos));
        double damage = BaseDamage(player) * 2.5f;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = calculateScale(slashIndex);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 5, 45); // 1回目の斬撃
            case 1 -> new Vec3(10, 5, 135); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vector3f calculateScale(int slashIndex) {
        return switch (slashIndex) {
            case 0,1 -> new Vector3f(7.2f, 3f, 7.2f); // 1回目の斬撃
            case 2 -> new Vector3f(0.75f,0.75f,4f); // 2回目の斬撃
            default -> new Vector3f(0, 0, 0);
        };
    }


    private void move(ServerPlayer player,double moveSpeed){
        Vec3 moveVec = player.getLookAngle().scale(moveSpeed);
        player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
        player.hurtMarked = true;
        player.invulnerableTime = 15;
    }

}