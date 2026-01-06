package com.zoma1101.swordskill.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * スキル発動試行時に発生するイベント。
 * キャンセルすることで、スキルの発動を阻止できます。
 */
@Cancelable
public class SkillTryUseEvent extends PlayerEvent {
    private final String skillName;

    public SkillTryUseEvent(Player player, String skillName) {
        super(player);
        this.skillName = skillName;
    }

    public String getSkillName() {
        return skillName;
    }
}