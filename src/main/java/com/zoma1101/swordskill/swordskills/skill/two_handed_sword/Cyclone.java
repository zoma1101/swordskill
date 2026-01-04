package com.zoma1101.swordskill.swordskills.skill.two_handed_sword;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.AxeRedSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Cyclone implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 1) { // 1回目の斬撃
            performSlash(level, player, 0);
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 4) { // 2回目の斬撃
            performSlash(level, player, 1);
            SimpleSkillSound(level,player.position());
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex) {
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0);
        double damage = BaseDamage(player) * 1.5;
        double knockbackForce = BaseKnowBack(player)* (float) 0.1;
        Vector3f size = new Vector3f(7.2f, 3f, 7.2f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(player,slashIndex);
        String skill_particle = AxeRedSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }

    private Vec3 calculateRotation(Player player, int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-10, 0, 0); // 1回目の斬撃
            case 1 -> new Vec3(10-player.getXRot()*2, 180, 0); // 2回目の斬撃
            default -> new Vec3(0, 0, 0);
        };
    }

}
