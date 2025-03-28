package com.zoma1101.swordskill.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.data.DataManager;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillSlotInfoPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.zoma1101.swordskill.client.handler.ClientTickHandler.SetSlotSkill;
import static com.zoma1101.swordskill.data.WeaponTypeUtils.setWeaponType;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(); // 追加

    private static final Map<ServerPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<ServerPlayer, ItemStack> offHandItems = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            ServerPlayer player = (ServerPlayer) event.player;
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            if (mainHandItems.containsKey(player) && offHandItems.containsKey(player)) {
                ItemStack previousMainHandItem = mainHandItems.get(player);
                ItemStack previousOffHandItem = offHandItems.get(player);
                if (!ItemStack.matches(mainHandItem, previousMainHandItem) || !ItemStack.matches(offHandItem, previousOffHandItem)) {
                    setWeaponType(player);
                    sendSkillSlotInfo(player);
                    SetSlotSkill();
                }
            }
            mainHandItems.put(player, mainHandItem.copy());
            offHandItems.put(player, offHandItem.copy());
        }
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendSkillSlotInfo(player);
        }
    }

    public static void sendSkillSlotInfo(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");
        if (weaponSkills != null) {
            String weaponName = WeaponTypeUtils.getWeaponName();
            if (weaponName != null) {
                JsonArray skillSlot = weaponSkills.getAsJsonArray(weaponName);
                int[] skillIds = new int[5];
                if (skillSlot != null) {
                    for (int i = 0; i < 5; i++) {
                        skillIds[i] = skillSlot.get(i).getAsInt();
                    }
                } else {
                    LOGGER.warn("武器種 {} のスキルスロット情報が見つかりません。", weaponName); // 追加
                }
                NetworkHandler.INSTANCE.sendTo(new SkillSlotInfoPacket(skillIds), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            } else {
                LOGGER.warn("プレイヤーの武器種を判定できませんでした。"); // 追加
            }
        }
    }


}