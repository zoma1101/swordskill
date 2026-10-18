package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.payload.SelectSmithingRecipePayload;
import com.zoma1101.swordskill.payload.SetSmithingModePayload;
import com.zoma1101.swordskill.smithing.ForgedWeaponData;
import com.zoma1101.swordskill.smithing.SmithingForgeMenu;
import com.zoma1101.swordskill.smithing.SmithingIngredient;
import com.zoma1101.swordskill.smithing.SmithingMaterial;
import com.zoma1101.swordskill.smithing.SmithingMode;
import com.zoma1101.swordskill.smithing.SmithingRecipe;
import com.zoma1101.swordskill.smithing.SmithingRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 鍛冶台の画面。上部のボタンでモード（作成・強化・溶解・再鍛造）を切り替える。
 *
 * <p>作成モードではメイン素材を入れるとレシピ（= 武器）の一覧が出る。
 * 一覧から選ぶと対応素材がゴースト表示され、揃うと完成スロットに品が出る。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SmithingForgeScreen extends AbstractContainerScreen<SmithingForgeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/gui/smithing_forge.png");

    private static final int MODE_BUTTON_Y = 18;
    private static final int MODE_BUTTON_WIDTH = 40;
    private static final int MODE_BUTTON_HEIGHT = 14;
    private static final int MODE_BUTTON_GAP = 2;

    /** レシピ一覧の領域（GUI ローカル座標）。 */
    private static final int LIST_X = 8;
    private static final int LIST_Y = 56;
    private static final int LIST_W = 86;
    private static final int LIST_H = 84;
    private static final int ROW_H = 14;
    private static final int ROWS = LIST_H / ROW_H;
    private static final int SCROLLBAR_X = LIST_X + LIST_W - 6;
    private static final int SCROLLBAR_W = 6;

    private final Map<SmithingMode, Button> modeButtons = new EnumMap<>(SmithingMode.class);
    private int scroll;

    public SmithingForgeScreen(SmithingForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 232;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 138;
    }

    /** レシピ一覧を出すモードか（作成・再鍛造）。 */
    private boolean showsRecipeList() {
        SmithingMode mode = getMenu().getMode();
        return mode == SmithingMode.CREATE || mode == SmithingMode.REFORGE;
    }

    @Override
    protected void init() {
        super.init();
        modeButtons.clear();
        SmithingMode[] modes = SmithingMode.values();
        for (int i = 0; i < modes.length; i++) {
            SmithingMode mode = modes[i];
            int x = this.leftPos + 6 + i * (MODE_BUTTON_WIDTH + MODE_BUTTON_GAP);
            Button button = Button.builder(mode.displayName(), b -> selectMode(mode))
                    .bounds(x, this.topPos + MODE_BUTTON_Y, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT)
                    .build();
            modeButtons.put(mode, button);
            addRenderableWidget(button);
        }
        updateModeButtons();
    }

    private void selectMode(SmithingMode mode) {
        if (getMenu().getMode() == mode) {
            return;
        }
        getMenu().setMode(mode);
        PacketDistributor.sendToServer(new SetSmithingModePayload(mode.ordinal()));
        scroll = 0;
        updateModeButtons();
    }

    private void updateModeButtons() {
        SmithingMode current = getMenu().getMode();
        modeButtons.forEach((mode, button) -> button.active = mode != current);
    }

    // ------------------------------------------------------------------
    // 描画
    // ------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 176x232 のテクスチャを等倍で貼る（テクスチャ実寸を渡さないと拡大される）
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        renderInactiveSlots(graphics);
        if (showsRecipeList()) {
            renderRecipeList(graphics, mouseX, mouseY);
        }
        if (getMenu().getMode() == SmithingMode.CREATE) {
            renderGhostIngredients(graphics);
        }
    }

    /** そのモードで使わない入力スロットを暗くして、使えないことを示す。 */
    private void renderInactiveSlots(GuiGraphics graphics) {
        for (int i = 0; i < SmithingForgeMenu.SLOT_RESULT; i++) {
            Slot slot = getMenu().getSlot(i);
            if (slot == null || slot.isActive()) {
                continue;
            }
            int x = this.leftPos + slot.x - 1;
            int y = this.topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xB0202020);
        }
    }

    private void renderRecipeList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<SmithingRecipe> list = getMenu().recipes();
        int selected = getMenu().getSelectedIndex();
        int maxScroll = Math.max(0, list.size() - ROWS);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        int x = this.leftPos + LIST_X;
        int y = this.topPos + LIST_Y;

        if (list.isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("gui.swordskill.smithing.recipe.empty"),
                    x + 2, y + 4, 0x808080, false);
            return;
        }

        SmithingMaterial material = getMenu().material();
        for (int row = 0; row < ROWS; row++) {
            int index = scroll + row;
            if (index >= list.size()) {
                break;
            }
            SmithingRecipe recipe = list.get(index);
            int rowY = y + row * ROW_H;
            boolean hovered = isInList(mouseX, mouseY) && mouseY >= rowY && mouseY < rowY + ROW_H;

            if (index == selected) {
                graphics.fill(x, rowY, x + LIST_W, rowY + ROW_H - 1, 0x66FFFFFF);
            } else if (hovered) {
                graphics.fill(x, rowY, x + LIST_W, rowY + ROW_H - 1, 0x33FFFFFF);
            }

            ItemStack preview = previewStack(recipe, material);
            graphics.pose().pushPose();
            graphics.pose().translate(x + 1.0F, rowY + 1.0F, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(preview, 0, 0);
            graphics.pose().popPose();

            String name = this.font.plainSubstrByWidth(recipe.displayName().getString(), LIST_W - 18);
            graphics.drawString(this.font, name, x + 14, rowY + 3, index == selected ? 0x2B2B00 : 0x404040, false);
        }

        renderScrollbar(graphics, list.size(), maxScroll);
    }

    private void renderScrollbar(GuiGraphics graphics, int size, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int trackX = this.leftPos + SCROLLBAR_X;
        int trackY = this.topPos + LIST_Y;
        int trackH = ROWS * ROW_H;
        int thumbH = Math.max(10, trackH * ROWS / size);
        int thumbY = trackY + (trackH - thumbH) * scroll / maxScroll;
        graphics.fill(trackX, trackY, trackX + SCROLLBAR_W, trackY + trackH, 0x40000000);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFFC6C6C6);
    }

    /** 選んだレシピの対応素材を、空のスロットに薄く表示する。 */
    private void renderGhostIngredients(GuiGraphics graphics) {
        SmithingRecipe recipe = getMenu().selectedRecipe();
        if (recipe == null) {
            return;
        }
        List<SmithingIngredient> needed = recipe.materials();
        for (int i = 0; i < needed.size() && i < SmithingForgeMenu.EXTRA_COUNT; i++) {
            Slot slot = getMenu().getSlot(SmithingForgeMenu.SLOT_EXTRA + i);
            if (slot == null || !slot.isActive() || slot.hasItem()) {
                continue;
            }
            ItemStack ghost = needed.get(i).displayStack();
            if (ghost.isEmpty()) {
                continue;
            }
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.45F);
            graphics.renderItem(ghost, x, y);
            graphics.renderItemDecorations(this.font, ghost, x, y);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        SmithingMode mode = getMenu().getMode();
        switch (mode) {
            case CREATE -> {
                label(graphics, "gui.swordskill.smithing.slot.material", 28, 39);
                label(graphics, "gui.swordskill.smithing.slot.extras", 100, 45);
                label(graphics, "gui.swordskill.smithing.slot.result", 146, 45);
            }
            case ENHANCE -> {
                label(graphics, "gui.swordskill.smithing.slot.target", 28, 39);
                label(graphics, "gui.swordskill.smithing.slot.enhance", 100, 45);
                label(graphics, "gui.swordskill.smithing.slot.result", 146, 45);
            }
            case MELT -> {
                label(graphics, "gui.swordskill.smithing.slot.target", 28, 39);
                label(graphics, "gui.swordskill.smithing.slot.result", 146, 45);
            }
            case REFORGE -> {
                label(graphics, "gui.swordskill.smithing.slot.ingot", 28, 39);
                label(graphics, "gui.swordskill.smithing.slot.material", 100, 45);
                label(graphics, "gui.swordskill.smithing.slot.result", 146, 45);
            }
        }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(this.font, Component.translatable(key), x, y, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!showsRecipeList()) {
            return;
        }
        int index = recipeIndexAt(mouseX, mouseY);
        List<SmithingRecipe> list = getMenu().recipes();
        if (index >= 0 && index < list.size()) {
            graphics.renderTooltip(this.font, recipeTooltip(list.get(index)), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> recipeTooltip(SmithingRecipe recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.displayName());
        SmithingMaterial material = getMenu().material();
        if (material != null) {
            lines.add(Component.translatable("gui.swordskill.smithing.recipe.material",
                    material.displayName(), recipe.count()));
        }
        if (getMenu().getMode() == SmithingMode.CREATE) {
            for (SmithingIngredient ingredient : recipe.materials()) {
                lines.add(Component.translatable("gui.swordskill.smithing.recipe.extra",
                        ingredientName(ingredient), ingredient.count()));
            }
        }
        return lines;
    }

    private Component ingredientName(SmithingIngredient ingredient) {
        ItemStack stack = ingredient.displayStack();
        if (!stack.isEmpty()) {
            return stack.getHoverName();
        }
        return Component.literal(ingredient.tag() != null ? "#" + ingredient.tag() : "");
    }

    /** 一覧に出す見た目のプレビュー（素材の色で染まる）。 */
    private ItemStack previewStack(SmithingRecipe recipe, SmithingMaterial material) {
        ItemStack stack = new ItemStack(SmithingRegistry.FORGED_WEAPON.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(recipe.modelIndex()));
        if (material != null) {
            CompoundTag forge = new CompoundTag();
            forge.putInt("color_main", material.colorMain());
            forge.putInt("color_sub", material.colorSub());
            CompoundTag root = new CompoundTag();
            root.put(ForgedWeaponData.ROOT_KEY, forge);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return stack;
    }

    // ------------------------------------------------------------------
    // 入力
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && showsRecipeList()) {
            int index = recipeIndexAt(mouseX, mouseY);
            if (index >= 0) {
                getMenu().selectRecipe(index);
                PacketDistributor.sendToServer(new SelectSmithingRecipePayload(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (showsRecipeList() && isInList(mouseX, mouseY)) {
            int maxScroll = Math.max(0, getMenu().recipes().size() - ROWS);
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isInList(double mouseX, double mouseY) {
        int x = this.leftPos + LIST_X;
        int y = this.topPos + LIST_Y;
        return mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROWS * ROW_H;
    }

    /** マウス位置にあるレシピ番号。無ければ -1。 */
    private int recipeIndexAt(double mouseX, double mouseY) {
        if (!isInList(mouseX, mouseY)) {
            return -1;
        }
        int row = (int) ((mouseY - (this.topPos + LIST_Y)) / ROW_H);
        int index = scroll + row;
        return index < getMenu().recipes().size() ? index : -1;
    }
}
