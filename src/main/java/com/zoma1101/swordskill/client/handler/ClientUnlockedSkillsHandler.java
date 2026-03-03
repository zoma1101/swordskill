package com.zoma1101.swordskill.client.handler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientUnlockedSkillsHandler {
    private static final Set<Integer> unlockedSkills = new HashSet<>();

    public static void updateUnlockedSkills(List<Integer> skills) {
        unlockedSkills.clear();
        unlockedSkills.addAll(skills);
    }

    public static boolean isUnlocked(int skillId) {
        return unlockedSkills.contains(skillId);
    }

    public static void reset() {
        unlockedSkills.clear();
    }
}
