package com.zoma1101.SwordSkill.client.gui;

import com.zoma1101.SwordSkill.data.WeaponTypeUtils;
import com.zoma1101.SwordSkill.main.SwordSkill;
import com.zoma1101.SwordSkill.network.NetworkHandler;
import com.zoma1101.SwordSkill.network.SkillSlotSelectionPacket;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import com.zoma1101.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// スキルスロット選択画面
public class DecideSkillScreen extends Screen {

    private static int SELECTSKILL;

    public DecideSkillScreen(int selectedSkill) {
        super(Component.literal("スキルスロット選択"));
        SELECTSKILL = selectedSkill;
    }

    @Override
    protected void init() {
        super.init();

        // スロット 0 から 4 までのボタンを追加
        for (int i = 0; i < 5; i++) {
            final int slotIndex = i;
            addRenderableWidget(Button.builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".slot."+ i), button -> {
                saveSkillToSlot(slotIndex);
                Minecraft.getInstance().setScreen(null);
            }).bounds(width / 2 - 50, height / 2 - 40 + i * 25, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, Component.translatable("gui." + SwordSkill.MOD_ID + ".select_slot"), width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void saveSkillToSlot(int slotIndex) {
        if (minecraft != null && minecraft.player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(SELECTSKILL);
            if (skill != null) {
                SkillData.WeaponType playerWeaponType = WeaponTypeUtils.getWeaponType(); // 追加
                if (playerWeaponType != null) { // 追加
                    NetworkHandler.INSTANCE.sendToServer(new SkillSlotSelectionPacket(skill.getId(), slotIndex, playerWeaponType)); // 修正
                } // 追加
            }
        }
    }

}