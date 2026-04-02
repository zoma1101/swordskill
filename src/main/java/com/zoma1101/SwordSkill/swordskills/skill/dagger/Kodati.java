package com.zoma1101.swordskill.swordskills.skill.dagger;
import com.zoma1101.swordskill.swordskills.BaseSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Kodati extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        if (tickCount == 3) {
            performSlash(level, player, 0, 0.8, 0.1, SkillID);
            swingArm(player);
        } else if (tickCount == 6) {
            performSlash(level, player, 1, 0.8, 0.25, SkillID);
            swingArm(player);
        } else if (tickCount == 11) {
            performSlash(level, player, 2, 1.2, 0.75, SkillID);
            swingArm(player);
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, double damageMult, double knockbackMult, int SkillID) {
        Vec3 rotation = calculateRotation(slashIndex);
        Vector3f size = new Vector3f(1.5f, 2f, 2f);
        Vec3 move = new Vec3(0, 0, 15); // 前方に飛翔する速度
        
        spawnMovingRelativeSlash(level, player, 1.5, 0.0, 0.0, rotation, size, damageMult, knockbackMult, SkillID, move);
    }

    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-3, 0, 45);
            case 1 -> new Vec3(3, 0, 135);
            case 2 -> new Vec3(-3, 0, 5);
            default -> new Vec3(0, 0, 0);
        };
    }
}