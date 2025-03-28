package com.zoma1101.swordskill.swordskills.skill.rapier;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.Spia_Particle;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class ShootingStar implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick==1){
            move(player);
            player.invulnerableTime = 30;
        }
        else if (FinalTick==3){
            performSlash(level,player);
        }
    }
    private void move(ServerPlayer player){
        Vec3 moveVec = player.getLookAngle().scale(5);
        player.setDeltaMovement(player.getDeltaMovement().add(moveVec));
        player.hurtMarked = true;
    }
    private void performSlash(Level level, ServerPlayer player) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(5)); // 目の前2ブロック
        double damage = RushDamage(player) * 4.5f;
        double knockbackForce = BaseKnowBack(player) * 0.75f;
        Vector3f size = new Vector3f(0.5f, 0.5f, 11f);
        int duration = 12;
        Vec3 Rotation = new Vec3(0,0,20);
        SimpleSkillSound(level,spawnPos);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration, Spia_Particle(),Vec3.ZERO);
    }

}