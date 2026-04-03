package com.zoma1101.swordskill.swordskills.skill.rapier;
 
import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
 
import java.util.List;
 
public class Linear extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        // リニアーは単発の強力な突き
        if (FinalTick == 0) {
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position()
                    .add(0, player.getEyeHeight() * 0.75, 0)
                    .add(lookVec.scale(2.25));
 
            double damage = getBaseDamage(player) * 2.5;
            double knockbackForce = getBaseKnockback(player) * 0.5;
            
            // 突きエフェクトとして RAY と SHAPE_THRUST を付与
            // サイズを少し大きくして迫力を出す
            Vector3f size = new Vector3f(1.0f, 1.0f, 5.5f);
            int duration = 12;
            Vec3 Rotation = new Vec3(0, 0, 45); // 対角線状の傾き
            
            spawnAttackEffect(level, spawnPos, Rotation, size, player, damage, knockbackForce, duration, 
                    getNormalSkillTexture(), List.of(SkillTag.RAY, SkillTag.SHAPE_THRUST), SkillID, Vec3.ZERO, false);
            
            playSimpleSkillSound(level, spawnPos);
        }
    }
}
