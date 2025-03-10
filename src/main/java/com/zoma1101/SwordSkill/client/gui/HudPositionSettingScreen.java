package com.zoma1101.SwordSkill.client.gui;

import com.zoma1101.SwordSkill.config.ClientConfig;
import com.zoma1101.SwordSkill.main.SwordSkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class HudPositionSettingScreen extends Screen {

    private int moveAmount = 1;

    public HudPositionSettingScreen() {
        super(Component.literal("HUD Position Adjustment"));
    }

    @Override
    protected void init() {
        super.init();

        // 移動量スライダー
        this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 150, this.height - 80, 150, 20, Component.translatable("gui."+SwordSkill.MOD_ID+".setting.hud.move_scale", moveAmount), (moveAmount - 0.0) / 5.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.translatable("gui."+SwordSkill.MOD_ID+".setting.hud.move_scale", (int) (1+this.value * 5)));
            }

            @Override
            protected void applyValue() {
                moveAmount = (int) (1+this.value * 5);
            }
        });

        // スケール調整スライダー
        this.addRenderableWidget(new AbstractSliderButton(this.width / 2, this.height - 80, 150, 20, Component.translatable("gui."+SwordSkill.MOD_ID+".setting.hud.scale", ClientConfig.hudScale.get()), (ClientConfig.HUD_SCALE_MAX - 0.0) / ClientConfig.HUD_SCALE_MAX) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.translatable("gui."+SwordSkill.MOD_ID+".setting.hud.scale", (int) (this.value * ClientConfig.HUD_SCALE_MAX)));
            }

            @Override
            protected void applyValue() {
                ClientConfig.hudScale.set((int) (this.value * ClientConfig.HUD_SCALE_MAX));
                ClientConfig.SPEC.save();
            }
        });

        // 保存ボタン
        this.addRenderableWidget(Button.builder(Component.translatable("gui."+SwordSkill.MOD_ID+".setting.hud.save"), button -> {
            ClientConfig.SPEC.save();
            this.onClose();
        }).bounds(this.width / 2 - 75, this.height - 40, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_W:
                ClientConfig.hudPosY.set(ClientConfig.hudPosY.get() - moveAmount);
                break;
            case GLFW.GLFW_KEY_A:
                ClientConfig.hudPosX.set(ClientConfig.hudPosX.get() - moveAmount);
                break;
            case GLFW.GLFW_KEY_S:
                ClientConfig.hudPosY.set(ClientConfig.hudPosY.get() + moveAmount);
                break;
            case GLFW.GLFW_KEY_D:
                ClientConfig.hudPosX.set(ClientConfig.hudPosX.get() + moveAmount);
                break;
        }
        ClientConfig.SPEC.save();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }
}