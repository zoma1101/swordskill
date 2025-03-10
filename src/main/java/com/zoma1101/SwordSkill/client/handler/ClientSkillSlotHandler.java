package com.zoma1101.SwordSkill.client.handler;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSkillSlotHandler {
    private static int[] skillIds = new int[5];

    public static void setSkillSlotInfo(int[] skillIds) {
        ClientSkillSlotHandler.skillIds = skillIds;
    }

    public static int[] getSkillSlotInfo() {
        return skillIds;
    }
}