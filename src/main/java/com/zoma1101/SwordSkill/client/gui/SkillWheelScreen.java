package com.zoma1101.swordskill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SkillWheelScreen extends Screen {
    private final int numSlots = 5;
    private int hoveredSlot = -1;

    public SkillWheelScreen() {
        super(Component.literal("Skill Wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // ゲームを止めない
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 背景を薄く暗くする
        guiGraphics.fill(0, 0, width, height, 0x44000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int radius = 80;

        // マウス位置から角度を計算
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 20) { // 中心付近は無視
            // 上方向(Yマイナス)を0度とした時計回りの角度
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;
            
            // 5つのセクション (360/5 = 72度ずつ)
            hoveredSlot = (int) (angle / 72.0);
            if (hoveredSlot >= numSlots) hoveredSlot = 0;
        } else {
            hoveredSlot = -1;
        }

        int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo();

        for (int i = 0; i < numSlots; i++) {
            double midAngleDeg = i * 72;
            double midAngleRad = Math.toRadians(midAngleDeg - 90);
            
            int iconX = centerX + (int) (Math.cos(midAngleRad) * radius) - 16;
            int iconY = centerY + (int) (Math.sin(midAngleRad) * radius) - 16;

            // 背景枠
            int color = (i == hoveredSlot) ? 0xBBFFFFFF : 0x66000000;
            guiGraphics.fill(iconX - 4, iconY - 4, iconX + 36, iconY + 36, color);

            int skillId = skillIds[i];
            if (skillId != -1) {
                SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
                if (skill != null && skill.getIconTexture() != null) {
                    RenderSystem.setShaderTexture(0, skill.getIconTexture());
                    guiGraphics.blit(skill.getIconTexture(), iconX, iconY, 0, 0, 32, 32, 32, 32);
                }
            } else {
                // 空きスロット
                guiGraphics.drawCenteredString(font, String.valueOf(i + 1), iconX + 16, iconY + 12, 0x88888888);
            }
        }
        
        // 中心部に選択中のスキル名を表示
        if (hoveredSlot != -1) {
            int skillId = skillIds[hoveredSlot];
            if (skillId != -1) {
                SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
                if (skill != null) {
                    String name = Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName()).getString();
                    guiGraphics.drawCenteredString(font, name, centerX, centerY - 5, 0xFFFFAA00);
                }
            } else {
                guiGraphics.drawCenteredString(font, "None", centerX, centerY - 5, 0x88888888);
            }
        }

        // キーが離されたら閉じる (生のGLFW状態で判定)
        long window = Minecraft.getInstance().getWindow().getWindow();
        int keyCode = Keybindings.INSTANCE.SwordSkill_Wheel_Key.getKey().getValue();
        if (!com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, keyCode)) {
            this.onClose();
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        if (hoveredSlot != -1) {
            ClientTickHandler.setSelectedSlot(hoveredSlot);
        }
        // Minecraft 1.20+ では setScreen(null) で閉じます
        Minecraft.getInstance().setScreen(null);
    }
}
