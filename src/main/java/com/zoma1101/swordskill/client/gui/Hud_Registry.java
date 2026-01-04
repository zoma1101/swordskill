package com.zoma1101.swordskill.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.data.AutoWeaponDataSetter; // 追加
import com.zoma1101.swordskill.data.WeaponData; // 追加
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Hud_Registry {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation SKILL_SLOT_DISPLAY_LAYER = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_display");

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_SPACING = 4;

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {

        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_SLOT_DISPLAY_LAYER, (guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 1. まずJSONデータから武器種を取得
            String weaponType = WeaponTypeDetector.getWeaponName(mc.player.getMainHandItem());

            // 2. JSONになければ、自動判定 (AutoWeaponDataSetter) を試す
            if (weaponType == null) {
                // 必要であればここで Config のチェックを入れることも可能です
                // if (ClientConfig.enableAutoWeapon.get()) { ... }

                WeaponData autoData = AutoWeaponDataSetter.AutoWeaponDataSetting(mc.player.getMainHandItem());

                // 自動判定で有効な武器が見つかった場合 (名前が null でない場合)
                if (autoData != null && autoData.weaponName() != null) {
                    weaponType = autoData.weaponName();
                }
            }

            // 有効な武器を持っている場合のみ描画
            if (weaponType != null && !weaponType.equals("None")) {
                int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo();
                int selectedSlot = ClientTickHandler.getSelectedSlot();
                int hudPosXConfig = ClientConfig.hudPosX.get();
                int hudY = ClientConfig.hudPosY.get() + 10;
                int hudSize = ClientConfig.hudScale.get();

                int hudX = getHudX(hudSize, mc, hudPosXConfig);

                if (skillIds == null || skillIds.length < SLOT_COUNT) {
                    skillIds = new int[SLOT_COUNT];
                    java.util.Arrays.fill(skillIds, -1);
                }

                for (int i = 0; i < SLOT_COUNT; i++) {
                    if (skillIds[i] != -1) {
                        SkillData skill = SwordSkillRegistry.SKILLS.get(skillIds[i]);

                        if (skill != null) {
                            ResourceLocation icon = skill.getIconTexture();
                            int x = hudX + i * (hudSize + SLOT_SPACING);

                            if (i == selectedSlot) {
                                guiGraphics.fill(x - 2, hudY - 2, x + hudSize + 2, hudY + hudSize + 2, 0x80FFFFFF);
                            }

                            RenderSystem.setShaderTexture(0, icon);
                            guiGraphics.blit(icon, x, hudY, 0, 0, hudSize, hudSize, hudSize, hudSize);

                            float cooldownRatio = ClientForgeHandler.getCooldownRatio(i);
                            if (cooldownRatio < 1.0f) {
                                int cooldownHeight = (int) (hudSize * (1.0f - cooldownRatio));
                                guiGraphics.fill(x, hudY + hudSize - cooldownHeight, x + hudSize, hudY + hudSize, 0x80FFFFFF);
                            }
                        }
                    }
                }
            }

            RenderSystem.disableBlend();
        });
        LOGGER.info("Registered Skill Slot Display GUI Layer");
    }

    private static int getHudX(int hudSize, Minecraft mc, int hudPosXConfig) {
        int totalSkillBarWidth = (hudSize * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        if (SLOT_COUNT <= 0) {
            totalSkillBarWidth = 0;
        } else if (SLOT_COUNT == 1) {
            totalSkillBarWidth = hudSize;
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        return (screenWidth - totalSkillBarWidth) / 2 + hudPosXConfig;
    }
}