package com.zoma1101.SwordSkill.swordskills.skill.katana;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.AxePinkSkillTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class SakuraMai implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0, 0.5F);
            SimpleSkillSound(level,player.position());
            move(player,1);
        } else if (FinalTick == 4) { // 2回目の斬撃
            performSlash(level, player, 1, 0.5F);
            SimpleSkillSound(level,player.position());
            move(player,1);
        } else if (FinalTick == 8) { // 2回目の斬撃
            performSlash(level, player, 0, 0.5F);
            SimpleSkillSound(level,player.position());
            move(player,1);
        } else if (FinalTick == 12) { // 2回目の斬撃
            performSlash(level, player, 1, 0.75F);
            SimpleSkillSound(level,player.position());
            move(player,1);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(1.0));
        double damage = BaseDamage(player) * 0.8f;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = AxePinkSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 45); // 1回目の斬撃
            case 1 -> new Vec3(6, 5, 135); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

    private void move(ServerPlayer player,double moveSpeed){
        Vec3 moveVec = player.getLookAngle().scale(moveSpeed);
        player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
        player.hurtMarked = true;
        player.invulnerableTime = 15;
    }

}
