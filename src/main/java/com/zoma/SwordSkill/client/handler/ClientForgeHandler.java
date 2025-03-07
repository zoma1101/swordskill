package com.zoma.SwordSkill.client.handler;

import com.zoma.SwordSkill.client.gui.SwordSkillSelectionScreen;
import com.zoma.SwordSkill.client.screen.Keybindings;
import com.zoma.SwordSkill.main.SwordSkill;
import com.zoma.SwordSkill.network.NetworkHandler;
import com.zoma.SwordSkill.network.SkillRequestPacket;
import com.zoma.SwordSkill.network.UseSkillPacket;
import com.zoma.SwordSkill.swordskills.SkillData;
import com.zoma.SwordSkill.swordskills.SkillUtils;
import com.zoma.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.zoma.SwordSkill.client.handler.ClientTickHandler.getSelectedSlot;
import static com.zoma.SwordSkill.swordskills.SkillData.SkillType.TRANSFORM;
import static com.zoma.SwordSkill.swordskills.SkillData.SkillType.TRANSFORM_FINISH;
import static com.zoma.SwordSkill.swordskills.SkillUtils.getWeaponType;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeHandler {

    private static final Map<Integer, Integer> cooldowns = new HashMap<>();
    private static int addSkillIndex=0;
    private static Integer skillUsedTicks = null; // スキル使用後の経過 Tick をカウントする変数

    private static final Integer limitTickMax = 12;
    private static final Integer limitTickMin = 7;

    public static void setSelectedSkillIndex(int index) {
        Minecraft.getInstance().player.getPersistentData().putInt("selectedSkillIndex", index);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            updateCooldowns();

            if (Keybindings.INSTANCE.SwordSkill_Use_Key.isDown()) {
                Keybindings.INSTANCE.SwordSkill_Use_Key.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[getSelectedSlot()];
                UseSkill(skillId);
            }

            if (Keybindings.INSTANCE.SwordSkill_Use_Key_0.isDown()){
                Keybindings.INSTANCE.SwordSkill_Use_Key_0.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[0];
                UseSkill(skillId);
            }
            if (Keybindings.INSTANCE.SwordSkill_Use_Key_1.isDown()){
                Keybindings.INSTANCE.SwordSkill_Use_Key_1.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[1];
                UseSkill(skillId);
            }
            if (Keybindings.INSTANCE.SwordSkill_Use_Key_2.isDown()){
                Keybindings.INSTANCE.SwordSkill_Use_Key_2.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[2];
                UseSkill(skillId);
            }
            if (Keybindings.INSTANCE.SwordSkill_Use_Key_3.isDown()){
                Keybindings.INSTANCE.SwordSkill_Use_Key_3.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[3];
                UseSkill(skillId);
            }
            if (Keybindings.INSTANCE.SwordSkill_Use_Key_4.isDown()){
                Keybindings.INSTANCE.SwordSkill_Use_Key_4.consumeClick();
                int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[4];
                UseSkill(skillId);
            }


            if (skillUsedTicks != null) {
                skillUsedTicks++; // 経過 Tick をインクリメント
                if (skillUsedTicks > limitTickMax){
                    addSkillIndex=0;
                    skillUsedTicks = null;
                }
            }
        }
    }

    private static void ExecuteSkill(int SkillID, int CoolDown_SkillID) {
        SkillData SkillData = SwordSkillRegistry.SKILLS.get(SkillID);
        if (SkillData != null) {
            SkillData.WeaponType weaponType = getWeaponType(); // 追加
            if (weaponType != null && SkillData.getAvailableWeaponTypes().contains(weaponType)) { // 追加
                NetworkHandler.sendToServer(new UseSkillPacket(SkillData.getId(), SkillData.getFinalTick()));
                cooldowns.put(CoolDown_SkillID, SkillData.getCooldown());
            }
        }
    }

    private static void updateCooldowns() {
        cooldowns.forEach((skillId, remainingTicks) -> {
            if (remainingTicks > 0) {
                cooldowns.put(skillId, remainingTicks - 1);
            }
        });
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        NetworkHandler.INSTANCE.sendToServer(new SkillRequestPacket());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybindings.INSTANCE.SwordSkill_Selector_Key.isDown() && getWeaponType() != null) {
            Minecraft.getInstance().setScreen(new SwordSkillSelectionScreen());
        }
    }

    public static float getCooldownRatio(int slotIndex) { // 変更箇所
        int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[slotIndex];
        if (cooldowns.containsKey(skillId)) {
            int remainingTicks = cooldowns.get(skillId);
            SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
            if (skill != null) {
                if (skill.getType() == TRANSFORM) {
                    // TRANSFORMスキルの場合、TRANSFORM_FINISHスキルのクールダウンを参照
                    for (int i = skillId + 1; i < SwordSkillRegistry.SKILLS.size(); i++) {
                        SkillData nextSkill = SwordSkillRegistry.SKILLS.get(i);
                        if (nextSkill.getType() == TRANSFORM_FINISH) {
                            return 1.0f - (float) remainingTicks / nextSkill.getCooldown();
                        }
                    }
                }
                return 1.0f - (float) remainingTicks / skill.getCooldown();
            }
        }
        return 1.0f; // クールダウンがない場合は1.0を返す
    }

    private static void UseSkill(int selectedSkillIndex){
            if (selectedSkillIndex >= 0 && selectedSkillIndex < SwordSkillRegistry.SKILLS.size()) {
                int currentSkillIndex=selectedSkillIndex+addSkillIndex;
                if (!cooldowns.containsKey(selectedSkillIndex) || cooldowns.get(selectedSkillIndex) <= 0) {
                    ExecuteSkill(selectedSkillIndex,selectedSkillIndex);
                    skillUsedTicks = 0; // スキル使用後に 0 で初期化
                } else if (skillUsedTicks != null) {
                    if ( skillUsedTicks<limitTickMax && skillUsedTicks>limitTickMin && SwordSkillRegistry.SKILLS.get(currentSkillIndex).getType() == TRANSFORM){
                        addSkillIndex++;
                        currentSkillIndex=selectedSkillIndex+addSkillIndex;
                        ExecuteSkill(currentSkillIndex,selectedSkillIndex);
                        skillUsedTicks = 0;

                    }
                    else if (skillUsedTicks<limitTickMax && skillUsedTicks>limitTickMin && SwordSkillRegistry.SKILLS.get(currentSkillIndex).getType() == TRANSFORM_FINISH) {
                        ExecuteSkill(currentSkillIndex,selectedSkillIndex);
                        addSkillIndex=0;
                        skillUsedTicks = null;
                    }
                }
            }
    }

}