package com.zoma1101.SwordSkill.swordskills.skill;

import com.zoma1101.SwordSkill.SwordSkill;
import com.zoma1101.SwordSkill.swordskills.ISkill;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class HowToUse implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        player.sendSystemMessage(Component.translatable(SwordSkill.MOD_ID+".skill.how_to_use.description2"));
    }
}

// 他のスキルクラスも同様に修正