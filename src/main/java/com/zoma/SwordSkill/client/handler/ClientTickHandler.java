package com.zoma.SwordSkill.client.handler;

import com.zoma.SwordSkill.client.screen.Keybindings;
import com.zoma.SwordSkill.network.NetworkHandler;
import com.zoma.SwordSkill.network.SkillRequestPacket;
import com.zoma.SwordSkill.network.SkillSelectionPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.zoma.SwordSkill.swordskills.SkillUtils.getWeaponType;

@Mod.EventBusSubscriber(modid = "swordskill", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientTickHandler {

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
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
                SetSlotSkill();
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