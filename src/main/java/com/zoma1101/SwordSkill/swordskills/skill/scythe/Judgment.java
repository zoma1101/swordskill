package com.zoma1101.SwordSkill.swordskills.skill.scythe;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.AxePurpleSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.PurpleSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Judgment implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            performSlash(level, player, 0, 0.15F,1.5f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 5) {
            performSlash(level, player, 1, 0.15F,2f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 9) {
            performSlash(level, player, 2, 0.15F,1.5f);
            SimpleSkillSound(level,player.position());
            Vec3 moveVec = player.getLookAngle().scale(-1.5);
            player.setDeltaMovement(player.getDeltaMovement().add(moveVec));
            player.hurtMarked = true;
        } else if (FinalTick == 12) {
            performSlash(level, player, 1, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 16) {
            performSlash(level, player, 3, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 19) {
            performSlash(level, player, 4, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 22) {
            performSlash(level, player, 3, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 26) {
            performSlash(level, player, 4, 0.75F,2f);
            SimpleSkillSound(level,player.position());
            Vec3 moveVec = player.getLookAngle().scale(2.5);
            player.setDeltaMovement(moveVec);
            player.hurtMarked = true;
        } else if (FinalTick == 30) {
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            performSlash(level, player, 5, 0.75F,2f);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, double Damage) {
        Vec3 loockVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,loockVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(8f, 3f, 4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex,player);
        String skill_particle = switch (slashIndex){
            case 3,4,5 -> AxePurpleSkillTexture();
            default -> PurpleSkillTexture();
        };
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex,ServerPlayer player) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 45);
            case 1 -> new Vec3(-14, -5, 165);
            case 2 -> new Vec3(6, -5, 90);
            case 3 -> new Vec3(6, 0, 0);
            case 4 -> new Vec3(6-player.getXRot()*2, 180, 0);
            case 5 -> new Vec3(-6, 8, 45);
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        new Vec3(0, 0, 0);
        Vec3 relativePos = switch (slashIndex) {
            case 3 -> lookVec.scale(1.7);
            case 4 -> lookVec.scale(-1.7);
            default -> lookVec.scale(2.3);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }


}
