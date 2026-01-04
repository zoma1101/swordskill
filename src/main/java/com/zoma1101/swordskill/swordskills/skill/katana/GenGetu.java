package com.zoma1101.swordskill.swordskills.skill.katana;

import com.zoma1101.swordskill.payload.PlayAnimationPayload;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.Random;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class GenGetu implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 8){
            Random random = new Random();
            int result = random.nextBoolean() ? 0 : 1;
            if (result==0){
                PacketDistributor.sendToPlayer(player, new PlayAnimationPayload(SkillID,"upper"));
            }
            else {
                PacketDistributor.sendToPlayer(player, new PlayAnimationPayload(SkillID,"under"));
            }
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.5, 0).add(lookVec.scale(2)); // 目の前2ブロック
            double damage = RushDamage(player)*3.5f;
            double knockbackForce = BaseKnowBack(player)*0.95;
            Vector3f size = new Vector3f(7.2f, 2f, 2.4f);
            int duration = 12;
            Vec3 Rotation;

            if (result == 0){
                Rotation = new Vec3(0,0,125);
            } else {
                Rotation = new Vec3(0, 0, -35);
            }
            String skill_particle = RedSkillTexture();
            SimpleSkillSound(level,spawnPos);
            spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
        }

    }
}