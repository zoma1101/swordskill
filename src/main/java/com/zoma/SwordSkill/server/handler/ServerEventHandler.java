package com.zoma.SwordSkill.server.handler;

import com.zoma.SwordSkill.data.DataManager;
import com.zoma.SwordSkill.swordskills.SkillUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zoma.SwordSkill.network.NetworkHandler;
import com.zoma.SwordSkill.network.SkillSlotInfoPacket;
import com.zoma.SwordSkill.swordskills.SkillData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.zoma.SwordSkill.main.SwordSkill;
import net.minecraftforge.network.NetworkDirection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger; // 追加

import java.util.HashMap;
import java.util.Map;

import static com.zoma.SwordSkill.client.handler.ClientTickHandler.SetSlotSkill;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID)
public class ServerEventHandler {

    private static final Logger LOGGER = LogManager.getLogger(); // 追加

    private static Map<ServerPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static Map<ServerPlayer, ItemStack> offHandItems = new HashMap<>();

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
                    // アイテムが変更された場合の処理
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
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            sendSkillSlotInfo(player);
        }
    }

    public static void sendSkillSlotInfo(ServerPlayer player) {
        JsonObject playerData = DataManager.loadPlayerData(player);
        JsonObject weaponSkills = playerData.getAsJsonObject("weaponSkills");
        if (weaponSkills != null) {
            SkillData.WeaponType weaponType = SkillUtils.getWeaponType(player);
            if (weaponType != null) {
                JsonArray skillSlot = weaponSkills.getAsJsonArray(weaponType.name());
                int[] skillIds = new int[5];
                if (skillSlot != null) {
                    for (int i = 0; i < 5; i++) {
                        skillIds[i] = skillSlot.get(i).getAsInt();
                    }
                } else {
                    LOGGER.warn("武器種 {} のスキルスロット情報が見つかりません。", weaponType.name()); // 追加
                }
                NetworkHandler.INSTANCE.sendTo(new SkillSlotInfoPacket(skillIds), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            } else {
                LOGGER.warn("プレイヤーの武器種を判定できませんでした。"); // 追加
            }
        }
    }
}