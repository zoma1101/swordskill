package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.data.WeaponTypeUtils;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillRequestPacket;
import com.zoma1101.swordskill.network.SkillSlotSelectionPacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

@OnlyIn(Dist.CLIENT)
public class SwordSkillSelectionScreen extends Screen {

    public int selectedSkillIndex = 0;
    private Set<SkillData.WeaponType> weaponType; // 追加

    public SwordSkillSelectionScreen() {
        super(Component.translatable("gui."+SwordSkill.MOD_ID+".title"));
        if (Minecraft.getInstance().player != null) {
            weaponType = WeaponTypeUtils.getWeaponType(); // 初期化
        }
        NetworkHandler.INSTANCE.sendToServer(new SkillRequestPacket());
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            selectedSkillIndex = getPreviousDisplayableSkillIndex(selectedSkillIndex);
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            selectedSkillIndex = getNextDisplayableSkillIndex(selectedSkillIndex);
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            onClose();
            return true;
        }

        boolean handled = super.keyPressed(keyCode, scanCode, modifiers); // super.keyPressed() を先に実行

        if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_0.getKey().getValue()) {
            saveSkillToSlot(0);
            return true;
        } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_1.getKey().getValue()) {
            saveSkillToSlot(1);
            return true;
        } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_2.getKey().getValue()) {
            saveSkillToSlot(2);
            return true;
        } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_3.getKey().getValue()) {
            saveSkillToSlot(3);
            return true;
        } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_4.getKey().getValue()) {
            saveSkillToSlot(4);
            return true;
        }

        return handled; // super.keyPressed() の結果を返す
    }

    @Override
    protected void init() {
        super.init();

        // 決定ボタンを追加
        addRenderableWidget(Button.builder(Component.translatable("gui."+SwordSkill.MOD_ID+".select"), button -> openSlotSelectionScreen()).bounds(width / 2 - 50, height - 40, 100, 20).build());
    }

    private void openSlotSelectionScreen() {
        Minecraft.getInstance().setScreen(new DecideSkillScreen(selectedSkillIndex));
    }



    private void saveSkillToSlot(int slotIndex) {
        if (minecraft != null && minecraft.player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
            if (skill != null) {
                String playerWeaponType = WeaponTypeUtils.getWeaponName(); // 追加
                if (playerWeaponType != null  && !playerWeaponType.equals("None")) { // 追加
                    NetworkHandler.INSTANCE.sendToServer(new SkillSlotSelectionPacket(skill.getId(), slotIndex, playerWeaponType)); // 修正
                } // 追加
            }
        }
    }


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);

        int centerX = width / 2;
        int centerY = height / 2;

        SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);

        if (skill != null && !skill.isHide()) {
            guiGraphics.drawString(font, Component.translatable("gui." + SwordSkill.MOD_ID + ".cooldown", skill.getCooldown()).getString(), centerX - 50, centerY - 48, 0X00FF7F);

            // 使用可能な武器をアイコンで表示
            int iconX = centerX - 50;
            int iconY = centerY - 35;
            for (SkillData.WeaponType type : skill.getAvailableWeaponTypes()) {
                ResourceLocation iconTexture = getWeaponIconTexture(type); // アイコンテクスチャを取得
                guiGraphics.blit(iconTexture, iconX, iconY, 0, 0, 16, 16, 16, 16); // アイコンを描画

                // マウスカーソルがアイコンの上にあるかどうかを判定
                if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= iconY && mouseY < iconY + 16) {
                    // ツールチップで武器名を表示
                    guiGraphics.renderTooltip(font, Component.translatable(SwordSkill.MOD_ID + ".weapon." + type.name().toLowerCase()), mouseX, mouseY);
                }

                iconX += 18; // 次のアイコンのX座標を調整
            }

            int nameX = centerX - 50;
            int nameY = centerY - 60;
            guiGraphics.drawString(font, Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName()).getString(), nameX, nameY, 0x00FFAA);

            // スキルの説明を複数行で表示
            String descriptionKey = SwordSkill.MOD_ID + ".skill." + skill.getName() + ".description";
            String description = Component.translatable(descriptionKey).getString();
            String[] descriptionLines = description.split("\n"); // 改行で分割

            int descriptionY = centerY - 15; // 最初の行のY座標
            for (String line : descriptionLines) {
                guiGraphics.drawString(font, line, centerX - 120, descriptionY, 0xFFFFFF);
                descriptionY += font.lineHeight + 2; // 次の行のY座標を調整
            }

            if (skill.getIconTexture() != null) {
                int weapon_iconX = centerX - 90;
                int weapon_iconY = centerY - 60;
                guiGraphics.blit(skill.getIconTexture(), weapon_iconX, weapon_iconY, 0, 0, 32, 32, 32, 32);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }


    @Override
    public void onClose() {
        super.onClose();
    }

    private int getPreviousDisplayableSkillIndex(int currentIndex) {
        int previousIndex = currentIndex;
        do {
            previousIndex = (previousIndex - 1 + SwordSkillRegistry.SKILLS.size()) % SwordSkillRegistry.SKILLS.size();
        } while (SwordSkillRegistry.SKILLS.get(previousIndex).isHide() || SwordSkillRegistry.SKILLS.get(previousIndex).getAvailableWeaponTypes().stream().noneMatch(weaponType::contains));
        return previousIndex;
    }

    private int getNextDisplayableSkillIndex(int currentIndex) {
        int nextIndex = currentIndex;
        do {
            nextIndex = (nextIndex + 1) % SwordSkillRegistry.SKILLS.size();
        } while (SwordSkillRegistry.SKILLS.get(nextIndex).isHide() || SwordSkillRegistry.SKILLS.get(nextIndex).getAvailableWeaponTypes().stream().noneMatch(weaponType::contains));
        return nextIndex;
    }

    // 武器種ごとのアイコンテクスチャを取得するメソッド
    private ResourceLocation getWeaponIconTexture(SkillData.WeaponType type) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/gui/weapon_icons/" + type.name().toLowerCase() + ".png");
    }
}