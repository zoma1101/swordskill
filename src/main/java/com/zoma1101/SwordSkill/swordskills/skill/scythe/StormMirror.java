package com.zoma1101.swordskill.swordskills.skill.scythe;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;
import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.AxePurpleSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class StormMirror implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            PlayerAnimation(SkillID,"move");
            Vec3 moveVec = player.getLookAngle().scale(4);
            player.setDeltaMovement(moveVec.x, moveVec.y, moveVec.z);
            player.hurtMarked = true;
        }
        else if (FinalTick == 3){
            PlayerAnimation(SkillID,"finish");
        }
        else if (FinalTick == 5) { // 1回目の斬撃
                performSlash(level, player, 0, 0.3F);
                SimpleSkillSound(level,player.position());
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
        }
         else if (FinalTick == 7) { // 2回目の斬撃
            performSlash(level, player, 1, 0.75F);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback) {
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0);
        double damage = BaseDamage(player) * 2;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex,player);
        String skill_particle = AxePurpleSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex,ServerPlayer player) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 0, 0); // 1回目の斬撃
            case 1 -> new Vec3(-10-player.getXRot()*2, 180, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

}
