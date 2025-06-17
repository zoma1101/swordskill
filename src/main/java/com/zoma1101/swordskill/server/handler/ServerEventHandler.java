package com.zoma1101.swordskill.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.WeaponData;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.payload.SkillSlotInfoPayload;
import com.zoma1101.swordskill.swordskills.SkillData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.zoma1101.swordskill.data.WeaponTypeUtils.setWeaponType;

@EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Map<ServerPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<ServerPlayer, ItemStack> offHandItems = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) { // 型チェックとキャストを同時に行う
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            // プレイヤーが初めてTickされる場合、またはアイテムが変更された場合
            if (!mainHandItems.containsKey(player) || !offHandItems.containsKey(player) ||
                    !ItemStack.matches(mainHandItem, mainHandItems.get(player)) ||
                    !ItemStack.matches(offHandItem, offHandItems.get(player))) {

                setWeaponType(player);
                sendSkillSlotInfo(player);
                // SetSlotSkill(); // このメソッドはクライアント側のメソッドのようです。サーバーからは直接呼び出せません。
                // おそらく、SkillSlotInfoPayload を受け取ったクライアント側で処理すべき内容です。
            }
            mainHandItems.put(player, mainHandItem.copy());
            offHandItems.put(player, offHandItem.copy());
        }
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            mainHandItems.remove(player);
            offHandItems.remove(player);
            WeaponTypeUtils.setWeaponType(player); // ログイン時に武器タイプを設定
            sendSkillSlotInfo(player); // ログイン時にスキル情報を送信
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            mainHandItems.remove(player);
            offHandItems.remove(player);
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
            currentWeaponTypes = (serverWeaponData.weaponType() != null) ? serverWeaponData.weaponType() : Collections.emptySet();
        }

        int[] skillIds = new int[]{-1, -1, -1, -1, -1}; // デフォルト値

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
        // 修正点: sendToServer から sendToPlayer へ変更
        PacketDistributor.sendToPlayer(player, new SkillSlotInfoPayload(skillIds, currentWeaponName, currentWeaponTypes));
    }
}