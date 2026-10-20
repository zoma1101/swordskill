package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.smithing.SmithingForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 鍛冶台の画面。5×5 のグリッドと補強スロット 3 つを並べる。
 *
 * <p>置いた形から作れる武器はサーバーが判定するので、クライアントは同期された
 * 完成スロットをそのまま描く。</p>
 */
public class SmithingForgeScreen extends AbstractContainerScreen<SmithingForgeMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
            "textures/gui/smithing_forge.png");
    /** テクスチャの実寸。imageWidth/imageHeight と揃えないとスロット穴と絵がずれる。 */
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 232;

    public SmithingForgeScreen(SmithingForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 232;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        label(graphics, "gui.swordskill.smithing.reinforce", 8, 112);
        label(graphics, "gui.swordskill.smithing.result", 134, 46);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(this.font, Component.translatable(key), x, y, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        // バニラの AbstractContainerScreen はスロットのツールチップを自動では出さないため、自分で呼ぶ
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
