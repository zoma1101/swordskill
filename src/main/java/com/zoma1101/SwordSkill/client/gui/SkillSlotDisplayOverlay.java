package com.zoma1101.SwordSkill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.SwordSkill.client.handler.ClientForgeHandler;
import com.zoma1101.SwordSkill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.SwordSkill.client.handler.ClientTickHandler;
import com.zoma1101.SwordSkill.config.ClientConfig;
import com.zoma1101.SwordSkill.data.WeaponTypeUtils;
import com.zoma1101.SwordSkill.swordskills.SkillData;
import com.zoma1101.SwordSkill.swordskills.SwordSkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkillSlotDisplayOverlay {

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_SIZE = 0;
    private static final int SLOT_SPACING = 4;

    public static final IGuiOverlay HUD_SKILL_SLOT = (gui, guiGraphics, partialTicks, width, height) -> {
        RenderSystem.enableBlend();
        String weaponType = WeaponTypeUtils.getWeaponName();
        if (weaponType != null && !weaponType.equals("None")) {
            int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo();
            int selectedSlot = ClientTickHandler.getSelectedSlot();
            int hudX = ClientConfig.hudPosX.get();
            int hudY = ClientConfig.hudPosY.get();
            int hudSize = SLOT_SIZE + ClientConfig.hudScale.get();

            for (int i = 0; i < SLOT_COUNT; i++) {
                if (skillIds[i] != -1) {
                    SkillData skill = SwordSkillRegistry.SKILLS.get(skillIds[i]);
                    if (skill != null) {
                        ResourceLocation icon = skill.getIconTexture();
                        // マウスカーソルの位置にHUDの左上の角を重ねるように修正
                        int x = hudX + (width - (SLOT_COUNT * hudSize + (SLOT_COUNT - 1) * SLOT_SPACING)) / 2 + i * (hudSize + SLOT_SPACING);
                        int y = hudY + 10;

                        if (i == selectedSlot) {
                            guiGraphics.fill(x - 2, y - 2, x + hudSize + 2, y + hudSize + 2, 0x80FFFFFF);
                        }

                        guiGraphics.blit(icon, x, y, 0, 0, hudSize, hudSize, hudSize, hudSize);

                        float cooldownRatio = ClientForgeHandler.getCooldownRatio(i);
                        if (cooldownRatio < 1.0f) {
                            int cooldownHeight = (int) (hudSize * (1.0f - cooldownRatio));
                            guiGraphics.fill(x, y + hudSize - cooldownHeight, x + hudSize, y + hudSize, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }
        RenderSystem.disableBlend();
    };
}