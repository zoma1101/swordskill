package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.payload.SyncPlayerLevelPayload;
import com.zoma1101.swordskill.playerlevel.StatType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** サーバーから同期されたプレイヤーレベルとステータスを保持するクライアント側キャッシュ。 */
@OnlyIn(Dist.CLIENT)
public class ClientPlayerLevelHandler {

    private static int level = 1;
    private static int maxLevel = 1;
    private static long xp;
    private static long xpToNext;
    private static int spBonus;
    private static int str;
    private static int agility;
    private static int availablePoints;
    private static int totalPoints;
    private static int spentPoints;
    private static boolean synced;

    private ClientPlayerLevelHandler() {
    }

    public static void update(SyncPlayerLevelPayload payload) {
        level = Math.max(1, payload.level());
        maxLevel = Math.max(1, payload.maxLevel());
        xp = Math.max(0L, payload.xp());
        xpToNext = Math.max(0L, payload.xpToNext());
        spBonus = payload.spBonus();
        str = payload.str();
        agility = payload.agility();
        availablePoints = payload.availablePoints();
        totalPoints = payload.totalPoints();
        spentPoints = payload.spentPoints();
        synced = true;
    }

    public static boolean hasData() {
        return synced;
    }

    public static int getLevel() {
        return level;
    }

    public static int getMaxLevel() {
        return maxLevel;
    }

    public static long getXp() {
        return xp;
    }

    public static long getXpToNext() {
        return xpToNext;
    }

    public static int getSpBonus() {
        return spBonus;
    }

    public static int getAvailablePoints() {
        return availablePoints;
    }

    public static int getTotalPoints() {
        return totalPoints;
    }

    public static int getSpentPoints() {
        return spentPoints;
    }

    public static int getStat(StatType type) {
        return switch (type) {
            case STR -> str;
            case AGI -> agility;
        };
    }

    /** 現在レベルで最大に達しているか。 */
    public static boolean isMaxLevel() {
        return xpToNext <= 0L || level >= maxLevel;
    }

    public static void reset() {
        level = 1;
        maxLevel = 1;
        xp = 0L;
        xpToNext = 0L;
        spBonus = 0;
        str = 0;
        agility = 0;
        availablePoints = 0;
        totalPoints = 0;
        spentPoints = 0;
        synced = false;
    }
}
