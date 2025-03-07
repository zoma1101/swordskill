package com.zoma.SwordSkill.swordskills;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface ISkill { // 名前を変更
    void execute(Level level, ServerPlayer player,int FinalTick, int SkillID);
}