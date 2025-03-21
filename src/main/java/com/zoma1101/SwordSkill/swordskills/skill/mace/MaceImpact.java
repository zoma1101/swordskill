package com.zoma1101.SwordSkill.swordskills.skill.mace;

import com.zoma1101.SwordSkill.effects.EffectRegistry;
import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.SwordSkill.server.handler.SkillExecutionManager.skillExecutions;
import static com.zoma1101.SwordSkill.swordskills.SkillSound.*;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.MaceGreen_Texture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class MaceImpact implements ISkill {
    private static float s_PosY;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) {
            player.setDeltaMovement(new Vec3(0, 3, 0));
            player.hurtMarked = true;
        }
        else if (FinalTick == 5){
            s_PosY = (float) player.position().y;
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 160));
        }
         else if (FinalTick > 5) {
            player.setDeltaMovement(new Vec3(0, -1.5, 0));
            player.hurtMarked = true;
            if (player.onGround()) {
                float E_PosY = (float) player.position().y;
                performSlash(level, player, Math.abs(s_PosY - E_PosY));
                StrongSkillSound(level, player.position());
                GodSkillSound(level, player.position());
                Vec3 moveVec = player.getLookAngle().scale(-1.5);
                player.setDeltaMovement(moveVec);
                player.hurtMarked = true;
                skillExecutions.remove(player.getUUID());

            }
        }
    }

    private void performSlash(Level level, ServerPlayer player, float attackper) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0).add(lookVec.scale(2.0));
        double damage = RushDamage(player) * 1.5f * attackper;
        double knockbackForce = BaseKnowBack(player)*1.5f;
        Vector3f size = new Vector3f(7f, 3f, 6f);
        int duration = 12;
        Vec3 Rotation = new Vec3(-30, -15, 90);
        String skill_particle = MaceGreen_Texture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
}
