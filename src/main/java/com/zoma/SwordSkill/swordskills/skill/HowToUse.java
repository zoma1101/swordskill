package com.zoma.SwordSkill.swordskills.skill;

import com.zoma.SwordSkill.main.SwordSkill;
import com.zoma.SwordSkill.swordskills.ISkill;
import com.zoma.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.zoma.SwordSkill.swordskills.SkillSound.SimpleSkillSound;
import static com.zoma.SwordSkill.swordskills.SkillTexture.NomalSkillTexture;
import static com.zoma.SwordSkill.swordskills.SkillUtils.*;

public class HowToUse implements ISkill { // インターフェースを実装
    @Override
    public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
        player.sendSystemMessage(Component.translatable(SwordSkill.MOD_ID+".skill.how_to_use.description2"));
    }
}

// 他のスキルクラスも同様に修正