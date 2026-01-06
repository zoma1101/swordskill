package com.zoma1101.swordskill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

@OnlyIn(Dist.CLIENT)
public class SkillSlotDisplayOverlay {

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_SIZE = 0; // サイズはConfigのスケールで決定されるため0
    private static final int SLOT_SPACING = 4;

    public static final IGuiOverlay HUD_SKILL_SLOT = (gui, guiGraphics, partialTicks, width, height) -> {
        RenderSystem.enableBlend();

        // 現在の武器種を取得
        String weaponType = ClientSkillSlotHandler.getCurrentWeaponName();

        if (weaponType != null && !weaponType.equals("None")) {
            // クライアントハンドラから現在のスロット情報を取得
            // (サーバーからのパケットで同期されている前提)
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

                        // HUDの描画位置計算
                        int x = hudX + (width - (SLOT_COUNT * hudSize + (SLOT_COUNT - 1) * SLOT_SPACING)) / 2 + i * (hudSize + SLOT_SPACING);
                        int y = hudY + 10;

                        // 選択中のスロットを強調表示
                        if (i == selectedSlot) {
                            guiGraphics.fill(x - 2, y - 2, x + hudSize + 2, y + hudSize + 2, 0x80FFFFFF);
                        }

                        // アイコン描画
                        guiGraphics.blit(icon, x, y, 0, 0, hudSize, hudSize, hudSize, hudSize);

                        // クールダウン表示
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