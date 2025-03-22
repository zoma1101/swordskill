package com.zoma1101.SwordSkill.swordskills.skill.whip;

import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static com.zoma1101.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma1101.SwordSkill.swordskills.SkillTexture.NomalWhipTexture;
import static com.zoma1101.SwordSkill.swordskills.SkillUtils.*;

public class Whip implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 spawnPos = player.position().add(0, player.getEyeHeight()*0.75, 0).add(lookVec.scale(2.0)); // 目の前2ブロック
        float damage = (float) (BaseDamage(player)*2f);
        double knockbackForce = BaseKnowBack(player)*0.5;
        float size = 2;
        int duration = 20;
        float move = 1f;
        Vec2 Rotation = new Vec2(0,0);
        String NomalWhip = NomalWhipTexture();
        SimpleSkillSound(level,spawnPos);
        spawnWhipEffect(level, spawnPos, Rotation,player, NomalWhip,duration,damage,knockbackForce,size, move);
    }
}
// 他のスキルクラスも同様に修正