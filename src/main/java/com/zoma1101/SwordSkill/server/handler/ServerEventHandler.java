package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.capability.PlayerSkillsProvider;
import com.zoma1101.swordskill.data.WeaponData;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillSlotInfoPacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Map<ServerPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<ServerPlayer, ItemStack> offHandItems = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

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
        // 死亡によるリスポーンの場合、元のプレイヤーのCapabilityは無効化されているため復活させる必要がある
        boolean isDeath = event.isWasDeath();
        if (isDeath) {
            event.getOriginal().reviveCaps();
        }

        try {
            event.getOriginal().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(oldSkills -> {
                event.getEntity().getCapability(PlayerSkillsProvider.PLAYER_SKILLS).ifPresent(newSkills -> {
                    // NBT経由ではなく、copyFromメソッドを使って直接データをコピーする
                    // (PlayerSkillsクラスにcopyFromが実装されているため、これを使うのが最も確実です)
                    newSkills.copyFrom(oldSkills);
                });
            });
        } finally {
            // 処理が終わったら、死亡時の場合は再度無効化しておく
            if (isDeath) {
                event.getOriginal().invalidateCaps();
            }
        }
    }

    // ★追加: リスポーン時の同期処理
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // リスポーン時にクライアントへデータを同期
            // これがないと、サーバー側ではデータが残っていてもクライアント側(GUIなど)で反映されない場合がある
            sendSkillSlotInfo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // ログイン時にもサーバー側の武器タイプを設定し、クライアントに情報を送信
            WeaponTypeUtils.setWeaponType(player);
            sendSkillSlotInfo(player);

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

    public static void sendSkillSlotInfo(ServerPlayer player) {
        // サーバー側のWeaponDataを取得
        WeaponData serverWeaponData = WeaponTypeUtils.getWeaponData(player);
        String currentWeaponName = "None";
        // WeaponTypeのセットを取得 (nullなら空セット)
        Set<SkillData.WeaponType> currentWeaponTypes = Collections.emptySet();

        if (serverWeaponData != null) {
            currentWeaponName = (serverWeaponData.weaponName() != null) ? serverWeaponData.weaponName() : "None";
            currentWeaponTypes = (serverWeaponData.weaponType() != null) ? serverWeaponData.weaponType() : Collections.emptySet();
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
                    NetworkDirection.PLAY_TO_CLIENT
            );
        });
    }
}