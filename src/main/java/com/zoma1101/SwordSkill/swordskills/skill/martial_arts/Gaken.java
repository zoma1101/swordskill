package com.zoma1101.swordskill.swordskills.skill.martial_arts;
 
import com.zoma1101.swordskill.swordskills.BaseSkill;
import com.zoma1101.swordskill.swordskills.SkillTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
 
import java.util.List;
 
public class Gaken extends BaseSkill {
    @Override
    public void execute(Level level, ServerPlayer player, int tickCount, int SkillID) {
        // トレイルの開始とジャンプ
        if (tickCount == 0) {
            setTrailActive(player, true);
            // 上方へ軽く跳躍
        }
        if (tickCount == 1) {
            player.setDeltaMovement(player.getDeltaMovement().add(0,0.7f,0));
            player.hurtMarked = true;
        }
        if (tickCount == 7) {
            player.setDeltaMovement(player.getDeltaMovement().add(0,-0.9f,0));
            player.hurtMarked = true;
        }
 
        // 攻撃判定の生成 (空中での叩きつけ)
        if (tickCount == 11) {
            // 下方への突き（足での叩きつけ）を表現するため、横の判定範囲を広く設定 (1.8 -> 2.8)
            Vector3f size = new Vector3f(2.8f, 2.8f, 4.0f); 
            double damage = 20.0; // 固定ダメージ 20
            double knockback = getBaseKnockback(player) * 1.8;
            
            // プレイヤーの足下付近に発生させる
            Vec3 spawnPos = player.position().add(0, 1, 0);
            

            spawnAttackEffect(level, spawnPos, new Vec3(90, 0, 0), size, player, damage, knockback, 10, 
                    getNormalSkillTexture(), List.of(SkillTag.RAY, SkillTag.SHAPE_THRUST), SkillID, Vec3.ZERO, false, false);
            
            playSimpleSkillSound(level, spawnPos);
        }
 
        // トレイルの終了
        if (tickCount >= 18) {
            setTrailActive(player, false);
        }
    }
}
