package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.smithing.MeltingForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** 溶解炉の画面。鍛造武器を 1 つ入れるとインゴットが出る。 */
public class MeltingForgeScreen extends AbstractContainerScreen<MeltingForgeMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
            "textures/gui/melting_forge.png");
    private static final int TEXTURE_SIZE = 256;

    public MeltingForgeScreen(MeltingForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // スロットのツールチップは自分で呼ぶ必要がある
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
