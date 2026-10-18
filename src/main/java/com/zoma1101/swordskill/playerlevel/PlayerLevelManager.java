package com.zoma1101.swordskill.playerlevel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.payload.SyncPlayerLevelPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * プレイヤーレベル（SAO でいうキャラクターレベル）とステータスの管理。
 *
 * <p>レベルはプレイヤーごとに 1〜{@code playerLevelMax} の範囲で保持し、
 * {@code swordskill_data/<uuid>.json} に保存する。
 *
 * <p>レベルアップで得られるのは <b>ステータスポイント</b>（{@code statPointsPerLevel}）と
 * 最大 SP のみで、レベルそのものは HP も火力も増やさない。火力は STR、移動速度は AGI に
 * 割り振った分だけ伸びる（{@link CombatStats} の独自計算式）。防御は防具（バニラの防御力）が担当する。
 */
public final class PlayerLevelManager {

    public static final String LEVEL_KEY = "player_level";
    public static final String XP_KEY = "player_level_xp";
    public static final String STR_KEY = "player_str";
    public static final String AGI_KEY = "player_agi";

    private static final ResourceLocation SP_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "level_bonus_sp");
    private static final ResourceLocation MOVE_SPEED_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "agility_bonus_move_speed");

    private static final Map<UUID, LevelData> DATA = new HashMap<>();

    private PlayerLevelManager() {
    }

    private record LevelData(int level, long xp, int str, int agility) {

        int spentPoints() {
            return Math.max(0, str) + Math.max(0, agility);
        }
    }

    /** 割り振りの結果。 */
    public enum AllocateResult {
        SUCCESS,
        NO_POINTS,
        INVALID
    }

    // --- 設定 -----------------------------------------------------------

    public static boolean enabled() {
        return ServerConfig.levelSystemEnabled.get();
    }

    public static int maxLevel() {
        return Math.max(1, ServerConfig.playerLevelMax.get());
    }

    public static int xpPerKill() {
        return Math.max(0, ServerConfig.levelXpPerKill.get());
    }

    public static int pointsPerLevel() {
        return Math.max(0, ServerConfig.statPointsPerLevel.get());
    }

    /** レベル {@code level} から {@code level + 1} へ進むのに必要な XP。 */
    public static long xpToNext(int level) {
        double base = Math.max(1.0D, ServerConfig.levelXpBase.get());
        double exponent = Math.max(1.0D, ServerConfig.levelXpExponent.get());
        double value = base * Math.pow(Math.max(1, level), exponent);
        return Math.max(1L, Math.round(value));
    }

    /** レベルによる最大 SP ボーナス。 */
    public static int spBonusFor(int level) {
        double cap = Math.max(0.0D, ServerConfig.levelSpBonusCap.get());
        double perLevel = Math.max(0.0D, ServerConfig.levelSpPerLevel.get());
        return (int) Math.round(Math.min(cap, Math.max(0, level - 1) * perLevel));
    }

    // --- キャッシュ -----------------------------------------------------

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(maxLevel(), level));
    }

    private static int readInt(JsonObject data, String key) {
        if (!data.has(key)) {
            return 0;
        }
        try {
            JsonElement stored = data.get(key);
            return Math.max(0, stored.getAsInt());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static LevelData readFromDisk(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        int level = 1;
        long xp = 0L;
        if (playerData.has(LEVEL_KEY)) {
            try {
                level = clampLevel(playerData.get(LEVEL_KEY).getAsInt());
            } catch (RuntimeException ignored) {
                level = 1;
            }
        }
        if (playerData.has(XP_KEY)) {
            try {
                xp = Math.max(0L, playerData.get(XP_KEY).getAsLong());
            } catch (RuntimeException ignored) {
                xp = 0L;
            }
        }
        return new LevelData(level, xp, readInt(playerData, STR_KEY), readInt(playerData, AGI_KEY));
    }

    private static LevelData data(ServerPlayer player) {
        return DATA.computeIfAbsent(player.getUUID(), ignored -> readFromDisk(player));
    }

    private static void put(ServerPlayer player, LevelData value) {
        DATA.put(player.getUUID(), value);
        save(player, value);
    }

    private static void save(ServerPlayer player, LevelData data) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        playerData.addProperty(LEVEL_KEY, data.level());
        playerData.addProperty(XP_KEY, data.xp());
        playerData.addProperty(STR_KEY, Math.max(0, data.str()));
        playerData.addProperty(AGI_KEY, Math.max(0, data.agility()));
        DataManager.savePlayerData(player, playerData);
    }

    /** ディスクから読み込む。 */
    public static void loadPlayer(ServerPlayer player) {
        DATA.put(player.getUUID(), readFromDisk(player));
    }

    public static void unloadPlayer(UUID playerId) {
        DATA.remove(playerId);
    }

    // --- 取得 -----------------------------------------------------------

    public static int getLevel(ServerPlayer player) {
        return data(player).level();
    }

    public static long getXp(ServerPlayer player) {
        return data(player).xp();
    }

    /** 次のレベルまでに必要な XP。最大レベルなら 0。 */
    public static long getXpToNext(ServerPlayer player) {
        int level = getLevel(player);
        return level >= maxLevel() ? 0L : xpToNext(level);
    }

    public static int getStat(ServerPlayer player, StatType type) {
        LevelData data = data(player);
        return switch (type) {
            case STR -> data.str();
            case AGI -> data.agility();
        };
    }

    /** 割り振り済みの合計ポイント。 */
    public static int getSpentPoints(ServerPlayer player) {
        return data(player).spentPoints();
    }

    /** レベルから得られる総ポイント。 */
    public static int getTotalPoints(ServerPlayer player) {
        return pointsPerLevel() * Math.max(0, data(player).level() - 1);
    }

    /** 未使用のステータスポイント。 */
    public static int getAvailablePoints(ServerPlayer player) {
        return Math.max(0, getTotalPoints(player) - getSpentPoints(player));
    }

    // --- レベル変更 -----------------------------------------------------

    /** レベルを設定する。戻り値は設定後の値。 */
    public static int setLevel(ServerPlayer player, int level) {
        LevelData current = data(player);
        int clamped = clampLevel(level);
        put(player, new LevelData(clamped, current.xp(), current.str(), current.agility()));
        applyRewards(player);
        syncToClient(player);
        return clamped;
    }

    /** レベルを加算する。戻り値は加算後の値。 */
    public static int addLevel(ServerPlayer player, int delta) {
        return setLevel(player, getLevel(player) + delta);
    }

    /**
     * XP を加算し、必要なら複数回レベルアップする。
     *
     * @return 上がったレベル数
     */
    public static int addXp(ServerPlayer player, long amount) {
        if (!enabled() || amount <= 0L) {
            return 0;
        }
        LevelData current = data(player);
        int level = current.level();
        long xp = current.xp() + amount;
        int max = maxLevel();
        int gained = 0;
        while (level < max) {
            long need = xpToNext(level);
            if (xp < need) {
                break;
            }
            xp -= need;
            level++;
            gained++;
        }
        if (level >= max) {
            xp = 0L;
        }
        put(player, new LevelData(level, xp, current.str(), current.agility()));
        if (gained > 0) {
            applyRewards(player);
            notifyLevelUp(player, level, gained);
        }
        syncToClient(player);
        return gained;
    }

    private static void notifyLevelUp(ServerPlayer player, int level, int gained) {
        player.displayClientMessage(Component.translatable(
                "message." + SwordSkill.MOD_ID + ".player_level_up",
                level, gained * pointsPerLevel(), getAvailablePoints(player)), true);
    }

    // --- ステータス割り振り ---------------------------------------------

    /** ステータスポイントを {@code type} に {@code amount} だけ割り振る。 */
    public static AllocateResult allocate(ServerPlayer player, StatType type, int amount) {
        if (type == null || amount <= 0) {
            return AllocateResult.INVALID;
        }
        int available = getAvailablePoints(player);
        if (available <= 0) {
            return AllocateResult.NO_POINTS;
        }
        int toAdd = Math.min(amount, available);
        setStat(player, type, getStat(player, type) + toAdd);
        return AllocateResult.SUCCESS;
    }

    /** ステータスを直接設定する（管理コマンド用）。戻り値は設定後の値。 */
    public static int setStat(ServerPlayer player, StatType type, int amount) {
        int value = Math.max(0, amount);
        LevelData current = data(player);
        LevelData updated = switch (type) {
            case STR -> new LevelData(current.level(), current.xp(), value, current.agility());
            case AGI -> new LevelData(current.level(), current.xp(), current.str(), value);
        };
        put(player, updated);
        applyRewards(player);
        syncToClient(player);
        return value;
    }

    // --- 撃破による XP --------------------------------------------------

    /** モンスター撃破時にプレイヤーレベル XP を加算する。 */
    public static void awardForKill(ServerPlayer player, LivingEntity killed) {
        int amount = xpPerKill();
        if (amount <= 0 || killed == null || killed == player || !enabled()) {
            return;
        }
        if (ServerConfig.levelXpFromMonstersOnly.get()
                && killed.getType().getCategory() != MobCategory.MONSTER) {
            return;
        }
        addXp(player, amount);
    }

    // --- 報酬 -----------------------------------------------------------

    /** レベルとステータスに応じた最大 SP / 移動速度を属性へ反映する。 */
    public static void applyRewards(ServerPlayer player) {
        boolean on = enabled();
        int level = data(player).level();

        double spBonus = on ? spBonusFor(level) : 0.0D;
        double speedBonus = on ? CombatStats.agilityMoveSpeedBonus(player) : 0.0D;

        AttributeInstance maxSp = player.getAttribute(SwordSkillAttribute.MAX_SP);
        if (maxSp != null) {
            maxSp.removeModifier(SP_BONUS_ID);
            if (spBonus > 0.0D) {
                maxSp.addOrReplacePermanentModifier(new AttributeModifier(
                        SP_BONUS_ID, spBonus, AttributeModifier.Operation.ADD_VALUE));
            }
        }

        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(MOVE_SPEED_BONUS_ID);
            if (speedBonus > 0.0D) {
                moveSpeed.addOrReplacePermanentModifier(new AttributeModifier(
                        MOVE_SPEED_BONUS_ID, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
    }

    // --- クライアント同期 -----------------------------------------------

    public static void syncToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildSyncPayload(player));
    }

    /** クライアント表示用の同期パケットを組み立てる。 */
    public static SyncPlayerLevelPayload buildSyncPayload(ServerPlayer player) {
        LevelData data = data(player);
        int level = data.level();
        long next = level >= maxLevel() ? 0L : xpToNext(level);
        return new SyncPlayerLevelPayload(
                level, maxLevel(), data.xp(), next,
                enabled() ? spBonusFor(level) : 0,
                data.str(), data.agility(),
                getAvailablePoints(player), getTotalPoints(player), getSpentPoints(player));
    }
}
