package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillSlotSelectionPacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, Component.translatable("gui." + SwordSkill.MOD_ID + ".select_slot"), width / 2, height / 2 - 60, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void saveSkillToSlot(int slotIndex) {
        if (minecraft != null && minecraft.player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(SELECTSKILL);
            if (skill != null) {
                String playerWeaponName = ClientSkillSlotHandler.getCurrentWeaponName();
                if (playerWeaponName != null) { // 追加
                    NetworkHandler.INSTANCE.sendToServer(new SkillSlotSelectionPacket(skill.getId(), slotIndex, playerWeaponName)); // 修正
                } // 追加
            }
        }
    }

}