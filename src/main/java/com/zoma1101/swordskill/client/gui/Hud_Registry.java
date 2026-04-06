package com.zoma1101.swordskill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Hud_Registry {

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_SPACING = 4;

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {

        final ResourceLocation SKILL_SLOT_DISPLAY_LAYER = ResourceLocation
                .fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_display");

        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_SLOT_DISPLAY_LAYER, (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // ClientSkillSlotHandlerから現在の武器情報を取得 (サーバー同期済み)
            String weaponType = ClientSkillSlotHandler.getCurrentWeaponName();

            // スキルが一つもセットされていないかチェック
            int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo();
            boolean hasAnySkill = false;
            for (int id : skillIds) {
                if (id != -1) {
                    hasAnySkill = true;
                    break;
                }
            }

            // 有効な武器を持っており、かつスキルが1つ以上セットされている場合のみ描画
            if (weaponType != null && !weaponType.equals("None") && hasAnySkill) {
                int selectedSlot = ClientTickHandler.getSelectedSlot();
                int hudPosXConfig = ClientConfig.hudPosX.get();
                int hudYConfig = ClientConfig.hudPosY.get();
                int hudSize = ClientConfig.hudScale.get();

                int screenWidth = mc.getWindow().getGuiScaledWidth();

                // 描画位置の計算 (参考コードの計算ロジックに合わせる)
                int totalWidth = SLOT_COUNT * hudSize + (SLOT_COUNT - 1) * SLOT_SPACING;
                int hudX = hudPosXConfig + (screenWidth - totalWidth) / 2;
                int hudY = hudYConfig + 10;

                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (skillIds[i] != -1) {
                        SkillData skill = SwordSkillRegistry.SKILLS.get(skillIds[i]);
                        if (skill != null) {
                            ResourceLocation icon = skill.getIconTexture();
                            int x = hudX + i * (hudSize + SLOT_SPACING);

                            // 選択中のスロットを強調表示
                            if (i == selectedSlot) {
                                guiGraphics.fill(x - 2, hudY - 2, x + hudSize + 2, hudY + hudSize + 2, 0x80FFFFFF);
                            }

                            // アイコン描画
                            guiGraphics.blit(icon, x, hudY, 0, 0, hudSize, hudSize, hudSize, hudSize);

                            // クールダウン表示
                            float cooldownRatio = ClientForgeHandler.getCooldownRatio(i);
                            if (cooldownRatio < 1.0f) {
                                int cooldownHeight = (int) (hudSize * (1.0f - cooldownRatio));
                                guiGraphics.fill(x, hudY + hudSize - cooldownHeight, x + hudSize, hudY + hudSize,
                                        0x80FFFFFF);
                            }
                        }
                    }
                }

                // --- SPバー描画 ---
                double currentSP = com.zoma1101.swordskill.network.toClient.ClientSPData.get();
                double maxSP = com.zoma1101.swordskill.network.toClient.ClientSPData.getMax();
                if (maxSP <= 0) {
                    maxSP = mc.player.getAttributeValue(SwordSkillAttribute.MAX_SP);
                }
                float spRatio = (float) (currentSP / maxSP);

                int barX = hudX;
                int barY = hudY + hudSize; // スキルバーのすぐ下
                int barHeight = 5;

                ResourceLocation spBarLocation = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
                        "textures/gui/sp_bar.png");

                // 1. バーの背景描画 (sp_bar.png の上部 0~5px)
                guiGraphics.blit(spBarLocation, barX, barY, totalWidth, barHeight, 0, 0, totalWidth, 5, totalWidth, 10);

                // 2. バーの前面描画 (sp_bar.png の下部 5~10px)
                int filledWidth = (int) (totalWidth * spRatio);
                if (filledWidth > 0) {
                    guiGraphics.blit(spBarLocation, barX, barY, filledWidth, barHeight, 0, 5, filledWidth, 5,
                            totalWidth, 10);
                }
            }

            RenderSystem.disableBlend();
        });
    }

}