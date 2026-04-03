package com.zoma1101.swordskill.swordskills.skill.martial_arts;
 
import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
 
import java.util.List;
 
public class Senda extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        // トレイルの開始 (0-15 tick)
        if (tickCount == 0) {
            setTrailActive(player, true);
        }
 
        // 攻撃判定の生成 (8 tick目)
        if (tickCount == 8) {
            Vector3f size = new Vector3f(1.2f, 1.2f, 1.5f); // 2.5から1.5に短縮
            double damage = 9.0; // 固定ダメージ 9
            double knockback = getBaseKnockback(player) * 1.2;
            
            // 手の届く範囲に攻撃を発生させる (距離を少し詰める)
            Vec3 lookVec = player.getLookAngle();
            Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.7, 0).add(lookVec.scale(1.4));
 
            spawnAttackEffect(level, spawnPos, Vec3.ZERO, size, player, damage, knockback, 8, 
                    getNormalSkillTexture(), List.of(SkillTag.RAY, SkillTag.SHAPE_THRUST), SkillID, Vec3.ZERO, false);
            
            playSimpleSkillSound(level, spawnPos);
            swingArm(player);
        }
 
        // トレイルの終了
        if (tickCount >= 15) {
            setTrailActive(player, false);
        }
    }
}
