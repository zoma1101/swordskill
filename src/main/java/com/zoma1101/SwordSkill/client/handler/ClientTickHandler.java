package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillRequestPacket;
import com.zoma1101.swordskill.network.SkillSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

import static com.zoma1101.swordskill.server.handler.SkillExecutionManager.skillExecutions;


@Mod.EventBusSubscriber(modid = "swordskill", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientTickHandler {

    private static final Map<LocalPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<LocalPlayer, ItemStack> offHandItems = new HashMap<>();

    private static int selectedSlot = 0;
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Keybindings.INSTANCE.SwordSkill_QuickSelect_Key.isDown()) {
            selectedSlot -= (int) event.getScrollDelta();
            if (selectedSlot < 0) {
                selectedSlot = 4;
            } else if (selectedSlot > 4) {
                selectedSlot = 0;
            }
            event.setCanceled(true);
        }
        else if (!skillExecutions.isEmpty()) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            if (mainHandItems.containsKey(player) && offHandItems.containsKey(player)) {
                ItemStack previousMainHandItem = mainHandItems.get(player);
                ItemStack previousOffHandItem = offHandItems.get(player);
                if (!ItemStack.matches(mainHandItem, previousMainHandItem) || !ItemStack.matches(offHandItem, previousOffHandItem)) {
                    SetSlotSkill();
                }
            }
            mainHandItems.put(player, mainHandItem.copy());
            offHandItems.put(player, offHandItem.copy());
        }
    }

    public static int getSelectedSlot() { // 追加
        return selectedSlot;
    }

    public static void SetSlotSkill(){
        int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[selectedSlot];
        if (skillId != -1) {
            NetworkHandler.INSTANCE.sendToServer(new SkillSelectionPacket(skillId));
            NetworkHandler.INSTANCE.sendToServer(new SkillRequestPacket());
        }
    }


}