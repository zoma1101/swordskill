package com.zoma1101.swordskill.swordskills.skill.claw;

import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.GreenSkillTexture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class DragonClaw implements ISkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick== 1){
            performNeil(level,player,0,0.05f,0.75f);
            performNeil(level,player,3,0.05f,0.75f);
        }
        else if (FinalTick==7){
            performNeil(level,player,6,0.15f,1.0f);
            performNeil(level,player,9,0.15f,1.0f);
            player.setDeltaMovement(0,2,0);
            player.hurtMarked = true;
        }
        else if (FinalTick==9){
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 40));
        }
    }

    private void performNeil(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage){
        performSlash(level, player, slashIndex, knockback,Damage);
        performSlash(level, player, slashIndex+1, knockback,Damage);
        performSlash(level, player, slashIndex+2, knockback,Damage);
        SimpleSkillSound(level,player.position());
    }


    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback,double Damage) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player, lookVec, slashIndex); // 相対座標を計算
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size = new Vector3f(4.2f, 3f, 1.4f);
        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex);
        String skill_particle = GreenSkillTexture();

        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,skill_particle,Vec3.ZERO);
    }
    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize(); // 右方向ベクトル
        Vec3 upVec = rightVec.cross(lookVec).normalize(); // 上方向ベクトル
        Vec3 relativePos = switch (slashIndex) {
            case 0 -> upVec.scale(0.5).add(rightVec.scale(1.25));
            case 1 -> upVec.scale(0).add(rightVec.scale(1.25));
            case 2 -> upVec.scale(-0.5).add(rightVec.scale(1.25));
            case 3 -> upVec.scale(0.5).add(rightVec.scale(-1.25));
            case 4 -> upVec.scale(0).add(rightVec.scale(-1.25));
            case 5 -> upVec.scale(-0.5).add(rightVec.scale(-1.25));
            case 6 -> rightVec.scale(1.1).add(lookVec.scale(1.3));
            case 7 -> rightVec.scale(0.5).add(lookVec.scale(1.3));
            case 8 -> rightVec.scale(-0.1).add(lookVec.scale(1.3));
            case 9 -> rightVec.scale(0.1).add(lookVec.scale(1.3));
            case 10 -> rightVec.scale(-0.5).add(lookVec.scale(1.3));
            case 11 -> rightVec.scale(-1.1).add(lookVec.scale(1.3));
            default -> new Vec3(0, 0, 0);
        };

        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.75, 0); // プレイヤーの現在位置に相対座標を加算
    }
    private Vec3 calculateRotation(int slashIndex) {
        return switch (slashIndex) {
            case 0 -> new Vec3(12, 95, -170);
            case 1 -> new Vec3(-6, 95, -170);
            case 2 -> new Vec3(-12, 95, -170);
            case 3 -> new Vec3(-12, -85, 10);
            case 4 -> new Vec3(6, -85, 10);
            case 5 -> new Vec3(12, -85, 10);
            case 6 -> new Vec3(6, 5, -70);
            case 7 -> new Vec3(-6, 5, -70);
            case 8 -> new Vec3(-12, 5, -70);
            case 9 -> new Vec3(6, 5, -110);
            case 10 -> new Vec3(-6, 5, -110);
            case 11 -> new Vec3(-12, 5, -110);
            default -> new Vec3(0, 0, 0);
        };
    }
}
