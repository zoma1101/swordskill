package com.zoma.SwordSkill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma.SwordSkill.client.handler.ClientForgeHandler;
import com.zoma.SwordSkill.client.handler.ClientSkillSlotHandler;
import com.zoma.SwordSkill.client.handler.ClientTickHandler;
import com.zoma.SwordSkill.config.ClientConfig;
import com.zoma.SwordSkill.swordskills.SkillData;
import com.zoma.SwordSkill.swordskills.SkillUtils;
import com.zoma.SwordSkill.swordskills.SwordSkillRegistry;
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
        SkillData.WeaponType weaponType = SkillUtils.getWeaponType(); // 修正
        if (weaponType != null) {
            int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo(); // 修正
            int selectedSlot = ClientTickHandler.getSelectedSlot();
            int hudX = ClientConfig.hudPosX.get();
            int hudY = ClientConfig.hudPosX.get();
            int hudSize = SLOT_SIZE + ClientConfig.hudScale.get();

            for (int i = 0; i < SLOT_COUNT; i++) {
                if (skillIds[i] != -1) {
                    SkillData skill = SwordSkillRegistry.SKILLS.get(skillIds[i]);
                    if (skill != null) {
                        ResourceLocation icon = skill.getIconTexture();
                        int x = hudX + (width - (SLOT_COUNT * hudSize + (SLOT_COUNT - 1) * SLOT_SPACING)) / 2 + i * (hudSize + SLOT_SPACING);
                        int y = hudY + 10;

                        if (i == selectedSlot) {
                            guiGraphics.fill(x - 2, y - 2, x + hudSize + 2, y + hudSize + 2, 0x80FFFFFF);
                        }

                        guiGraphics.blit(icon, x, y, 0, 0, hudSize, hudSize, hudSize, hudSize);

                        // クールダウンの割合に応じて白色の正方形を描画
                        float cooldownRatio = ClientForgeHandler.getCooldownRatio(i); // 修正
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