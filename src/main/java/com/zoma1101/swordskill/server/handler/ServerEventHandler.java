package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.config.ServerConfig;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.SkillDataLoader;
import com.zoma1101.swordskill.data.WeaponData;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.payload.SkillSlotInfoPayload;
import com.zoma1101.swordskill.payload.SyncSPPayload;
import com.zoma1101.swordskill.payload.SyncDataSkillsPayload;
import com.zoma1101.swordskill.proficiency.ProficiencyManager;
import com.zoma1101.swordskill.playerlevel.PlayerLevelManager;
import com.zoma1101.swordskill.skilllevel.SkillLevelManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.zoma1101.swordskill.data.SkillDataFetcher;
import com.zoma1101.swordskill.payload.SyncUnlockedSkillsPayload;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.zoma1101.swordskill.data.WeaponTypeUtils.setWeaponType;

@EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Map<UUID, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<UUID, ItemStack> offHandItems = new HashMap<>();
    /** 最後にクライアントへ送った武器情報。耐久値が減っただけの変化で再送しないための比較用。 */
    private static final Map<UUID, WeaponData> lastSentWeaponData = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) { // 型チェックとキャストを同時に行う
            UUID playerId = player.getUUID();
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            ItemStack cachedMainHand = mainHandItems.get(playerId);
            ItemStack cachedOffHand = offHandItems.get(playerId);

            // プレイヤーが初めてTickされる場合、またはアイテムが変更された場合
            // （ItemStack.matches は耐久値も比較するため、攻撃のたびにここへ入る）
            if (cachedMainHand == null || cachedOffHand == null
                    || !ItemStack.matches(mainHandItem, cachedMainHand)
                    || !ItemStack.matches(offHandItem, cachedOffHand)) {

                setWeaponType(player);
                // 耐久値が減っただけなら武器種・武器名は変わらない。
                // 変わったときだけクライアントへ送り、毎ヒットのファイル読み込みとパケットを避ける。
                sendSkillSlotInfoIfWeaponChanged(player);
            }
            mainHandItems.put(playerId, mainHandItem.copy());
            offHandItems.put(playerId, offHandItem.copy());

            // SP回復 (時間経過)
            if (player.tickCount % 10 == 0) { // 10チック(0.5秒)ごとに同期
                updateSP(player, true);
                com.zoma1101.swordskill.smithing.SmithingEquipmentHandler.update(player);
            } else {
                updateSP(player, false);
            }
        }
    }

    private static void updateSP(ServerPlayer player, boolean sync) {
        double maxSP = player.getAttributeValue(SwordSkillAttribute.MAX_SP);
        double regen = player.getAttributeValue(SwordSkillAttribute.SP_REGEN);
        double currentSP = player.getPersistentData().getDouble("SS_CurrentSP");

        if (currentSP < maxSP) {
            // regenは1秒あたりの回復量(1.0/20.0 = 0.05/tick)
            double newSP = Math.min(maxSP, currentSP + regen);
            player.getPersistentData().putDouble("SS_CurrentSP", newSP);
            
            // 前回同期時との差分が1.0以上、または満タンになった時に同期
            double lastSyncSP = player.getPersistentData().getDouble("SS_LastSyncSP");
            if (sync || Math.abs(newSP - lastSyncSP) >= 1.0 || newSP >= maxSP) {
                PacketDistributor.sendToPlayer(player, new SyncSPPayload(newSP, maxSP));
                player.getPersistentData().putDouble("SS_LastSyncSP", newSP);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 鍔迫り合い中の両者間のダメージは無効。決着は自動判定だけで行う。
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity clashAttacker
                && SkillClashManager.isSameClash(clashAttacker.getUUID(), event.getEntity().getUUID())) {
            event.setCanceled(true);
            return;
        }
        // 鍛造武器: 武器記憶（傾向補正・クリティカル）とレベル差補正（与ダメージ）
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            net.minecraft.world.item.ItemStack weapon = attacker.getMainHandItem();
            boolean forged = com.zoma1101.swordskill.smithing.SmithingCombat.isForgedWeapon(weapon);
            com.zoma1101.swordskill.smithing.WeaponMemoryCategory category =
                    com.zoma1101.swordskill.smithing.WeaponMemoryCategory.NORMAL;
            boolean critical = false;
            if (forged) {
                category = com.zoma1101.swordskill.smithing.SmithingCombat.currentCategory();
                critical = com.zoma1101.swordskill.smithing.SmithingCombat.rollCritical(attacker, weapon);
                double memoryMultiplier = com.zoma1101.swordskill.smithing.SmithingCombat
                        .offenseMultiplier(weapon, category, critical);
                if (memoryMultiplier != 1.0D) {
                    event.setAmount((float) Math.max(0.0D, event.getAmount() * memoryMultiplier));
                }
            }

            // レベル差補正: レベルを持つ敵に対してだけ作用する
            double levelFactor = com.zoma1101.swordskill.playerlevel.CombatStats.levelDiffAttackFactor(
                    PlayerLevelManager.getLevel(attacker),
                    com.zoma1101.swordskill.playerlevel.EnemyLevel.get(event.getEntity()));
            if (levelFactor != 1.0D) {
                event.setAmount((float) Math.max(0.0D, event.getAmount() * levelFactor));
            }

            if (forged) {
                com.zoma1101.swordskill.smithing.SmithingCombat.recordDealt(weapon, category,
                        event.getAmount(), critical,
                        com.zoma1101.swordskill.smithing.SmithingCombat.resolveCooldown(attacker));
            }
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            double maxSP = player.getAttributeValue(SwordSkillAttribute.MAX_SP);
            double currentSP = player.getPersistentData().getDouble("SS_CurrentSP");

            // 攻撃時に2%回復
            double recovery = maxSP * 0.02;
            currentSP = Math.min(maxSP, currentSP + recovery);
            player.getPersistentData().putDouble("SS_CurrentSP", currentSP);
            
            // 攻撃時は変化が大きいため即時同期
            PacketDistributor.sendToPlayer(player, new SyncSPPayload(currentSP, maxSP));
            player.getPersistentData().putDouble("SS_LastSyncSP", currentSP);
        }

        // 鍛造武器を握っている間に受けたダメージを武器記憶に記録する
        if (event.getEntity() instanceof ServerPlayer victim) {
            boolean selfInflicted = event.getSource().getEntity() == victim;
            if (!selfInflicted) {
                net.minecraft.world.item.ItemStack weapon = victim.getMainHandItem();
                if (com.zoma1101.swordskill.smithing.SmithingCombat.isForgedWeapon(weapon)) {
                    com.zoma1101.swordskill.smithing.SmithingCombat.recordTaken(weapon, event.getAmount());
                }
            }
        }

        // カウンタースキルのチャージ中は被ダメージを軽減し、実際に受けた分を蓄積する。
        // 他の補正をすべて適用した最終ダメージに対して軽減をかける。
        if (event.getEntity() instanceof ServerPlayer counterPlayer
                && !event.isCanceled()
                && event.getSource().getEntity() != counterPlayer) {
            double damageCut = com.zoma1101.swordskill.server.handler.CounterManager
                    .damageCutOf(counterPlayer.getUUID());
            if (damageCut > 0.0D) {
                float reduced = (float) Math.max(0.0D, event.getAmount() * (1.0D - damageCut));
                com.zoma1101.swordskill.server.handler.CounterManager
                        .accumulate(counterPlayer.getUUID(), reduced);
                event.setAmount(reduced);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            mainHandItems.remove(player.getUUID());
            offHandItems.remove(player.getUUID());
            lastSentWeaponData.remove(player.getUUID());
            SkillDataFetcher.loadPlayer(player);
            WeaponTypeUtils.setWeaponType(player); // ログイン時に武器タイプを設定
            for (SyncDataSkillsPayload payload : SyncDataSkillsPayload.createBatch(
                    SwordSkillRegistry.getDataDrivenDefinitions())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
            // 鍛冶定義（素材・部品・レシピ）も配る
            com.zoma1101.swordskill.payload.SyncSmithingDefsPayload.sendTo(player);
            sendSkillSlotInfo(player); // ログイン時にスキル情報を送信
            // 武器カテゴリごとの初期スキルは、アンロックアイテムなしで最初から解放しておく
            SkillDataFetcher.grantDefaultUnlockedSkills(player);
            syncUnlockedSkills(player); // ログイン時にアンロック情報を送信
            SkillLevelManager.loadPlayer(player);
            PacketDistributor.sendToPlayer(player, SkillLevelManager.buildSyncPayload(player));
            ProficiencyManager.loadPlayer(player);
            ProficiencyManager.syncToClient(player);
            PlayerLevelManager.loadPlayer(player);
            // プレイヤーレベルの報酬 (最大HP/SP) を SP 初期化より前に反映する
            PlayerLevelManager.applyRewards(player);

            // 属性設定をコンフィグから適用
            AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP);
            if (maxSpAttr != null) {
                maxSpAttr.setBaseValue(ServerConfig.defaultMaxSp.get());
            }
            AttributeInstance spRegenAttr = player.getAttribute(SwordSkillAttribute.SP_REGEN);
            if (spRegenAttr != null) {
                // Config value is "per second", so divide by 20 for "per tick"
                spRegenAttr.setBaseValue(ServerConfig.defaultSpRegen.get() / 20.0);
            }

            // SP初期化
            if (!player.getPersistentData().contains("SS_CurrentSP")) {
                player.getPersistentData().putDouble("SS_CurrentSP",
                        player.getAttributeValue(SwordSkillAttribute.MAX_SP));
            }
            // 同期用
            player.getPersistentData().putDouble("SS_LastSyncSP", player.getPersistentData().getDouble("SS_CurrentSP"));

            // クライアントへプレイヤーレベルを送る
            PlayerLevelManager.syncToClient(player);

            // ログイン直後の SP を同期する（満タンだと updateSP では同期されないため）
            PacketDistributor.sendToPlayer(player, new SyncSPPayload(
                    player.getPersistentData().getDouble("SS_CurrentSP"),
                    player.getAttributeValue(SwordSkillAttribute.MAX_SP)));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 属性設定を再度適用
            AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP);
            if (maxSpAttr != null) {
                maxSpAttr.setBaseValue(ServerConfig.defaultMaxSp.get());
            }
            AttributeInstance spRegenAttr = player.getAttribute(SwordSkillAttribute.SP_REGEN);
            if (spRegenAttr != null) {
                // Config value is "per second", divide by 20 for "per tick"
                spRegenAttr.setBaseValue(ServerConfig.defaultSpRegen.get() / 20.0);
            }

            // 全回復
            player.getPersistentData().putDouble("SS_CurrentSP", player.getAttributeValue(SwordSkillAttribute.MAX_SP));

            // プレイヤーレベルの報酬 (最大HP/SP) を再適用する
            PlayerLevelManager.applyRewards(player);
            PlayerLevelManager.syncToClient(player);

            // リスポーン直後の SP を同期する
            PacketDistributor.sendToPlayer(player, new SyncSPPayload(
                    player.getPersistentData().getDouble("SS_CurrentSP"),
                    player.getAttributeValue(SwordSkillAttribute.MAX_SP)));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            mainHandItems.remove(player.getUUID());
            offHandItems.remove(player.getUUID());
            lastSentWeaponData.remove(player.getUUID());
            SkillDataFetcher.unloadPlayer(player.getUUID());
            SkillExecutionManager.skillExecutions.remove(player.getUUID());
            DataDrivenSkill.clearPlayerState(player.getUUID());
            com.zoma1101.swordskill.server.handler.SkillClashManager.forget(player.getUUID());
            com.zoma1101.swordskill.server.handler.CounterManager.forget(player.getUUID());
            com.zoma1101.swordskill.swordskills.SkillUtils.cleanup(player.getUUID());
            com.zoma1101.swordskill.skilllevel.SkillLevelManager.unloadPlayer(player.getUUID());
            ProficiencyManager.unloadPlayer(player.getUUID());
            PlayerLevelManager.unloadPlayer(player.getUUID());
        }
    }

    /** スキルでモンスターを撃破したときに熟練度を加算する。 */
    @SubscribeEvent
    public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer dyingPlayer) {
            com.zoma1101.swordskill.server.handler.SkillClashManager.forget(dyingPlayer.getUUID());
            com.zoma1101.swordskill.server.handler.CounterManager.forget(dyingPlayer.getUUID());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() != player) {
            ProficiencyManager.awardForKill(player, event.getEntity());
            PlayerLevelManager.awardForKill(player, event.getEntity());
            net.minecraft.world.item.ItemStack weapon = player.getMainHandItem();
            if (com.zoma1101.swordskill.smithing.SmithingCombat.isForgedWeapon(weapon)) {
                com.zoma1101.swordskill.smithing.SmithingCombat.recordKill(weapon);
            }
        }
    }

    public static void sendSkillSlotInfo(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");

        WeaponData serverWeaponData = WeaponTypeUtils.getWeaponData(player);
        String currentWeaponName = "None";
        Set<SkillData.WeaponType> currentWeaponTypes = Collections.emptySet();

        if (serverWeaponData != null) {
            currentWeaponName = (serverWeaponData.weaponName() != null) ? serverWeaponData.weaponName() : "None";
            currentWeaponTypes = (serverWeaponData.weaponType() != null) ? serverWeaponData.weaponType()
                    : Collections.emptySet();
        }

        int[] skillIds = new int[] { -1, -1, -1, -1, -1 }; // デフォルト値

        if (weaponSkills != null && !currentWeaponName.equals("None")) {
            JsonArray skillSlot = weaponSkills.getAsJsonArray(currentWeaponName);
            if (skillSlot != null) {
                int limit = Math.min(skillSlot.size(), skillIds.length);
                for (int i = 0; i < limit; i++) {
                    if (skillSlot.get(i) != null && !skillSlot.get(i).isJsonNull()) {
                        int skillId = skillSlot.get(i).getAsInt();
                        SkillData assigned = SwordSkillRegistry.SKILLS.get(skillId);
                        // 派生先や説明用エントリはスロットに残っていても無効として扱う
                        skillIds[i] = (assigned != null && assigned.isSlotAssignable()) ? skillId : -1;
                    }
                }
            }
        }
        // 次回の「武器が変わったか」判定用に、いま送った武器情報を記録する
        lastSentWeaponData.put(player.getUUID(), WeaponTypeUtils.getWeaponData(player));
        // 修正点: sendToServer から sendToPlayer へ変更
        PacketDistributor.sendToPlayer(player,
                new SkillSlotInfoPayload(skillIds, currentWeaponName, currentWeaponTypes));
    }

    /**
     * 武器種・武器名が前回送信時から変わったときだけスロット情報を送る。
     * 耐久値が減っただけの ItemStack 変化では何も送らない。
     */
    public static void sendSkillSlotInfoIfWeaponChanged(ServerPlayer player) {
        WeaponData current = WeaponTypeUtils.getWeaponData(player);
        if (current.equals(lastSentWeaponData.get(player.getUUID()))) {
            return;
        }
        sendSkillSlotInfo(player);
    }

    private static final ResourceLocation DUAL_CLAW_SPEED_ID = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
            "dual_claw_speed_bonus");

    // 攻撃速度を上げる量 (例: +50%)
    private static final double DUAL_CLAW_SPEED_BOOST = 0.5;

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        // 鍛造武器の STR 要件チェック（装備不可なら弾く）
        com.zoma1101.swordskill.smithing.SmithingEquipmentHandler.onEquipmentChange(event);

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // サーバー側でのみ処理を行う
        if (player.level().isClientSide) {
            return;
        }

        // 1. キャッシュ更新
        WeaponTypeUtils.setWeaponType(player);

        // 2. データ判定
        var weaponData = WeaponTypeUtils.getWeaponData((net.minecraft.server.level.ServerPlayer) player);
        boolean isDualClaw = weaponData.weaponName() != null && weaponData.weaponName().equals("dual_claw");

        // 3. 属性変更処理
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr == null)
            return;

        // 修正: UUIDではなくResourceLocationで取得
        AttributeModifier existingModifier = attackSpeedAttr.getModifier(DUAL_CLAW_SPEED_ID);

        if (isDualClaw) {
            if (existingModifier == null) {
                AttributeModifier modifier = new AttributeModifier(
                        DUAL_CLAW_SPEED_ID,
                        DUAL_CLAW_SPEED_BOOST,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                attackSpeedAttr.addTransientModifier(modifier);
            }
        } else {
            if (existingModifier != null) {
                // 修正: ResourceLocationを使って削除
                attackSpeedAttr.removeModifier(DUAL_CLAW_SPEED_ID);
            }
        }
    }

    public static void syncUnlockedSkills(ServerPlayer player) {
        int[] unlockedArray = SkillDataFetcher.getUnlockedSkills(player);
        java.util.List<Integer> unlockedList = new java.util.ArrayList<>();
        for (int id : unlockedArray) {
            unlockedList.add(id);
        }
        PacketDistributor.sendToPlayer(player, new SyncUnlockedSkillsPayload(unlockedList));
    }
}