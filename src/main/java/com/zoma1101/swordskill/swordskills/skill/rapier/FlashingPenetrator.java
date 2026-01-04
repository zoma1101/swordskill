package com.zoma1101.swordskill.swordskills.skill.rapier;

import com.zoma1101.swordskill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma1101.swordskill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.swordskill.swordskills.SkillTexture.FlashingPenetrator_Texture;
import static com.zoma1101.swordskill.swordskills.SkillUtils.*;

public class FlashingPenetrator implements ISkill {
    private static Vec3 StartPos;
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        if (FinalTick==1){
            move(player,5);
            player.invulnerableTime = 30;
            StartPos = player.position();
        }
        else if (FinalTick==6){
            Vec3 FinishPos = player.position();
            Vec3 Direction = FinishPos.subtract(StartPos);
            double ScaleZ = ((Math.abs(Direction.x)+Math.abs(Direction.y)+Math.abs(Direction.z))/3);
            Vec3 normalizedVec = Direction.normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(normalizedVec.z, normalizedVec.x));
            float pitch = (float) Math.toDegrees(Math.asin(normalizedVec.y));
            Vec2 MinecraftRot = new Vec2(yaw-90,pitch);
            performSlash(level,player,(float) ScaleZ,MinecraftRot,Direction);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            //skillExecutions.remove(player.getUUID());
        } else {
            move(player,2);
        }
    }
    private void move(ServerPlayer player, float Move){
        Vec3 moveVec = player.getLookAngle().scale(Move);
        player.setDeltaMovement(player.getDeltaMovement().add(moveVec));
        player.hurtMarked = true;
    }
    private void performSlash(Level level, ServerPlayer player, float ScaleZ, Vec2 Rot, Vec3 Pos) {
        Vec3 SpawnPos = StartPos.add(Pos.scale( 0.5)).add(0, player.getEyeHeight()*0.5, 0);
        double damage = RushDamage(player) * 2.5f;
        double knockbackForce = BaseKnowBack(player) * 0.75f;
        Vector3f size = new Vector3f(2.5f, 2.5f, ScaleZ*2);
        int duration = 14;
        Vec3 Rotation = new Vec3(-Rot.y-player.getXRot(),Rot.x-player.getYRot(),45);
        SimpleSkillSound(level,player.position());
        spawnAttackEffect(level, SpawnPos, Rotation ,size, player, damage, knockbackForce, duration, FlashingPenetrator_Texture(),Vec3.ZERO);
    }

}