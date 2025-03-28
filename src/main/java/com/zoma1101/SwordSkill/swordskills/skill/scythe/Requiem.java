package com.zoma1101.swordskill.swordskills.skill.scythe;

import com.zoma1101.swordskill.effects.EffectRegistry;
import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.*;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class Requiem implements ISkill {

    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick == 5) {
            performSlash(level, player, 0, 0.15F,1.5f,PurpleSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 11) {
            performSlash(level, player, 1, 0.15F,2f,PurpleSkillTexture());
            SimpleSkillSound(level,player.position());
        } else if (FinalTick == 18) {
            performSlash(level, player, 2, 0.15F,1.5f,PurpleSkillTexture());
            SimpleSkillSound(level,player.position());
            player.setDeltaMovement(player.getDeltaMovement().add(0,0.8,0));
            player.hurtMarked = true;
        } else if (FinalTick == 26) {
            performSlash(level, player, 3, 0.75F,2f,AxePurpleSkillTexture());
            performSlash(level, player, 4, 0.75F,2f,AxePurpleSkillTexture());
            SimpleSkillSound(level,player.position());
            player.addEffect(new MobEffectInstance(EffectRegistry.NO_FALL_DAMAGE.get(), 40));
        }
    }

    private void performSlash(Level level, ServerPlayer player, int slashIndex, float knockback, double Damage, String Texture) {
        Vec3 loockVec = player.getLookAngle();
        Vec3 spawnPos = calculateRelativePosition(player,loockVec,slashIndex);
        double damage = BaseDamage(player) * Damage;
        double knockbackForce = BaseKnowBack(player)*knockback;
        Vector3f size;
        if (Axe_ParticleType.contains(Texture)){
            size = new Vector3f(8f, 2f, 8f);
        } else {
            size = new Vector3f(8f, 2.5f, 4f);
        }

        int duration = 12;
        Vec3 Rotation = calculateRotation(slashIndex,player);
        spawnAttackEffect(level, spawnPos, Rotation ,size, player, damage, knockbackForce, duration,Texture,Vec3.ZERO);
    }

    private Vec3 calculateRotation(int slashIndex,ServerPlayer player) {
        return switch (slashIndex) {
            case 0 -> new Vec3(-6, 5, 145);
            case 1 -> new Vec3(-14, -5, -145);
            case 2 -> new Vec3(6, -5, -90);
            case 3 -> new Vec3(-6, 0, 0);
            case 4 -> new Vec3(6-player.getXRot()*2, 180, 0);
            default -> new Vec3(0, 0, 0);
        };
    }

    private Vec3 calculateRelativePosition(ServerPlayer player, Vec3 lookVec, int slashIndex) {
        Vec3 relativePos = switch (slashIndex) {
            case 3,4 -> Vec3.ZERO;
            default -> lookVec.scale(2.3);
        };
        return player.position().add(relativePos).add(0, player.getEyeHeight() * 0.6, 0); // プレイヤーの現在位置に相対座標を加算
    }


}
