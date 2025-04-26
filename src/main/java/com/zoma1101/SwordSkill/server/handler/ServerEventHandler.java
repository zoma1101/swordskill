package com.zoma1101.swordskill.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
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

            boolean changed = false;

            if (!mainHandItems.containsKey(player) || !ItemStack.matches(mainHandItem, mainHandItems.get(player)) || !offHandItems.containsKey(player) || !ItemStack.matches(offHandItem, offHandItems.get(player)))
            {
                // サーバー側の武器タイプ情報を更新 (これがサーバー側のマップを更新する)
                WeaponTypeUtils.setWeaponType(player);
                changed = true;
            }

            // アイテム情報を更新 (変更チェックの後に行う)
            mainHandItems.put(player, mainHandItem.copy());
            offHandItems.put(player, offHandItem.copy());

            // アイテムが変更された場合のみ情報を送信する
            if (changed) {
                sendSkillSlotInfo(player);
            }
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

    public static void sendSkillSlotInfo(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");

        // サーバー側のWeaponDataを取得
        WeaponData serverWeaponData = WeaponTypeUtils.getWeaponData(player);
        String currentWeaponName = "None";
        // WeaponTypeのセットを取得 (nullなら空セット)
        Set<SkillData.WeaponType> currentWeaponTypes = Collections.emptySet();

        if (serverWeaponData != null) {
            currentWeaponName = (serverWeaponData.weaponName() != null) ? serverWeaponData.weaponName() : "None";
            currentWeaponTypes = (serverWeaponData.weaponType() != null) ? serverWeaponData.weaponType() : Collections.emptySet();}

        int[] skillIds = new int[]{-1, -1, -1, -1, -1};

        if (weaponSkills != null && !currentWeaponName.equals("None")) {
            JsonArray skillSlot = weaponSkills.getAsJsonArray(currentWeaponName);
            if (skillSlot != null) {
                int limit = Math.min(skillSlot.size(), skillIds.length);
                for (int i = 0; i < limit; i++) {
                    if (skillSlot.get(i) != null && !skillSlot.get(i).isJsonNull()) {
                        skillIds[i] = skillSlot.get(i).getAsInt();
                    }
                }
            }
        }

        // SkillSlotInfoPacketにスキルID配列、武器名、武器タイプセットを渡して送信
        NetworkHandler.INSTANCE.sendTo(
                new SkillSlotInfoPacket(skillIds, currentWeaponName, currentWeaponTypes),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}