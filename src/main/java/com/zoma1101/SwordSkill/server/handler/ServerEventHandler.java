package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponData;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillSlotInfoPacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent; // ★追加
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity; // ★追加
import net.minecraft.world.entity.LivingEntity; 
import net.minecraft.world.level.Level; 
import net.minecraft.resources.ResourceLocation; 
import com.zoma1101.swordskill.item.SampleItemRegistry; 
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

import java.util.*;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Map<ServerPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<ServerPlayer, ItemStack> offHandItems = new HashMap<>();


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            ServerPlayer player = (ServerPlayer) event.player;
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            // キャッシュから取得
            ItemStack cachedMain = mainHandItems.getOrDefault(player, ItemStack.EMPTY);
            ItemStack cachedOff = offHandItems.getOrDefault(player, ItemStack.EMPTY);

            // 変更検知
            boolean mainChanged = !ItemStack.matches(mainHandItem, cachedMain);
            boolean offChanged = !ItemStack.matches(offHandItem, cachedOff);

            if (mainChanged || offChanged) {
                // 変更があった場合のみ処理を行う
                WeaponTypeUtils.setWeaponType(player);

                mainHandItems.put(player, mainHandItem.copy());
                offHandItems.put(player, offHandItem.copy());

                sendSkillSlotInfo(player);
            }
        }
    }

    // ★修正: 死亡時のデータ引き継ぎ処理
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 次元移動であっても死亡であってもデータを引き継ぐ
        // Forgeの仕様上、Cloneイベント時点ではOriginalのCapabilityが無効化されていることがあるためreviveする
        event.getOriginal().reviveCaps();

        event.getOriginal().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(oldSkills -> 
            event.getEntity().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(newSkills -> {
                newSkills.copyFrom(oldSkills);
            })
        );
    }

    // ★追加: リスポーン時の同期処理
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 解析時にクライアントへ全データを同期
            applySPConfig(player);
            sendSkillSlotInfo(player);
            
            // ★重要: 習得済みスキルのリストも同期し直す
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                int[] unlockedArray = skills.getUnlockedSkills().stream().mapToInt(i -> i).toArray();
                NetworkHandler.INSTANCE.sendTo(
                    new com.zoma1101.swordskill.network.toClient.UnlockedSkillsResponsePacket(unlockedArray, skills.isMartialArtsUnlocked()),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
                );
                
                // ★追加: リスポーン（次元移動）時にもトレイルの設定状態を同期
                NetworkHandler.INSTANCE.sendTo(
                    new com.zoma1101.swordskill.network.toClient.SyncTrailConfigPacket(skills.isTrailEnabled()),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
                );
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // ログイン時にもサーバー側の武器タイプを設定し、クライアントに情報を送信
            // configから属性のベース値を設定
            applySPConfig(player);
            sendSkillSlotInfo(player);

            // ★追加: ログイン時にもトレイルの設定状態を同期
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                NetworkHandler.INSTANCE.sendTo(
                    new com.zoma1101.swordskill.network.toClient.SyncTrailConfigPacket(skills.isTrailEnabled()),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
                );
            });

            // ★追加: 既存のプレイヤーを含め、初回ログイン時に強制的にトレイルをONにする (Migration)
            player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
                if (!skills.isMigrated()) {
                    skills.setTrailEnabled(true);
                    skills.setMigrated(true);
                }
                
                // トレイルの設定状態をクライアントに同期
                NetworkHandler.INSTANCE.sendTo(
                    new com.zoma1101.swordskill.network.toClient.SyncTrailConfigPacket(skills.isTrailEnabled()),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
                );
            });

            // 念のため初期アイテム状態を記録
            mainHandItems.put(player, player.getMainHandItem().copy());
            offHandItems.put(player, player.getOffhandItem().copy());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // アイテム履歴のMapからも削除してメモリリーク防止
            mainHandItems.remove(player);
            offHandItems.remove(player);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 攻撃者がプレイヤーであり、かつソードスキルのエンティティ経由の攻撃でない（通常攻撃）の場合にSPを回復させる
        if (event.getSource().getDirectEntity() instanceof ServerPlayer player) {
            // ソードスキル（AttackEffectEntity）による攻撃ではないことを確認
            if (!(event.getSource().getDirectEntity() instanceof AttackEffectEntity)) {
                SPManager.onAttack(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            // 攻撃者がソードスキルを実行中かチェック
            if (SkillExecutionManager.skillExecutions.containsKey(player.getUUID())) {
                Level level = player.level();
                LivingEntity victim = event.getEntity();
                
                // 1. 体術の心得ドロップ (1%の確率)
                if (level.random.nextFloat() < 0.01f) {
                    victim.spawnAtLocation(SampleItemRegistry.MARTIAL_ARTS_SCROLL.get());
                }
                
                // 2. スキルオーブドロップ (ボスタグ付きモブの場合50%の確率)
                // forge:bosses タグ または 伝統的なボス判定
                boolean isBoss = victim.getType().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses")))
                                || !victim.canChangeDimensions(); // ドラゴンやウィザーは次元移動できないことが多い
                
                if (isBoss && level.random.nextFloat() < 0.5f) {
                    victim.spawnAtLocation(SampleItemRegistry.UNLOCKITEM.get());
                }
            }
        }
    }

    private static void applySPConfig(ServerPlayer player) {
        AttributeInstance maxSpAttr = player.getAttribute(SwordSkillAttribute.MAX_SP.get());
        if (maxSpAttr != null) {
            maxSpAttr.setBaseValue(ServerConfig.defaultMaxSP.get());
        }
        AttributeInstance regenSpAttr = player.getAttribute(SwordSkillAttribute.SP_REGEN.get());
        if (regenSpAttr != null) {
            regenSpAttr.setBaseValue(ServerConfig.defaultSPRegen.get());
        }
    }

    public static void sendSkillSlotInfo(ServerPlayer player) {
        // サーバー側のWeaponDataを取得
        WeaponData serverWeaponData = WeaponTypeUtils.getWeaponData(player);
        String currentWeaponName = "None";
        // WeaponTypeのセットを取得 (nullなら空セット)
        Set<SkillData.WeaponType> currentWeaponTypes = Collections.emptySet();

        if (serverWeaponData != null) {
            currentWeaponName = (serverWeaponData.weaponName() != null) ? serverWeaponData.weaponName() : "None";
            currentWeaponTypes = (serverWeaponData.weaponType() != null) ? serverWeaponData.weaponType()
                    : Collections.emptySet();
        }

        String finalWeaponName = currentWeaponName;
        Set<SkillData.WeaponType> finalWeaponTypes = currentWeaponTypes;

        // Capabilityからスロット情報を取得して送信
        player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(skills -> {
            // Capabilityから現在の武器に対応するスロット配列を取得
            int[] skillIds = skills.getSkillSlots(finalWeaponName);

            // SkillSlotInfoPacketにスキルID配列、武器名、武器タイプセットを渡して送信
            NetworkHandler.INSTANCE.sendTo(
                    new SkillSlotInfoPacket(skillIds, finalWeaponName, finalWeaponTypes),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);

            // ★追加: スロット情報送信時にもトレイルの設定状態を同期するように念のために追加
            NetworkHandler.INSTANCE.sendTo(
                    new com.zoma1101.swordskill.network.toClient.SyncTrailConfigPacket(skills.isTrailEnabled()),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);
        });
    }
}