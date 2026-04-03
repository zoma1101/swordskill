package com.zoma1101.swordskill.client.gui;
 
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
 
public class SkillWheelOverlay {
    public static boolean isVisible = false;
    private static int hoveredSlot = -1;
    private static final int numSlots = 5;
 
    public static final IGuiOverlay HUD_SKILL_WHEEL = (gui, guiGraphics, partialTicks, width, height) -> {
        if (!isVisible) return;
 
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
 
        // 背景を薄く暗くする
        guiGraphics.fill(0, 0, width, height, 0x44000000);
 
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = 80;
 
        // マウス位置から角度を計算 (GUIスケールを考慮)
        double[] xBuffer = new double[1];
        double[] yBuffer = new double[1];
        org.lwjgl.glfw.GLFW.glfwGetCursorPos(mc.getWindow().getWindow(), xBuffer, yBuffer);
        double mouseX = xBuffer[0] * (double)width / (double)mc.getWindow().getWidth();
        double mouseY = yBuffer[0] * (double)height / (double)mc.getWindow().getHeight();
 
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
 
        if (dist > 20) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;
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
                guiGraphics.drawCenteredString(mc.font, String.valueOf(i + 1), iconX + 16, iconY + 12, 0x88888888);
            }
        }
        
        if (hoveredSlot != -1) {
            int skillId = skillIds[hoveredSlot];
            if (skillId != -1) {
                SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
                if (skill != null) {
                    String name = net.minecraft.network.chat.Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName()).getString();
                    guiGraphics.drawCenteredString(mc.font, name, centerX, centerY - 5, 0xFFFFAA00);
                }
            } else {
                guiGraphics.drawCenteredString(mc.font, "None", centerX, centerY - 5, 0x88888888);
            }
        }
 
        // 常にキーの状態をチェックし、離されたら閉じる
        long window = mc.getWindow().getWindow();
        int keyCode = Keybindings.INSTANCE.SwordSkill_Wheel_Key.getKey().getValue();
        if (!InputConstants.isKeyDown(window, keyCode)) {
            closeWheel();
        }
    };
 
    public static void openWheel() {
        if (!isVisible) {
            isVisible = true;
            hoveredSlot = -1;
            Minecraft.getInstance().mouseHandler.releaseMouse();
        }
    }
 
    public static void closeWheel() {
        if (isVisible) {
            isVisible = false;
            if (hoveredSlot != -1) {
                ClientTickHandler.setSelectedSlot(hoveredSlot);
            }
            Minecraft.getInstance().mouseHandler.grabMouse();
        }
    }
}
