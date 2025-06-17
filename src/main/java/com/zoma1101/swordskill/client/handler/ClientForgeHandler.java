package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.HudPositionSettingScreen;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.payload.SkillLoadSlotPayload;
import com.zoma1101.swordskill.payload.SkillRequestPayload;
import com.zoma1101.swordskill.payload.SkillUnlockPayload;
import com.zoma1101.swordskill.payload.UseSkillPayload;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;


import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;
import static com.zoma1101.swordskill.client.handler.ClientTickHandler.getSelectedSlot;
import static com.zoma1101.swordskill.data.WeaponTypeUtils.*;
import static com.zoma1101.swordskill.swordskills.SkillData.SkillType.*;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientForgeHandler {

    private static final Map<Integer, Integer> cooldowns = new HashMap<>();
    private static int addSkillIndex=0;
    private static Integer skillUsedTicks = null; // スキル使用後の経過 Tick をカウントする変数
    private static Integer limitTickMax = 12;
    private static final Integer limitTickMin = 4;
    private static boolean SetWeaponType = false;

    public static void setSelectedSkillIndex(int index) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getPersistentData().putInt("selectedSkillIndex", index);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            updateCooldowns();

            if (SetWeaponType){
                if (WeaponTypeDetector.isReady() || Minecraft.getInstance().player == null) {
                    return;
                }
                String WeaponName = ClientSkillSlotHandler.getCurrentWeaponName();
                setWeaponType(player);
                PacketDistributor.sendToServer(new SkillLoadSlotPayload(WeaponName));
                SetWeaponType = false;
            }

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
            Set<SkillData.WeaponType> weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            String WeaponName = ClientSkillSlotHandler.getCurrentWeaponName();
            if (WeaponName != null && SkillData.getAvailableWeaponTypes().stream().anyMatch(Objects.requireNonNull(weaponType)::contains)) {
                PacketDistributor.sendToServer(new UseSkillPayload(SkillData.getId(), SkillData.getFinalTick()));
                cooldowns.put(CoolDown_SkillID, getCoolDown(SkillData));
                limitTickMax = SkillData.getTransformLimitTick();
                if (SkillData.getType().equals(RUSH)){
                    PlayerAnimation(SkillID,"start");
                }
                else {
                    PlayerAnimation(SkillID,"");
                }
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

    public static void setCooldowns(int SkillID, int SetCoolDown){
        cooldowns.put(SkillID, SetCoolDown);
    }


    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PacketDistributor.sendToServer(new SkillRequestPayload());
        SetWeaponType = true;
        PacketDistributor.sendToServer(new SkillUnlockPayload(0));
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        String WeaponName = ClientSkillSlotHandler.getCurrentWeaponName();
        if (WeaponName != null && !WeaponName.equals("None")) {
            if (Keybindings.INSTANCE.SwordSkill_Selector_Key.isDown()) {
                Minecraft.getInstance().setScreen(new SwordSkillSelectionScreen());
            } else if (Keybindings.INSTANCE.SwordSkill_HUD_Setting.isDown()) {
                Minecraft.getInstance().setScreen(new HudPositionSettingScreen());
            }
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
                            return 1.0f - (float) remainingTicks / getCoolDown(nextSkill);
                        }
                    }
                }
                return 1.0f - (float) remainingTicks / getCoolDown(skill);
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

    private static int getCoolDown(SkillData SkillData){
        LocalPlayer player = Minecraft.getInstance().player;
        double cooldown = player != null ? player.getAttributeBaseValue(SwordSkillAttribute.COOLDOWN_ATTRIBUTE) : 0;
        return (int) (SkillData.getCooldown() * cooldown);
    }
}