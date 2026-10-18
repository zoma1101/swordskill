package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientProficiencyHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillLevelHandler;
import com.zoma1101.swordskill.client.handler.ClientUnlockedSkillsHandler;
import com.zoma1101.swordskill.payload.CheckSkillUnlockedPayload;
import com.zoma1101.swordskill.payload.RequestSkillLevelsPayload;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;

/**
 * 熟練度の確認用画面。左にカテゴリ（武器種）ごとの熟練度・ポイント、右にそのカテゴリのスキルごとの
 * レベル・解放状況・解放に必要な熟練度を一覧表示する。
 * ステータス画面（{@link StatusScreen}）から開ける。
 */
@OnlyIn(Dist.CLIENT)
public class ProficiencyOverviewScreen extends Screen {

    private static final int PAD = 12;
    private static final int CATEGORY_ROW = 20;
    private static final int SKILL_ROW = 22;
    private static final int ICON_SIZE = 16;
    /** スクロールバーとテキストが重ならないように確保する右端の余白。 */
    private static final int SCROLLBAR_GUTTER = 8;

    private static final int COLOR_PANEL = 0xE6101116;
    private static final int COLOR_ACCENT = 0xFF5ADFFF;
    private static final int COLOR_TEXT = 0xFFF0F0F0;
    private static final int COLOR_TEXT_DIM = 0xFFA0A8B0;
    private static final int COLOR_TEXT_LOCKED = 0xFF8A8A8A;
    private static final int COLOR_HIGHLIGHT = 0x40FFFFFF;

    private final List<SkillData.WeaponType> categories = new ArrayList<>();
    private final List<SkillData> allSkills = new ArrayList<>();
    private int selectedCategory;
    private int categoryScroll;
    private int skillScroll;

    public ProficiencyOverviewScreen() {
        super(Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_title"));
        allSkills.addAll(SwordSkillRegistry.SKILLS.values());
        allSkills.sort(java.util.Comparator.comparingInt(SkillData::getId));
        collectCategories();
        PacketDistributor.sendToServer(new CheckSkillUnlockedPayload());
        PacketDistributor.sendToServer(new RequestSkillLevelsPayload());
    }

    private void collectCategories() {
        categories.clear();
        for (SkillData.WeaponType type : SkillData.WeaponType.values()) {
            if (type == SkillData.WeaponType.NONE) {
                continue;
            }
            for (SkillData skill : allSkills) {
                if (isListed(skill) && skill.getProficiencyCategory() == type) {
                    categories.add(type);
                    break;
                }
            }
        }
    }

    private static boolean isListed(SkillData skill) {
        return skill != null && skill.isAssignable();
    }

    private List<SkillData> skillsOf(SkillData.WeaponType category) {
        List<SkillData> result = new ArrayList<>();
        for (SkillData skill : allSkills) {
            if (isListed(skill) && skill.getProficiencyCategory() == category) {
                result.add(skill);
            }
        }
        return result;
    }

    @Override
    protected void init() {
        int buttonWidth = 110;
        int gap = 8;
        int totalWidth = buttonWidth * 2 + gap;
        int buttonX = width / 2 - totalWidth / 2;
        int buttonY = panelBottom() - 24;
        addRenderableWidget(Button
                .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".status_back"),
                        button -> Minecraft.getInstance().setScreen(new StatusScreen()))
                .bounds(buttonX, buttonY, buttonWidth, 20)
                .build());
        addRenderableWidget(Button
                .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_close"),
                        button -> onClose())
                .bounds(buttonX + buttonWidth + gap, buttonY, buttonWidth, 20)
                .build());
    }

    // ==================================================================
    // レイアウト
    // ==================================================================

    private int panelLeft() {
        return PAD;
    }

    private int panelTop() {
        return PAD;
    }

    private int panelRight() {
        return width - PAD;
    }

    private int panelBottom() {
        return height - PAD;
    }

    private int contentTop() {
        return panelTop() + 46;
    }

    private int headerY() {
        return panelTop() + 22;
    }

    private int summaryY() {
        return panelTop() + 33;
    }

    private int contentBottom() {
        return panelBottom() - 32;
    }

    private int leftX() {
        return panelLeft() + 10;
    }

    private int leftWidth() {
        return Mth.clamp((panelRight() - panelLeft()) / 3, 96, 150);
    }

    private int rightX() {
        return leftX() + leftWidth() + 14;
    }

    private int rightWidth() {
        return Math.max(40, panelRight() - 12 - rightX());
    }

    private int categoryRowY(int index) {
        return contentTop() + index * CATEGORY_ROW - categoryScroll;
    }

    private int skillRowY(int index) {
        return contentTop() + index * SKILL_ROW - skillScroll;
    }

    private int maxCategoryScroll() {
        int visible = contentBottom() - contentTop();
        return Math.max(0, categories.size() * CATEGORY_ROW - visible);
    }

    private int maxSkillScroll() {
        if (categories.isEmpty()) {
            return 0;
        }
        int visible = contentBottom() - contentTop();
        return Math.max(0, skillsOf(categories.get(selectedCategory)).size() * SKILL_ROW - visible);
    }

    // ==================================================================
    // 入力
    // ==================================================================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= leftX() && mouseX <= leftX() + leftWidth()) {
            for (int index = 0; index < categories.size(); index++) {
                int rowY = categoryRowY(index);
                if (mouseY >= rowY && mouseY < rowY + CATEGORY_ROW - 2) {
                    selectedCategory = index;
                    skillScroll = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = (int) Math.round(-scrollY * SKILL_ROW);
        if (mouseX < rightX()) {
            categoryScroll = Mth.clamp(categoryScroll + delta, 0, maxCategoryScroll());
        } else {
            skillScroll = Mth.clamp(skillScroll + delta, 0, maxSkillScroll());
        }
        return true;
    }

    // ==================================================================
    // 描画
    // ==================================================================

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, 0x99000000);
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();
        int bottom = panelBottom();
        guiGraphics.fill(left, top, right, bottom, COLOR_PANEL);
        guiGraphics.fill(left, top, right, top + 1, COLOR_ACCENT);
        guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_ACCENT);
        guiGraphics.fill(left, top, left + 1, bottom, COLOR_ACCENT);
        guiGraphics.fill(right - 1, top, right, bottom, COLOR_ACCENT);

        guiGraphics.drawCenteredString(font, title, width / 2, top + 8, COLOR_TEXT);

        int dividerX = rightX() - 7;
        guiGraphics.fill(dividerX, contentTop() - 4, dividerX + 1, contentBottom(), 0x40FFFFFF);

        drawCategories(guiGraphics, mouseX, mouseY);
        drawSkills(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawCategories(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_category_header"),
                leftX(), headerY(), COLOR_TEXT_DIM, false);
        if (categories.isEmpty()) {
            guiGraphics.drawString(font,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_empty"),
                    leftX(), contentTop(), COLOR_TEXT_DIM, false);
            return;
        }
        int maxScroll = maxCategoryScroll();
        int gutter = maxScroll > 0 ? SCROLLBAR_GUTTER : 2;
        int textRight = leftX() + leftWidth() - gutter;
        guiGraphics.enableScissor(leftX() - 3, contentTop(), leftX() + leftWidth(), contentBottom());
        for (int index = 0; index < categories.size(); index++) {
            int rowY = categoryRowY(index);
            if (rowY + CATEGORY_ROW < contentTop() || rowY > contentBottom()) {
                continue;
            }
            SkillData.WeaponType category = categories.get(index);
            boolean selected = index == selectedCategory;
            boolean hovered = mouseX >= leftX() && mouseX <= leftX() + leftWidth()
                    && mouseY >= rowY && mouseY < rowY + CATEGORY_ROW - 2;
            if (selected) {
                guiGraphics.fill(leftX() - 3, rowY - 2, leftX() + leftWidth(), rowY + CATEGORY_ROW - 4, 0x4030A8E0);
            } else if (hovered) {
                guiGraphics.fill(leftX() - 3, rowY - 2, leftX() + leftWidth(), rowY + CATEGORY_ROW - 4, COLOR_HIGHLIGHT);
            }

            Component name = Component.translatable(SwordSkill.MOD_ID + ".weapon." + category.name().toLowerCase());

            int proficiency = ClientProficiencyHandler.getProficiency(category);
            int points = ClientProficiencyHandler.getAvailablePoints(category);
            Component value = Component.literal(proficiency + (points > 0 ? "  (+" + points + ")" : ""));
            int valueX = Math.max(leftX(), textRight - font.width(value));
            int nameLimit = Math.max(leftX() + 8, valueX - 6);
            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(name.getString(), Math.max(8, nameLimit - leftX())),
                    leftX(), rowY, selected ? COLOR_ACCENT : COLOR_TEXT, false);
            guiGraphics.drawString(font, value, valueX, rowY, points > 0 ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
        }
        guiGraphics.disableScissor();
        drawScrollHint(guiGraphics, leftX(), leftWidth(), categoryScroll, maxScroll);
    }

    private void drawSkills(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (categories.isEmpty()) {
            return;
        }
        SkillData.WeaponType category = categories.get(selectedCategory);
        Component header = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_skill_header");
        guiGraphics.drawString(font, header, rightX(), headerY(), COLOR_TEXT_DIM, false);

        int perLevel = ClientProficiencyHandler.getPointsPerLevel();
        int proficiency = ClientProficiencyHandler.getProficiency(category);
        int progress = ClientProficiencyHandler.getProgress(category);
        int points = ClientProficiencyHandler.getAvailablePoints(category);
        Component summary = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_summary",
                proficiency, progress, perLevel, points);
        guiGraphics.enableScissor(rightX(), summaryY() - 1, rightX() + rightWidth(),
                summaryY() + font.lineHeight + 1);
        guiGraphics.drawString(font, summary, rightX(), summaryY(), COLOR_TEXT, false);
        guiGraphics.disableScissor();

        List<SkillData> skills = skillsOf(category);
        int maxScroll = maxSkillScroll();
        skillScroll = Mth.clamp(skillScroll, 0, maxScroll);
        if (skills.isEmpty()) {
            guiGraphics.drawString(font,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_empty"),
                    rightX(), contentTop(), COLOR_TEXT_DIM, false);
            return;
        }
        int maxLevel = ClientSkillLevelHandler.getMaxLevel();
        int gutter = maxScroll > 0 ? SCROLLBAR_GUTTER : 2;
        int stateRight = rightX() + rightWidth() - gutter;
        guiGraphics.enableScissor(rightX(), contentTop(), rightX() + rightWidth(), contentBottom());
        for (int index = 0; index < skills.size(); index++) {
            int rowY = skillRowY(index);
            if (rowY + SKILL_ROW < contentTop() || rowY > contentBottom()) {
                continue;
            }
            SkillData skill = skills.get(index);
            boolean unlocked = !UnlockedSkill.get() || ClientUnlockedSkillsHandler.isUnlocked(skill.getId());

            ResourceLocation icon = skill.getIconTexture();
            guiGraphics.blit(icon, rightX(), rowY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            int textX = rightX() + ICON_SIZE + 6;
            Component name = Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName());
            int level = ClientSkillLevelHandler.getLevel(skill.getId());
            Component levelText = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_level",
                    level, maxLevel);

            Component state;
            int stateColor;
            if (unlocked) {
                state = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_unlocked");
                stateColor = COLOR_TEXT_DIM;
            } else {
                state = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_locked",
                        skill.getUnlockProficiency());
                stateColor = COLOR_TEXT_LOCKED;
            }

            // 右端に状態（解放状況／必要な熟練度）を寄せ、名前・レベルは重ならない幅に切り詰める
            int stateWidth = font.width(state);
            int stateX = stateRight - stateWidth;
            boolean showState = stateX - 6 > textX + 12;
            if (!showState) {
                stateX = stateRight;
            }
            int textLimit = Math.max(textX, stateX - 6);
            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(name.getString(), Math.max(8, textLimit - textX)),
                    textX, rowY + 1, unlocked ? COLOR_TEXT : COLOR_TEXT_LOCKED, false);
            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(levelText.getString(), Math.max(8, textLimit - textX)),
                    textX, rowY + 10, level > 1 ? COLOR_ACCENT : COLOR_TEXT_DIM, false);
            if (showState) {
                guiGraphics.drawString(font, state, stateX, rowY + 5, stateColor, false);
            }
        }
        guiGraphics.disableScissor();
        drawScrollHint(guiGraphics, rightX(), rightWidth(), skillScroll, maxScroll);
    }

    private void drawScrollHint(GuiGraphics guiGraphics, int x, int panelWidth, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        int trackTop = contentTop();
        int trackHeight = contentBottom() - contentTop();
        int barHeight = Math.max(12, trackHeight * trackHeight / (trackHeight + maxScroll));
        int barY = trackTop + (int) ((trackHeight - barHeight) * (scroll / (float) maxScroll));
        guiGraphics.fill(x + panelWidth - 2, trackTop, x + panelWidth, contentBottom(), 0x30000000);
        guiGraphics.fill(x + panelWidth - 2, barY, x + panelWidth, barY + barHeight, 0x80FFFFFF);
    }
}
