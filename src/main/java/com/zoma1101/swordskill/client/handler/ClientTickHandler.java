package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.payload.SkillRequestPayload;
import com.zoma1101.swordskill.payload.SkillSelectionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;


import java.util.HashMap;
import java.util.Map;

import static com.zoma1101.swordskill.server.handler.SkillExecutionManager.skillExecutions;


@EventBusSubscriber(modid = "swordskill", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientTickHandler {

    private static final Map<LocalPlayer, ItemStack> mainHandItems = new HashMap<>();
    private static final Map<LocalPlayer, ItemStack> offHandItems = new HashMap<>();

    private static int selectedSlot = 0;
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Keybindings.INSTANCE.SwordSkill_QuickSelect_Key.isDown()) {
            // マウスホイールの上下スクロールを取得
            double scrollDelta = event.getScrollDeltaY();
            if (scrollDelta != 0) { // スクロールがあった場合のみ処理
                if (scrollDelta > 0) {
                    selectedSlot--;
                } else {
                    selectedSlot++;
                }

                // selectedSlot の範囲を 0 から 4 に制限
                if (selectedSlot < 0) {
                    selectedSlot = 4;
                } else if (selectedSlot > 4) {
                    selectedSlot = 0;
                }
                event.setCanceled(true); // スキル選択時は他のマウスホイール処理をキャンセル
            }
        }
        // スキル実行中は他のマウスホイール処理をキャンセル (これは元のロジックのまま)
        else if (!skillExecutions.isEmpty()) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) {
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

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    public static void SetSlotSkill(){
        int[] skillSlots = ClientSkillSlotHandler.getSkillSlotInfo();
        if (skillSlots != null && selectedSlot >= 0 && selectedSlot < skillSlots.length) {
            int skillId = skillSlots[selectedSlot];
            // skillId が -1 でない場合のみ送信 (未設定スロットは送信しないなど)
            if (skillId != -1) {
                PacketDistributor.sendToServer(new SkillSelectionPayload(skillId));
                PacketDistributor.sendToServer(new SkillRequestPayload());
            }
        }
    }
}