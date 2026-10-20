package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientProficiencyHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillLevelHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.payload.*;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SkillChains;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.zoma1101.swordskill.client.gui.SaoShapes.fillClipped;
import static com.zoma1101.swordskill.client.gui.SaoShapes.fillClippedGradient;
import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

@OnlyIn(Dist.CLIENT)
public class SwordSkillSelectionScreen extends Screen {

    private static final float UI_TEXT_SCALE = 0.9F;

    /** SAO UI の武器種タブに並べる武器種（NONE は除外）。 */
    private static final SkillData.WeaponType[] TAB_TYPES = {
            SkillData.WeaponType.ONE_HANDED_SWORD,
            SkillData.WeaponType.TWO_HANDED_SWORD,
            SkillData.WeaponType.KATANA,
            SkillData.WeaponType.AXE,
            SkillData.WeaponType.RAPIER,
            SkillData.WeaponType.CLAW,
            SkillData.WeaponType.SPEAR,
            SkillData.WeaponType.MACE,
            SkillData.WeaponType.SCYTHE,
            SkillData.WeaponType.DAGGER,
            SkillData.WeaponType.DUALSWORD
    };

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_DIAMOND_GAP = 10;
    /** 派生スキルのリンク図ノード。 */
    private static final int CHAIN_NODE_SIZE = 16;
    private static final int CHAIN_NODE_STEP = 21;
    private static final long SLIDE_DURATION_MS = 180L;
    private static final long FLASH_DURATION_MS = 260L;
    private static final long WARNING_DURATION_MS = 2400L;

    /** ライト／ダークはステータス画面・熟練度一覧画面と共通（クライアント設定に保存）。 */
    private static boolean isDark() {
        return SaoTheme.dark();
    }

    public Set<Integer> unlockedSkills = new HashSet<>();
    public int selectedSkillIndex = 0;
    private Set<SkillData.WeaponType> weaponType;

    /** 絞り込み中の武器種。null のときは装備中の武器種で自動絞り込みを行う。 */
    private SkillData.WeaponType activeFilter;
    /** 説明欄のページ番号（0始まり）。 */
    private int contentPage;
    private int lastPageCount = 1;
    /** 攻撃倍率リストの先頭に表示している行。 */
    private int powerScroll;
    /** 武器種タブの先頭インデックス（タブが多くて入りきらないときのみ 0 以外になる）。 */
    private int tabScroll;

    private long slideStartMs = Long.MIN_VALUE;
    private int slideDirection;
    private long flashStartMs = Long.MIN_VALUE;
    /** 装備武器種と合わないスキルをセットしようとしたときの警告メッセージ。 */
    private String warningKey;
    private long warningStartMs = Long.MIN_VALUE;

    /** 派生リンク図ノードの当たり判定（描画時に更新）。 */
    private final List<ChainNodeHit> chainNodeHits = new ArrayList<>();
    private int hoveredSlot = -1;

    public SwordSkillSelectionScreen() {
        super(Component.translatable("gui." + SwordSkill.MOD_ID + ".title"));
        if (Minecraft.getInstance().player != null) {
            weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            PacketDistributor.sendToServer(new CheckSkillUnlockedPayload());
        }

        PacketDistributor.sendToServer(new SkillRequestPayload());
        PacketDistributor.sendToServer(new RequestSkillLevelsPayload());
    }

    // ==================================================================
    // 入力
    // ==================================================================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            changeSelectedSkill(-1);
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            changeSelectedSkill(1);
        } else if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_UP) {
            changePage(-1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_DOWN) {
            changePage(1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET) {
            changePage(-1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            changePage(1);
            return true;
        }

        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);

        if (unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get()) {
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
        }
        return handled;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Layout layout = computeLayout();
            int arrow = hitTestTabArrow(layout, mouseX, mouseY);
            if (arrow != 0) {
                changeTabScroll(arrow);
                return true;
            }
            int tab = hitTestTab(layout, mouseX, mouseY);
            if (tab >= 0) {
                selectWeaponTab(tab);
                return true;
            }
            int slot = hitTestSlot(layout, mouseX, mouseY);
            if (slot >= 0) {
                saveSkillToSlot(slot);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            Layout layout = computeLayout();
            int delta = scrollY < 0.0D ? 1 : -1;
            if (isOverTabRow(layout, mouseX, mouseY) && layout.tabScrollable()) {
                changeTabScroll(delta);
                return true;
            }
            if (isOverPowerPanel(layout, mouseX, mouseY)) {
                powerScroll = Math.max(0, powerScroll + delta);
                return true;
            }
            changePage(delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void changeSelectedSkill(int direction) {
        int next = direction < 0
                ? getPreviousDisplayableSkillIndex(selectedSkillIndex)
                : getNextDisplayableSkillIndex(selectedSkillIndex);
        if (next != selectedSkillIndex) {
            selectedSkillIndex = next;
            contentPage = 0;
            powerScroll = 0;
            triggerSlide(direction);
        }
    }

    private void changePage(int delta) {
        int next = Mth.clamp(contentPage + delta, 0, Math.max(0, lastPageCount - 1));
        contentPage = next;
    }

    private void selectWeaponTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= TAB_TYPES.length) {
            return;
        }
        SkillData.WeaponType type = TAB_TYPES[tabIndex];
        int direction = 1;
        if (activeFilter == type) {
            // 同じタブを再クリックしたら「装備武器種で自動」に戻す
            activeFilter = null;
        } else {
            SkillData.WeaponType previous = activeFilter;
            activeFilter = type;
            direction = previous == null ? 1 : (type.ordinal() >= previous.ordinal() ? 1 : -1);
        }
        int first = firstMatchingSkillIndex();
        if (first >= 0 && first != selectedSkillIndex) {
            selectedSkillIndex = first;
        }
        contentPage = 0;
        powerScroll = 0;
        triggerSlide(direction);
        triggerFlash(0.6F);
    }

    private void changeTabScroll(int delta) {
        int visible = computeLayout().tabVisibleCount();
        int max = Math.max(0, TAB_TYPES.length - visible);
        tabScroll = Mth.clamp(tabScroll + delta, 0, max);
    }

    /** 装備中の武器種では使えないスキルをセットしようとしたときの警告。 */
    private void showWarning(String key) {
        warningKey = key;
        warningStartMs = Util.getMillis();
    }

    private void triggerSlide(int direction) {
        slideDirection = direction;
        slideStartMs = Util.getMillis();
    }

    private void triggerFlash(float strength) {
        flashStartMs = Util.getMillis();
    }

    // ==================================================================
    // レイアウト
    // ==================================================================

    private record Layout(
            boolean compact,
            int cardLeft, int cardWidth, int cardTop, int cardBottom, int cornerCut,
            int tabY, int tabIconSize, int tabStep, int tabStartX, int tabVisibleCount,
            int iconCenterY, int selectedIconSize, int nameY, int levelY, int cooldownY,
            int weaponIconY, int descriptionTop,
            int powerPanelWidth, int diamondSize, int diamondTop, int footerTop,
            boolean canAssign, boolean needsUnlock, boolean hasDerivedButton, boolean canLevelUp,
            int themeButtonSize, int themeButtonX, int themeButtonY) {

        /** 説明文の描画幅（右側の攻撃倍率パネルを除いた分）。 */
        int descriptionWidth() {
            return Math.max(60, cardWidth - 28 - powerPanelWidth - 8);
        }

        /** 攻撃倍率パネルの左端。 */
        int powerPanelLeft() {
            return cardLeft + cardWidth - 14 - powerPanelWidth;
        }

        /** タブ列全体の幅。 */
        int tabUsedWidth() {
            return (tabVisibleCount - 1) * tabStep + tabIconSize;
        }

        /** タブが全部入りきらない（＝スクロール可能）か。 */
        boolean tabScrollable() {
            return TAB_TYPES.length > tabVisibleCount;
        }
    }

    private Layout computeLayout() {
        boolean compact = height < 280;
        int cardWidth = Math.min(340, Math.max(width - 28, 1));
        int cardLeft = (width - cardWidth) / 2;
        int cardTop = 8;
        int cardBottom = Math.max(cardTop + 1, height - 8);
        int cornerCut = compact ? 7 : 12;

        int tabY = cardTop + (compact ? 15 : 33);
        int tabIconSize = compact ? 12 : 16;
        int tabStep = compact ? 16 : 21;
        // 武器種タブは幅に応じて表示数を決め、入りきらない分はスクロール（矢印／ホイール）で見る
        int tabAreaWidth = Math.max(tabIconSize * 3, cardWidth - 20);
        int tabVisibleCount = Mth.clamp((tabAreaWidth - tabIconSize) / tabStep + 1, 1, TAB_TYPES.length);
        tabScroll = Mth.clamp(tabScroll, 0, Math.max(0, TAB_TYPES.length - tabVisibleCount));
        int tabStartX = width / 2 - ((tabVisibleCount - 1) * tabStep + tabIconSize) / 2;

        int iconCenterY = cardTop + (compact ? 48 : 76);
        int selectedIconSize = compact ? 28 : 32;
        int nameY = cardTop + (compact ? 64 : 95);
        int levelY = cardTop + (compact ? 75 : 105);
        int cooldownY = cardTop + (compact ? 75 : 117);
        int weaponIconY = cardTop + (compact ? 86 : 129);
        int descriptionTop = cardTop + (compact ? 106 : 157);

        // 右側の攻撃倍率パネルの幅（説明文は残り幅に折り返す）
        int contentWidth = Math.max(40, cardWidth - 28);
        int powerPanelWidth = Math.min(compact ? 86 : 106, Math.max(52, contentWidth / 3));

        SkillData selectedSkillForLayout = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        boolean assignableSkill = selectedSkillForLayout != null && selectedSkillForLayout.isAssignable();
        // スロット（現在のセット内容）は常に表示する。セット可能かどうかだけを別に判定する
        boolean canAssign = unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get();
        // 説明枠（assignable=false）は解放も派生解放もレベルアップもできない
        boolean hasDerivedButton = assignableSkill && canAssign && hasDerivedUnlock();
        boolean needsUnlock = !canAssign && assignableSkill;
        boolean canLevelUp = assignableSkill && canAssign && canLevelUpSelected();

        int diamondSize = compact ? 26 : 32;
        // ウィンドウ幅が狭いときはスロットがカードからはみ出さないように縮める
        int maxDiamondTotal = Math.max(SLOT_COUNT * 4, cardWidth - 16);
        if (SLOT_COUNT * (diamondSize + SLOT_DIAMOND_GAP) - SLOT_DIAMOND_GAP > maxDiamondTotal) {
            diamondSize = Math.max(8, (maxDiamondTotal + SLOT_DIAMOND_GAP) / SLOT_COUNT - SLOT_DIAMOND_GAP);
        }
        int diamondTop = cardBottom - 6 - diamondSize;

        // ボタンは菱形の上に下から積む（派生 → 解放 / レベルアップ）
        int buttonRows = 0;
        if (hasDerivedButton) {
            buttonRows++;
        }
        if (needsUnlock || canLevelUp) {
            buttonRows++;
        }
        int buttonTop = diamondTop - buttonRows * 24;
        int footerTop = buttonTop - 10;

        // テーマ切替ボタン（右上）。通常は 32x32、コンパクト時は 16x16 で表示する
        int themeButtonSize = compact ? 16 : 32;
        int themeButtonX = cardLeft + cardWidth - (compact ? 8 : 12) - themeButtonSize;
        int themeButtonY = cardTop + 1;

        return new Layout(compact, cardLeft, cardWidth, cardTop, cardBottom, cornerCut,
                tabY, tabIconSize, tabStep, tabStartX, tabVisibleCount,
                iconCenterY, selectedIconSize, nameY, levelY, cooldownY,
                weaponIconY, descriptionTop, powerPanelWidth, diamondSize, diamondTop, footerTop,
                canAssign, needsUnlock, hasDerivedButton, canLevelUp,
                themeButtonSize, themeButtonX, themeButtonY);
    }

    private void ViewButton(Layout layout) {
        this.clearWidgets();

        addRenderableWidget(SaoButton.icon(
                Component.translatable("gui." + SwordSkill.MOD_ID + ".theme_toggle"),
                isDark() ? SaoTheme.THEME_BUTTON_ON : SaoTheme.THEME_BUTTON_OFF,
                () -> {
                    SaoTheme.toggle();
                    triggerFlash(0.4F);
                },
                layout.themeButtonX(), layout.themeButtonY(), layout.themeButtonSize(), isDark()));

        int buttonWidth = Math.min(160, width - 24);
        int buttonX = width / 2 - buttonWidth / 2;
        int row = 1;

        if (layout.hasDerivedButton()) {
            addRenderableWidget(SaoButton.of(
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock_derived"),
                    this::unlockDerivedSkill,
                    buttonX, layout.diamondTop() - 24 * row, buttonWidth, 20, isDark()));
            row++;
        }
        if (layout.needsUnlock()) {
            SkillData selected = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
            addRenderableWidget(SaoButton.of(
                    unlockButtonLabel(selected),
                    () -> unlockSkill(selectedSkillIndex),
                    buttonX, layout.diamondTop() - 24 * row, buttonWidth, 20, isDark()));
        } else if (layout.canLevelUp()) {
            SkillData selected = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
            int points = ClientProficiencyHandler.getAvailablePoints(
                    selected == null ? null : selected.getProficiencyCategory());
            addRenderableWidget(SaoButton.of(
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".level_up", points),
                    this::levelUpSelectedSkill,
                    buttonX, layout.diamondTop() - 24 * row, buttonWidth, 20, isDark()));
        }
    }

    /** 解放ボタンのラベル。熟練度が足りないときは必要量と現在値を出す。 */
    private Component unlockButtonLabel(SkillData skill) {
        int required = skill == null ? 0 : skill.getUnlockProficiency();
        if (required <= 0) {
            return Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock");
        }
        SkillData.WeaponType category = skill.getProficiencyCategory();
        int current = ClientProficiencyHandler.getProficiency(category);
        if (current >= required) {
            return Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock");
        }
        return Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock_locked",
                Component.translatable(SwordSkill.MOD_ID + ".weapon." + category.name().toLowerCase()),
                required, current);
    }

    // ==================================================================
    // 描画
    // ==================================================================

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 選択中スキルが絞り込み条件から外れていたら合わせる
        if (!matchesFilter(SwordSkillRegistry.SKILLS.get(selectedSkillIndex))) {
            int first = firstMatchingSkillIndex();
            if (first >= 0) {
                selectedSkillIndex = first;
            }
        }

        Layout layout = computeLayout();
        long now = Util.getMillis();
        float fade = slideFade(now);
        float slide = slideOffset(now);

        drawPanel(guiGraphics, layout);
        ViewButton(layout);

        chainNodeHits.clear();
        hoveredSlot = hitTestSlot(layout, mouseX, mouseY);

        int primaryTextColor = fade(isDark() ? 0xFFF0F0F0 : 0xFF202020, fade);
        int secondaryTextColor = fade(isDark() ? 0xFFC0C0C0 : 0xFF555555, fade);
        int levelAccent = fade(isDark() ? 0xFF5ADFFF : 0xFFB46515, fade);

        Component screenTitle = Component.translatable("gui." + SwordSkill.MOD_ID + ".title");
        drawSmallCenteredString(guiGraphics, screenTitle, width / 2, layout.cardTop() + (layout.compact() ? 3 : 8),
                primaryTextColor);
        if (!layout.compact()) {
            guiGraphics.fill(layout.cardLeft() + 12, layout.cardTop() + 27, layout.cardLeft() + layout.cardWidth() - 12,
                    layout.cardTop() + 28, fade(isDark() ? 0x55777777 : 0x55707070, fade));
        }

        Component tooltip = drawWeaponTabs(guiGraphics, layout, mouseX, mouseY, fade);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(slide, 0.0F, 0.0F);

        SkillData skill = selectedSkillIndex >= 0 && selectedSkillIndex < SwordSkillRegistry.SKILLS.size()
                ? SwordSkillRegistry.SKILLS.get(selectedSkillIndex)
                : null;
        if (skill != null && !skill.isHide()) {
            Component hoveredWeapon = drawSkillDetail(guiGraphics, layout, skill, mouseX, mouseY,
                    primaryTextColor, secondaryTextColor, levelAccent, fade);
            if (hoveredWeapon != null) {
                tooltip = hoveredWeapon;
            }
        }

        guiGraphics.pose().popPose();

        drawFlash(guiGraphics, layout, now);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        drawSlotDiamonds(guiGraphics, layout, fade, isAssignable(skill) && layout.canAssign());

        // --- ツールチップ ---
        if (tooltip != null) {
            guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        } else if (hoveredSlot >= 0) {
            if (skill != null && SkillChains.isDerived(skill.getId())) {
                guiGraphics.renderTooltip(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".derived_not_assignable"), mouseX, mouseY);
            } else if (skill != null && !skill.isAssignable()) {
                guiGraphics.renderTooltip(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".not_assignable"), mouseX, mouseY);
            } else if (!isAssignable(skill)) {
                guiGraphics.renderTooltip(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_weapon_mismatch"), mouseX, mouseY);
            } else if (!layout.canAssign()) {
                guiGraphics.renderTooltip(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_locked"), mouseX, mouseY);
            } else {
                int[] currentSkillIds = ClientSkillSlotHandler.getSkillSlotInfo();
                int skillId = hoveredSlot < currentSkillIds.length ? currentSkillIds[hoveredSlot] : -1;
                SkillData assigned = SwordSkillRegistry.SKILLS.get(skillId);
                Component assignedName = assigned == null
                        ? Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_empty")
                        : Component.translatable(SwordSkill.MOD_ID + ".skill." + assigned.getName());
                guiGraphics.renderTooltip(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_assignment", hoveredSlot + 1,
                                assignedName),
                        mouseX, mouseY);
            }
        } else {
            for (ChainNodeHit hit : chainNodeHits) {
                if (mouseX >= hit.x() && mouseX < hit.x() + hit.size()
                        && mouseY >= hit.y() && mouseY < hit.y() + hit.size()) {
                    SkillData node = SwordSkillRegistry.SKILLS.get(hit.skillId());
                    if (node != null) {
                        boolean unlocked = unlockedSkills.contains(hit.skillId());
                        guiGraphics.renderTooltip(font, Component.translatable(SwordSkill.MOD_ID + ".skill."
                                + node.getName()).withStyle(unlocked ? net.minecraft.ChatFormatting.WHITE
                                        : net.minecraft.ChatFormatting.GRAY),
                                mouseX, mouseY);
                    }
                    break;
                }
            }
        }

        // セット不可の警告（一定時間表示）／通常はキー操作のヒント
        boolean warningActive = warningKey != null && (now - warningStartMs) < WARNING_DURATION_MS;
        if (warningActive) {
            drawSmallCenteredString(guiGraphics, Component.translatable(warningKey), width / 2, layout.footerTop(),
                    fade(0xFFFF5A5A, fade));
        } else {
            int proficiencyLeft = layout.cardLeft() + 14;
            Component hint = Component.literal("A  <     >  D");
            int hintWidth = Math.round(font.width(hint) * UI_TEXT_SCALE);
            // 幅に余裕があるときだけキー操作ヒントを出し、熟練度表示と重ならないようにする
            if (!layout.compact() && hintWidth + 110 < layout.cardWidth() - 28) {
                drawSmallString(guiGraphics, hint, proficiencyLeft, layout.footerTop(), secondaryTextColor);
                proficiencyLeft += hintWidth + 10;
            }
            drawProficiencyLine(guiGraphics, layout, skill, proficiencyLeft, secondaryTextColor, levelAccent, fade);
        }
    }

    /** フッター右側に、選択中スキルのカテゴリ熟練度とレベルアップポイントを表示する。 */
    private void drawProficiencyLine(GuiGraphics guiGraphics, Layout layout, SkillData skill, int leftLimit,
            int textColor, int accentColor, float fade) {
        if (skill == null) {
            return;
        }
        SkillData.WeaponType category = skill.getProficiencyCategory();
        if (category == SkillData.WeaponType.NONE) {
            return;
        }
        int value = ClientProficiencyHandler.getProficiency(category);
        int perLevel = ClientProficiencyHandler.getPointsPerLevel();
        int progress = ClientProficiencyHandler.getProgress(category);
        int points = ClientProficiencyHandler.getAvailablePoints(category);
        Component categoryName = Component.translatable(SwordSkill.MOD_ID + ".weapon." + category.name().toLowerCase());
        Component text = points > 0
                ? Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_line_points",
                        categoryName, value, progress, perLevel, points)
                : Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_line",
                        categoryName, value, progress, perLevel);

        int right = layout.cardLeft() + layout.cardWidth() - 14;
        if (Math.round(font.width(text) * UI_TEXT_SCALE) > right - leftLimit) {
            // 幅が足りないときは「カテゴリ 熟練度 値」だけの短い表記にする
            text = points > 0
                    ? Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_short_points",
                            categoryName, value, points)
                    : Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_short", categoryName, value);
        }
        int textWidth = Math.round(font.width(text) * UI_TEXT_SCALE);
        if (textWidth > right - leftLimit) {
            return;
        }
        drawSmallString(guiGraphics, text, right - textWidth, layout.footerTop(),
                points > 0 ? accentColor : textColor);
    }

    /** SAO 風の斜めカットパネル＋発光アクセント枠を描く。 */
    private void drawPanel(GuiGraphics guiGraphics, Layout layout) {
        guiGraphics.fill(0, 0, width, height, isDark() ? 0x88000000 : 0x66000000);

        int left = layout.cardLeft();
        int right = layout.cardLeft() + layout.cardWidth();
        int top = layout.cardTop();
        int bottom = layout.cardBottom();
        int cut = layout.cornerCut();

        // 外側の光（ダーク）／影（ライト）
        if (isDark()) {
            fillClipped(guiGraphics, left - 3, top - 3, right + 3, bottom + 3, 0x10FFFFFF, cut + 3);
            fillClipped(guiGraphics, left - 2, top - 2, right + 2, bottom + 2, 0x20FFFFFF, cut + 2);
            fillClipped(guiGraphics, left - 1, top - 1, right + 1, bottom + 1, 0x30FFFFFF, cut + 1);
        } else {
            fillClipped(guiGraphics, left - 2, top - 2, right + 2, bottom + 4, 0x14000000, cut + 2);
        }

        int accentTop = isDark() ? 0xFF8FF0FF : 0xFF9E9E9E;
        int accentBottom = isDark() ? 0xFF2A72E8 : 0xFF3E3E3E;
        fillClippedGradient(guiGraphics, left, top, right, bottom, accentTop, accentBottom, cut);

        fillClipped(guiGraphics, left + 2, top + 2, right - 2, bottom - 2,
                isDark() ? 0xE61A1C20 : 0xE6F4F4F4, Math.max(1, cut - 2));
    }

    /** 決定・タブ切替時のフラッシュ演出。 */
    private void drawFlash(GuiGraphics guiGraphics, Layout layout, long now) {
        if (flashStartMs == Long.MIN_VALUE) {
            return;
        }
        float progress = (now - flashStartMs) / (float) FLASH_DURATION_MS;
        if (progress < 0.0F || progress >= 1.0F) {
            return;
        }
        float strength = 1.0F - progress;
        int rgb = isDark() ? 0x5ADFFF : 0xC06A18;

        int overlayAlpha = (int) (0x30 * strength);
        fillClipped(guiGraphics, layout.cardLeft() + 2, layout.cardTop() + 2,
                layout.cardLeft() + layout.cardWidth() - 2, layout.cardBottom() - 2,
                (overlayAlpha << 24) | rgb, Math.max(1, layout.cornerCut() - 2));

        int edgeAlpha = (int) (0xCC * strength);
        guiGraphics.fill(layout.cardLeft() + layout.cornerCut(), layout.cardTop(),
                layout.cardLeft() + layout.cardWidth() - layout.cornerCut(), layout.cardTop() + 1,
                (edgeAlpha << 24) | rgb);
    }

    /** 武器種タブを描き、ホバー中のタブのツールチップを返す。 */
    private Component drawWeaponTabs(GuiGraphics guiGraphics, Layout layout, int mouseX, int mouseY, float fade) {
        Component hovered = null;
        int iconSize = layout.tabIconSize();
        int end = Math.min(TAB_TYPES.length, tabScroll + layout.tabVisibleCount());

        // タブが入りきらないときは領域外をクリップして見切れさせる
        guiGraphics.enableScissor(layout.tabStartX() - 3, layout.tabY() - 3,
                layout.tabStartX() + layout.tabUsedWidth() + 3, layout.tabY() + iconSize + 4);
        for (int index = tabScroll; index < end; index++) {
            SkillData.WeaponType type = TAB_TYPES[index];
            int x = tabX(layout, index);
            int y = layout.tabY();
            boolean active = activeFilter == type;
            boolean equipped = activeFilter == null && weaponType != null && weaponType.contains(type);
            boolean hover = mouseX >= x - 2 && mouseX < x + iconSize + 2
                    && mouseY >= y - 2 && mouseY < y + iconSize + 2;

            int background = active ? (isDark() ? 0x6648DFFF : 0x2EC06A18)
                    : equipped ? (isDark() ? 0x3338CFFF : 0x1AC06A18)
                            : (isDark() ? 0x22000000 : 0x22000000);
            guiGraphics.fill(x - 2, y - 2, x + iconSize + 2, y + iconSize + 2, fade(background, fade));
            // texWidth/texHeight に描画サイズを渡すことで、16x16 のアイコンを iconSize に拡大縮小する
            guiGraphics.blit(getWeaponIconTexture(type), x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
            if (active) {
                guiGraphics.fill(x - 2, y + iconSize + 2, x + iconSize + 2, y + iconSize + 3,
                        fade(isDark() ? 0xFF5ADFFF : 0xFFC06A18, fade));
            }
            if (hover) {
                hovered = Component.translatable(SwordSkill.MOD_ID + ".weapon." + type.name().toLowerCase())
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui." + SwordSkill.MOD_ID + ".weapon_tab_hint")
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        guiGraphics.disableScissor();

        // スクロール矢印（武器種が増えて入りきらない場合のみ表示）
        if (layout.tabScrollable()) {
            boolean canLeft = tabScroll > 0;
            boolean canRight = tabScroll + layout.tabVisibleCount() < TAB_TYPES.length;
            int arrowY = layout.tabY() + iconSize / 2 - 4;
            int leftColor = fade(canLeft ? (isDark() ? 0xFF7FEFFF : 0xFFC06A18) : 0x44FFFFFF, fade);
            int rightColor = fade(canRight ? (isDark() ? 0xFF7FEFFF : 0xFFC06A18) : 0x44FFFFFF, fade);
            guiGraphics.drawString(font, "<", tabArrowLeftX(layout), arrowY, leftColor, false);
            guiGraphics.drawString(font, ">", tabArrowRightX(layout), arrowY, rightColor, false);
        }
        return hovered;
    }

    private int tabX(Layout layout, int tabIndex) {
        return layout.tabStartX() + (tabIndex - tabScroll) * layout.tabStep();
    }

    /** タブ列の左端に表示する左スクロール矢印のX座標。 */
    private int tabArrowLeftX(Layout layout) {
        return layout.tabStartX() - 10;
    }

    /** タブ列の右端に表示する右スクロール矢印のX座標。 */
    private int tabArrowRightX(Layout layout) {
        return layout.tabStartX() + layout.tabUsedWidth() + 1;
    }

    private boolean isOverTabRow(Layout layout, double mouseX, double mouseY) {
        double x1 = layout.tabStartX() - 13;
        double x2 = layout.tabStartX() + layout.tabUsedWidth() + 13;
        double y1 = layout.tabY() - 3;
        double y2 = layout.tabY() + layout.tabIconSize() + 4;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    private boolean isOverPowerPanel(Layout layout, double mouseX, double mouseY) {
        double x1 = layout.powerPanelLeft() - 6;
        double x2 = layout.cardLeft() + layout.cardWidth() - 10;
        double y1 = layout.descriptionTop() - 6;
        double y2 = layout.footerTop();
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    private int hitTestTab(Layout layout, double mouseX, double mouseY) {
        int iconSize = layout.tabIconSize();
        int end = Math.min(TAB_TYPES.length, tabScroll + layout.tabVisibleCount());
        for (int index = tabScroll; index < end; index++) {
            int x = tabX(layout, index);
            int y = layout.tabY();
            if (mouseX >= x - 2 && mouseX < x + iconSize + 2 && mouseY >= y - 2 && mouseY < y + iconSize + 2) {
                return index;
            }
        }
        return -1;
    }

    /** タブのスクロール矢印のクリック判定。0=なし, -1=左, 1=右。 */
    private int hitTestTabArrow(Layout layout, double mouseX, double mouseY) {
        if (!layout.tabScrollable()) {
            return 0;
        }
        int y1 = layout.tabY() - 2;
        int y2 = layout.tabY() + layout.tabIconSize() + 2;
        int leftX = tabArrowLeftX(layout);
        if (mouseX >= leftX && mouseX < leftX + 9 && mouseY >= y1 && mouseY < y2 && tabScroll > 0) {
            return -1;
        }
        int rightX = tabArrowRightX(layout);
        int max = TAB_TYPES.length - layout.tabVisibleCount();
        if (mouseX >= rightX && mouseX < rightX + 9 && mouseY >= y1 && mouseY < y2 && tabScroll < max) {
            return 1;
        }
        return 0;
    }

    private int hitTestSlot(Layout layout, double mouseX, double mouseY) {
        int size = layout.diamondSize();
        int step = size + SLOT_DIAMOND_GAP;
        int total = SLOT_COUNT * step - SLOT_DIAMOND_GAP;
        int startX = width / 2 - total / 2;
        int top = layout.diamondTop();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = startX + slot * step;
            if (mouseX >= x && mouseX < x + size && mouseY >= top && mouseY < top + size) {
                return slot;
            }
        }
        return -1;
    }

    /** アイコン・名称・レベル・クールダウン・武器種・説明を描く。ホバー中の武器種名を返す。 */
    private Component drawSkillDetail(GuiGraphics guiGraphics, Layout layout, SkillData skill, int mouseX, int mouseY,
            int primaryTextColor, int secondaryTextColor, int levelAccent, float fade) {
        int centerX = width / 2;
        int iconCenterY = layout.iconCenterY();
        int selectedIconSize = layout.selectedIconSize();
        SkillData previousSkill = getAdjacentDisplayableSkill(-1);
        SkillData nextSkill = getAdjacentDisplayableSkill(1);

        if (previousSkill != null) {
            drawSkillIcon(guiGraphics, previousSkill, centerX - 66, iconCenterY - 12, 24);
        }
        if (nextSkill != null) {
            drawSkillIcon(guiGraphics, nextSkill, centerX + 42, iconCenterY - 12, 24);
        }

        int selectedIconX = centerX - selectedIconSize / 2;
        int selectedIconY = iconCenterY - selectedIconSize / 2;
        // タブ列と重ならないようにハローは小さめにする（コンパクト時はさらに小さく）
        int haloOuter = layout.compact() ? 4 : 5;
        int haloMid = layout.compact() ? 2 : 3;
        int haloFrame = layout.compact() ? 1 : 2;
        int selectionGlow = fade(isDark() ? 0x1838CFFF : 0x12C06A18, fade);
        int selectionAura = fade(isDark() ? 0x3848DFFF : 0x24C06A18, fade);
        int selectionFrame = fade(isDark() ? 0xA05ADFFF : 0xA0C06A18, fade);
        guiGraphics.fill(selectedIconX - haloOuter, selectedIconY - haloOuter,
                selectedIconX + selectedIconSize + haloOuter, selectedIconY + selectedIconSize + haloOuter,
                selectionGlow);
        guiGraphics.fill(selectedIconX - haloMid, selectedIconY - haloMid,
                selectedIconX + selectedIconSize + haloMid, selectedIconY + selectedIconSize + haloMid,
                selectionAura);
        guiGraphics.fill(selectedIconX - haloFrame, selectedIconY - haloFrame,
                selectedIconX + selectedIconSize + haloFrame, selectedIconY + selectedIconSize + haloFrame,
                selectionFrame);
        guiGraphics.fill(selectedIconX - 1, selectedIconY - 1,
                selectedIconX + selectedIconSize + 1, selectedIconY + selectedIconSize + 1,
                fade(isDark() ? 0xFF1A1C20 : 0xFFF4F4F4, fade));
        drawSkillIcon(guiGraphics, skill, selectedIconX, selectedIconY, selectedIconSize);

        Component skillName = Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName());
        drawSmallCenteredString(guiGraphics, skillName, centerX, layout.nameY(), primaryTextColor);

        // --- スキルレベルと現在の攻撃倍率 ---
        int skillLevel = ClientSkillLevelHandler.getLevel(skill.getId());
        int maxSkillLevel = ClientSkillLevelHandler.getMaxLevel();
        double skillLevelMultiplier = ClientSkillLevelHandler.getMultiplier(skill.getId());
        if (layout.compact()) {
            Component compactLevel = Component.translatable("gui." + SwordSkill.MOD_ID + ".skill_level_short",
                    skillLevel, maxSkillLevel, formatMultiplier(skillLevelMultiplier));
            int levelX = layout.cardLeft() + layout.cardWidth() - 14
                    - Math.round(font.width(compactLevel) * UI_TEXT_SCALE);
            drawSmallString(guiGraphics, compactLevel, levelX, layout.levelY(), levelAccent);
        } else {
            Component levelText = Component.translatable("gui." + SwordSkill.MOD_ID + ".skill_level",
                    skillLevel, maxSkillLevel, formatMultiplier(skillLevelMultiplier));
            drawSmallString(guiGraphics, levelText, layout.cardLeft() + 14, layout.levelY(), levelAccent);
            int barX = layout.cardLeft() + 14 + Math.round(font.width(levelText) * UI_TEXT_SCALE) + 8;
            int barWidth = layout.cardLeft() + layout.cardWidth() - 14 - barX;
            if (barWidth > 24) {
                drawLevelBar(guiGraphics, barX, layout.levelY() + 3, barWidth, skillLevel, maxSkillLevel, fade);
            }
        }

        drawSmallString(guiGraphics,
                Component.translatable("gui." + SwordSkill.MOD_ID + ".cooldown", skill.getCooldown()),
                layout.cardLeft() + 14, layout.cooldownY(), secondaryTextColor);
        drawSmallString(guiGraphics,
                Component.translatable("gui." + SwordSkill.MOD_ID + ".sp_cost", (int) skill.getSpCost()),
                centerX + 8, layout.cooldownY(), secondaryTextColor);

        Component hoveredWeapon = null;
        int iconX = layout.cardLeft() + 14;
        int weaponIconY = layout.weaponIconY();
        for (SkillData.WeaponType type : skill.getAvailableWeaponTypes()) {
            if (iconX + 16 > layout.cardLeft() + layout.cardWidth() - 12) {
                break;
            }
            guiGraphics.blit(getWeaponIconTexture(type), iconX, weaponIconY, 0, 0, 16, 16, 16, 16);
            if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= weaponIconY && mouseY < weaponIconY + 16) {
                hoveredWeapon = Component.translatable(SwordSkill.MOD_ID + ".weapon." + type.name().toLowerCase());
            }
            iconX += 20;
        }

        drawContent(guiGraphics, layout, skill, fade);
        drawPowerList(guiGraphics, layout, skill, levelAccent, fade);

        if (!layout.compact()) {
            guiGraphics.fill(layout.cardLeft() + 12, layout.descriptionTop() - 8,
                    layout.cardLeft() + layout.cardWidth() - 12, layout.descriptionTop() - 7,
                    fade(isDark() ? 0x55777777 : 0x55707070, fade));
        }

        return hoveredWeapon;
    }

    /**
     * 右側の攻撃倍率リスト。段（ヒット）ごとに「1段目 100%」形式で縦に並べる。
     * レベル倍率が1倍でないときは「1段目 100%→300%」のように適用後も併記する。
     */
    private void drawPowerList(GuiGraphics guiGraphics, Layout layout, SkillData skill, int levelAccent, float fade) {
        List<Component> lines = powerEntries(skill);
        if (lines.isEmpty()) {
            powerScroll = 0;
            return;
        }
        int rowStep = font.lineHeight + 2;
        int panelLeft = layout.powerPanelLeft();
        int panelRight = layout.cardLeft() + layout.cardWidth() - 14;
        int top = layout.descriptionTop();
        int maxY = layout.footerTop();
        int headerStep = rowStep + 3;

        int available = Math.max(0, (maxY - top - headerStep) / rowStep);
        if (available <= 0) {
            return;
        }
        int maxScroll = Math.max(0, lines.size() - available);
        powerScroll = Mth.clamp(powerScroll, 0, maxScroll);
        int shown = Math.min(lines.size() - powerScroll, available);

        int panelBottom = top + headerStep + shown * rowStep + 1;
        if (maxScroll > 0) {
            // スクロール可能なときはパネルを最大高さまで広げる
            panelBottom = Math.max(panelBottom, top + headerStep + available * rowStep + 1);
        }

        // パネル背景＋左のアクセントバー
        fillClipped(guiGraphics, panelLeft - 4, top - 4, panelRight + 4, panelBottom,
                fade(isDark() ? 0x33000000 : 0x22FFFFFF, fade), 4);
        guiGraphics.fill(panelLeft - 4, top - 4, panelLeft - 2, panelBottom,
                fade(isDark() ? 0xAA5ADFFF : 0x99C06A18, fade));

        drawSmallString(guiGraphics, Component.translatable("gui." + SwordSkill.MOD_ID + ".power_prefix"),
                panelLeft, top, levelAccent);

        int textColor = fade(isDark() ? 0xFFE8E8E8 : 0xFF303030, fade);
        for (int index = 0; index < shown; index++) {
            drawSmallString(guiGraphics, lines.get(powerScroll + index), panelLeft,
                    top + headerStep + index * rowStep, textColor);
        }

        // 段数が多いときのスクロールバー
        if (maxScroll > 0) {
            int trackTop = top + 1;
            int trackBottom = panelBottom - 2;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Math.max(4, trackHeight * available / lines.size());
            int thumbY = trackTop + (int) ((trackHeight - thumbHeight) * (powerScroll / (float) maxScroll));
            guiGraphics.fill(panelRight + 1, trackTop, panelRight + 3, trackBottom,
                    fade(isDark() ? 0x44FFFFFF : 0x33000000, fade));
            guiGraphics.fill(panelRight + 1, thumbY, panelRight + 3, thumbY + thumbHeight, levelAccent);
        }
    }

    // ==================================================================
    // 説明欄（ページ送り＋派生リンク図）
    // ==================================================================

    private interface ContentBlock {
    }

    private record TextBlock(FormattedCharSequence line, int color) implements ContentBlock {
    }

    /** 派生スキルのリンク図ブロック。visualRows はアイコン行数（1行=CHAIN_NODE_STEP px）。 */
    private record ChainBlock(List<Integer> skillIds, int visualRows, Component label) implements ContentBlock {
    }

    private void drawContent(GuiGraphics guiGraphics, Layout layout, SkillData skill, float fade) {
        int descriptionLeft = layout.cardLeft() + 14;
        int descriptionWidth = layout.descriptionWidth();
        int rowStep = font.lineHeight + 2;

        List<ContentBlock> blocks = new ArrayList<>();

        List<Integer> derived = derivedSkillIds();
        if (!derived.isEmpty()) {
            Component chainLabel = chainLabel(derived);
            blocks.add(new ChainBlock(derived,
                    chainVisualRows(derived.size(), descriptionWidth, chainLabelWidth(chainLabel)), chainLabel));
        }

        String descriptionKey = SwordSkill.MOD_ID + ".skill." + skill.getName() + ".description";
        String description = Component.translatable(descriptionKey).getString();
        int descriptionColor = isDark() ? 0xFFCCCCCC : 0xFF505050;
        for (String paragraph : description.split("\n")) {
            for (FormattedCharSequence line : font.split(Component.literal(paragraph), descriptionWidth)) {
                blocks.add(new TextBlock(line, descriptionColor));
            }
        }

        int availableRows = Math.max(0, (layout.footerTop() - layout.descriptionTop()) / rowStep);
        if (availableRows <= 0) {
            lastPageCount = 1;
            return;
        }

        List<List<ContentBlock>> pages = paginate(blocks, availableRows, rowStep);
        boolean paging = pages.size() > 1;
        int contentRows = paging ? Math.max(1, availableRows - 1) : availableRows;
        if (paging) {
            pages = paginate(blocks, contentRows, rowStep);
        }
        lastPageCount = pages.size();
        contentPage = Mth.clamp(contentPage, 0, Math.max(0, pages.size() - 1));

        int row = 0;
        for (ContentBlock block : pages.get(contentPage)) {
            int y = layout.descriptionTop() + row * rowStep;
            if (block instanceof TextBlock text) {
                drawSmallString(guiGraphics, text.line(), descriptionLeft, y, fade(text.color(), fade));
                row += 1;
            } else if (block instanceof ChainBlock chain) {
                drawDerivedChain(guiGraphics, chain.skillIds(), chain.label(), descriptionLeft, y, descriptionWidth,
                        fade);
                row += rows(chain, rowStep);
            }
        }

        if (paging) {
            Component indicator = Component.translatable("gui." + SwordSkill.MOD_ID + ".page_indicator",
                    contentPage + 1, pages.size());
            int x = descriptionLeft + descriptionWidth - Math.round(font.width(indicator) * UI_TEXT_SCALE);
            drawSmallString(guiGraphics, indicator, x,
                    layout.descriptionTop() + contentRows * rowStep, levelAccentColor(fade));
        }
    }

    /** ブロックが消費するテキスト行数。リンク図は実際の高さに合わせて行を確保する。 */
    private int rows(ContentBlock block, int rowStep) {
        if (block instanceof ChainBlock chain) {
            return Math.max(1, Mth.ceil(chain.visualRows() * CHAIN_NODE_STEP / (float) rowStep));
        }
        return 1;
    }

    private List<List<ContentBlock>> paginate(List<ContentBlock> blocks, int rowsPerPage, int rowStep) {
        List<List<ContentBlock>> pages = new ArrayList<>();
        List<ContentBlock> current = new ArrayList<>();
        int used = 0;
        for (ContentBlock block : blocks) {
            int blockRows = rows(block, rowStep);
            if (used > 0 && used + blockRows > rowsPerPage) {
                pages.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            current.add(block);
            used += blockRows;
        }
        if (!current.isEmpty()) {
            pages.add(current);
        }
        if (pages.isEmpty()) {
            pages.add(new ArrayList<>());
        }
        return pages;
    }

    private int chainLabelWidth(Component label) {
        return font.width(label) + 6;
    }

    /** リンク図のラベル。元スキルを表示中なら「派生」、派生スキルを表示中なら「派生元: 元スキル名」。 */
    private Component chainLabel(List<Integer> chain) {
        int start = chain.isEmpty() ? -1 : chain.get(0);
        if (start < 0 || start == selectedSkillIndex) {
            return Component.translatable("gui." + SwordSkill.MOD_ID + ".derived_chain");
        }
        SkillData root = SwordSkillRegistry.SKILLS.get(start);
        Component rootName = root == null ? Component.empty()
                : Component.translatable(SwordSkill.MOD_ID + ".skill." + root.getName());
        return Component.translatable("gui." + SwordSkill.MOD_ID + ".derived_from", rootName);
    }

    private int chainFirstCapacity(int descriptionWidth, int labelWidth) {
        return Math.max(1, (descriptionWidth - labelWidth) / CHAIN_NODE_STEP);
    }

    private int chainPerRow(int descriptionWidth) {
        return Math.max(1, descriptionWidth / CHAIN_NODE_STEP);
    }

    private int chainVisualRows(int nodeCount, int descriptionWidth, int labelWidth) {
        int first = chainFirstCapacity(descriptionWidth, labelWidth);
        if (nodeCount <= first) {
            return 1;
        }
        return 1 + Mth.ceil((nodeCount - first) / (float) chainPerRow(descriptionWidth));
    }

    /** 派生スキルをノード＋接続線のリンク図として描く。選択中のノードは枠を強調する。 */
    private void drawDerivedChain(GuiGraphics guiGraphics, List<Integer> skillIds, Component label, int x, int y,
            int descriptionWidth, float fade) {
        int labelWidth = chainLabelWidth(label);
        drawSmallString(guiGraphics, label, x, y + 4, fade(isDark() ? 0xFFE0E0E0 : 0xFF303030, fade));

        int firstCapacity = chainFirstCapacity(descriptionWidth, labelWidth);
        int perRow = chainPerRow(descriptionWidth);
        int index = 0;
        int previousNodeX = Integer.MIN_VALUE;
        int previousNodeY = Integer.MIN_VALUE;
        for (Integer skillId : skillIds) {
            if (skillId == null || skillId < 0 || skillId >= SwordSkillRegistry.SKILLS.size()) {
                continue;
            }
            int row;
            int column;
            if (index < firstCapacity) {
                row = 0;
                column = index;
            } else {
                row = 1 + (index - firstCapacity) / perRow;
                column = (index - firstCapacity) % perRow;
            }
            int nodeX = x + (row == 0 ? labelWidth : 0) + column * CHAIN_NODE_STEP;
            int nodeY = y + row * CHAIN_NODE_STEP;
            SkillData nodeSkill = SwordSkillRegistry.SKILLS.get(skillId);
            boolean unlocked = unlockedSkills.contains(skillId);
            boolean current = skillId == selectedSkillIndex;

            int frame = current ? fade(isDark() ? 0xFFFFF0A0 : 0xFF2A2A2A, fade)
                    : unlocked ? fade(isDark() ? 0xFF7FEFFF : 0xFFC06A18, fade)
                            : fade(isDark() ? 0x88999999 : 0x99888888, fade);
            int body = fade(isDark() ? 0xE6101114 : 0xE6FFFFFF, fade);
            if (current) {
                // 選択中のノードはひと回り外側にも枠を描いて現在位置を示す
                guiGraphics.fill(nodeX - 1, nodeY - 1, nodeX + CHAIN_NODE_SIZE + 1, nodeY + CHAIN_NODE_SIZE + 1,
                        fade(isDark() ? 0xFFFFF0A0 : 0xFFC06A18, fade));
            }
            guiGraphics.fill(nodeX, nodeY, nodeX + CHAIN_NODE_SIZE, nodeY + CHAIN_NODE_SIZE, frame);
            guiGraphics.fill(nodeX + 1, nodeY + 1, nodeX + CHAIN_NODE_SIZE - 1, nodeY + CHAIN_NODE_SIZE - 1, body);
            if (nodeSkill != null) {
                drawSkillIcon(guiGraphics, nodeSkill, nodeX + 2, nodeY + 2, CHAIN_NODE_SIZE - 4);
                if (!unlocked) {
                    guiGraphics.fill(nodeX + 1, nodeY + 1, nodeX + CHAIN_NODE_SIZE - 1, nodeY + CHAIN_NODE_SIZE - 1,
                            fade(isDark() ? 0x991A1C20 : 0x99F0F0F0, fade));
                }
            }

            // 直前のノードと接続線でつなぐ
            if (previousNodeX != Integer.MIN_VALUE && previousNodeY == nodeY) {
                int lineY = nodeY + CHAIN_NODE_SIZE / 2;
                guiGraphics.fill(previousNodeX + CHAIN_NODE_SIZE, lineY, nodeX, lineY + 1,
                        fade(isDark() ? 0x885ADFFF : 0x66C06A18, fade));
            }

            chainNodeHits.add(new ChainNodeHit(skillId, nodeX, nodeY, CHAIN_NODE_SIZE));
            previousNodeX = nodeX;
            previousNodeY = nodeY;
            index++;
        }
    }

    private record ChainNodeHit(int skillId, int x, int y, int size) {
    }

    // ==================================================================
    // スロット（ひし形パネル）
    // ==================================================================

    private void drawSlotDiamonds(GuiGraphics guiGraphics, Layout layout, float fade, boolean assignable) {
        int size = layout.diamondSize();
        int radius = size / 2;
        int step = size + SLOT_DIAMOND_GAP;
        int total = SLOT_COUNT * step - SLOT_DIAMOND_GAP;
        int startX = width / 2 - total / 2;
        int top = layout.diamondTop();

        int[] currentSkillIds = ClientSkillSlotHandler.getSkillSlotInfo();
        int activeSlot = ClientTickHandler.getSelectedSlot();

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int x = startX + slot * step;
            int centerX = x + radius;
            int centerY = top + radius;
            int skillId = slot < currentSkillIds.length ? currentSkillIds[slot] : -1;
            SkillData assignedSkill = SwordSkillRegistry.SKILLS.get(skillId);
            boolean hover = hoveredSlot == slot;
            boolean active = slot == activeSlot;

            int frame;
            if (!assignable) {
                // 装備武器種と合わないスキルを選んでいる間はセット不可であることを示す
                frame = fade(isDark() ? 0x66999999 : 0x99888888, fade);
            } else if (hover || active) {
                frame = fade(isDark() ? 0xFF7FEFFF : 0xFFC06A18, fade);
            } else {
                frame = fade(isDark() ? 0x885ADFFF : 0x88000000, fade);
            }
            fillDiamond(guiGraphics, centerX, centerY, radius, frame);
            fillDiamond(guiGraphics, centerX, centerY, radius - 1,
                    fade(isDark() ? 0xE6101114 : 0xE6FFFFFF, fade));
            if (assignedSkill != null) {
                // ひし形の内接正方形サイズ（= radius）で描くと角がはみ出さない
                drawSkillIcon(guiGraphics, assignedSkill, centerX - radius / 2, centerY - radius / 2, radius);
            }
            // スロット番号
            drawSmallString(guiGraphics, Component.literal(Integer.toString(slot + 1)), x - 8, centerY - 4,
                    fade(isDark() ? 0xFF999999 : 0xFF777777, fade));
        }
    }

    /** ひし形を1pxずつ塗る。 */
    private void fillDiamond(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        if (radius <= 0) {
            return;
        }
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            if (half <= 0) {
                continue;
            }
            guiGraphics.fill(centerX - half, centerY + dy, centerX + half + 1, centerY + dy + 1, color);
        }
    }

    // ==================================================================
    // パネル描画ヘルパ
    // ==================================================================

    private static int fade(int color, float factor) {
        if (factor >= 1.0F) {
            return color;
        }
        int alpha = (int) Math.max(0, Math.min(255, (color >>> 24) * factor));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    // ==================================================================
    // アニメーション
    // ==================================================================

    private float slideOffset(long now) {
        if (slideStartMs == Long.MIN_VALUE) {
            return 0.0F;
        }
        long elapsed = now - slideStartMs;
        if (elapsed >= SLIDE_DURATION_MS) {
            return 0.0F;
        }
        float t = Mth.clamp(elapsed / (float) SLIDE_DURATION_MS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        return -slideDirection * 26.0F * (1.0F - eased);
    }

    private float slideFade(long now) {
        if (slideStartMs == Long.MIN_VALUE) {
            return 1.0F;
        }
        long elapsed = now - slideStartMs;
        if (elapsed >= SLIDE_DURATION_MS) {
            return 1.0F;
        }
        float t = Mth.clamp(elapsed / (float) SLIDE_DURATION_MS, 0.0F, 1.0F);
        return 0.25F + 0.75F * t;
    }

    // ==================================================================
    // スキル内容
    // ==================================================================

    private void unlockSkill(int selectedSkill) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkill);
            if (skill == null || !skill.isAssignable()) {
                return;
            }
            // 熟練度による解放条件（クリエイティブは免除）
            if (!player.isCreative() && !proficiencyUnlockMet(skill)) {
                showWarning("gui." + SwordSkill.MOD_ID + ".unlock_proficiency_warning");
                return;
            }
            // 条件を満たしていれば解放をリクエスト（結果はサーバーから同期される）
            PacketDistributor.sendToServer(new RequestUnlockSkillPayload(selectedSkill));
            triggerFlash(1.0F);
        }
    }

    private void unlockDerivedSkill() {
        int index = nextDerivedUnlockIndex();
        if (index >= 0) {
            unlockSkill(index);
        }
    }

    private void saveSkillToSlot(int slotIndex) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        if (skill == null) {
            return;
        }
        // 説明用エントリはセットできない
        if (!skill.isAssignable()) {
            showWarning("gui." + SwordSkill.MOD_ID + ".not_assignable");
            return;
        }
        // 派生先は直接セットできない（元スキルからのコンボでのみ使用）
        if (SkillChains.isDerived(skill.getId())) {
            showWarning("gui." + SwordSkill.MOD_ID + ".derived_not_assignable");
            return;
        }
        // スロットは常時表示なので、未解放のスキルはここで弾く
        if (UnlockedSkill.get() && !unlockedSkills.contains(selectedSkillIndex)) {
            showWarning("gui." + SwordSkill.MOD_ID + ".slot_locked");
            return;
        }
        // 装備中の武器種で使えないスキルはセットできない（タブで別武器種を表示中でも同様）
        if (!isAssignable(skill)) {
            showWarning("gui." + SwordSkill.MOD_ID + ".slot_weapon_mismatch");
            return;
        }
        String playerWeaponType = ClientSkillSlotHandler.getCurrentWeaponName();
        if (playerWeaponType != null && !playerWeaponType.equals("None")) {
            PacketDistributor.sendToServer(new SkillSlotSelectionPayload(skill.getId(), slotIndex, playerWeaponType));
            triggerFlash(0.9F);
        }
    }

    /**
     * そのスキルが「今装備している武器種」で使用できるか。
     * 選択画面のタブで別の武器種を表示していても、装備中の武器種に対応しないスキルはセットできない。
     */
    private boolean isAssignable(SkillData skill) {
        if (skill == null || !skill.isSlotAssignable()) {
            return false;
        }
        Set<SkillData.WeaponType> equipped = (weaponType != null && !weaponType.isEmpty())
                ? weaponType
                : ClientSkillSlotHandler.getCurrentWeaponTypes();
        if (equipped == null || equipped.isEmpty()) {
            return false;
        }
        return skill.getAvailableWeaponTypes().stream().anyMatch(equipped::contains);
    }

    /** 熟練度ポイントでレベルアップできる選択中スキルか。 */
    private boolean canLevelUpSelected() {
        return canLevelUp(SwordSkillRegistry.SKILLS.get(selectedSkillIndex));
    }

    /**
     * 熟練度ポイントでレベルアップできるか。
     * 未解放・セット不可（説明枠）・最大レベル・ポイント不足のいずれかなら false。
     */
    private boolean canLevelUp(SkillData skill) {
        if (skill == null || !skill.isAssignable()) {
            return false;
        }
        if (UnlockedSkill.get() && !unlockedSkills.contains(skill.getId())) {
            return false;
        }
        if (ClientProficiencyHandler.getAvailablePoints(skill.getProficiencyCategory()) <= 0) {
            return false;
        }
        return ClientSkillLevelHandler.getLevel(skill.getId()) < ClientSkillLevelHandler.getMaxLevel();
    }

    /** 選択中スキルを熟練度ポイントで1レベル上げる。 */
    private void levelUpSelectedSkill() {
        SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        if (!canLevelUp(skill)) {
            showWarning("gui." + SwordSkill.MOD_ID + ".level_up_unavailable");
            return;
        }
        PacketDistributor.sendToServer(new RequestSkillLevelUpPayload(skill.getId()));
        triggerFlash(0.9F);
    }

    /** 解放に必要な熟練度を満たしているか。 */
    private boolean proficiencyUnlockMet(SkillData skill) {
        int required = skill == null ? 0 : skill.getUnlockProficiency();
        return required <= 0 || ClientProficiencyHandler.getProficiency(skill.getProficiencyCategory()) >= required;
    }

    /** TRANSFORM チェーンの終端（TRANSFORM_FINISH）のインデックス。 */
    private int transformFinishIndex() {
        int size = SwordSkillRegistry.SKILLS.size();
        for (int i = 0; selectedSkillIndex + i < size; i++) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i);
            if (skill == null || skill.getType() == SkillData.SkillType.TRANSFORM_FINISH) {
                return selectedSkillIndex + i;
            }
        }
        return selectedSkillIndex;
    }

    private boolean hasDerivedUnlock() {
        return nextDerivedUnlockIndex() >= 0;
    }

    /**
     * 選択中スキルの派生チェーン（TRANSFORM の次段〜TRANSFORM_FINISH）のうち、
     * まだ解放していない最初のスキルの ID。派生解放の対象が無ければ -1。
     * 元スキルが未解放のときは、先に元スキルを解放させるため -1 を返す。
     */
    private int nextDerivedUnlockIndex() {
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        if (selectedSkill == null || selectedSkill.getType() != SkillData.SkillType.TRANSFORM) {
            return -1;
        }
        // 解放制限が無効なら派生の解放作業は不要
        if (!UnlockedSkill.get()) {
            return -1;
        }
        if (!unlockedSkills.contains(selectedSkillIndex)) {
            return -1;
        }
        int finish = transformFinishIndex();
        int size = SwordSkillRegistry.SKILLS.size();
        for (int i = selectedSkillIndex + 1; i <= finish && i < size; i++) {
            if (!unlockedSkills.contains(i)) {
                return i;
            }
        }
        return -1;
    }

    /** 選択中スキルが属する派生チェーン全体（元スキル〜TRANSFORM_FINISH）のIDリスト。 */
    private List<Integer> derivedSkillIds() {
        return SkillChains.chainOf(selectedSkillIndex);
    }

    private void drawLevelBar(GuiGraphics guiGraphics, int x, int y, int width, int level, int maxLevel, float fade) {
        int segments = Math.max(1, Math.min(maxLevel, 20));
        int gap = 2;
        int segmentWidth = Math.max(1, (width - gap * (segments - 1)) / segments);
        int filledColor = fade(isDark() ? 0xFF5ADFFF : 0xFFB46515, fade);
        int emptyColor = fade(isDark() ? 0x44FFFFFF : 0x33000000, fade);
        for (int index = 0; index < segments; index++) {
            int left = x + index * (segmentWidth + gap);
            guiGraphics.fill(left, y, left + segmentWidth, y + 4, index < level ? filledColor : emptyColor);
        }
    }

    /**
     * 段（ヒット）ごとの攻撃倍率を発生順に並べたリスト。レベル倍率が1倍でないときは
     * 「1段目 100%→300%」のように適用後の値も併記する。
     */
    private List<Component> powerEntries(SkillData skill) {
        List<Component> lines = new ArrayList<>();
        DataSkillDefinition definition = skill.getDataDefinition();
        if (definition == null) {
            return lines;
        }
        List<PowerEntry> entries = new ArrayList<>();
        for (DataSkillDefinition.Hitbox hitbox : definition.hitboxes()) {
            if (hitbox.hitboxEnabled()) {
                entries.add(new PowerEntry(hitbox.tick(), hitbox.damageMultiplier()));
            }
        }
        for (DataSkillDefinition.ProjectileEvent projectile : definition.projectiles()) {
            entries.add(new PowerEntry(projectile.tick(), projectile.damageMultiplier()));
        }
        if (entries.isEmpty()) {
            return lines;
        }
        entries.sort(java.util.Comparator.comparingInt(PowerEntry::tick));

        double levelMultiplier = ClientSkillLevelHandler.getMultiplier(skill.getId());
        boolean leveled = Math.abs(levelMultiplier - 1.0D) > 0.001D;
        for (int index = 0; index < entries.size(); index++) {
            double base = entries.get(index).multiplier();
            if (leveled) {
                lines.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".power_hit_leveled",
                        index + 1, formatPercent(base), formatPercent(base * levelMultiplier)));
            } else {
                lines.add(Component.translatable("gui." + SwordSkill.MOD_ID + ".power_hit",
                        index + 1, formatPercent(base)));
            }
        }
        return lines;
    }

    private int levelAccentColor(float fade) {
        return fade(isDark() ? 0xFF5ADFFF : 0xFFB46515, fade);
    }

    private static String formatMultiplier(double multiplier) {
        return String.format(java.util.Locale.ROOT, "%.2f", multiplier);
    }

    private static String formatPercent(double multiplier) {
        double percent = multiplier * 100.0D;
        if (Math.abs(percent - Math.round(percent)) < 0.05D) {
            return Math.round(percent) + "%";
        }
        return String.format(java.util.Locale.ROOT, "%.1f%%", percent);
    }

    private record PowerEntry(int tick, double multiplier) {
    }

    // ==================================================================
    // テキスト描画ヘルパ
    // ==================================================================

    private void drawSmallString(GuiGraphics guiGraphics, Component text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(UI_TEXT_SCALE, UI_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawSmallString(GuiGraphics guiGraphics, FormattedCharSequence text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(UI_TEXT_SCALE, UI_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawSmallCenteredString(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int x = centerX - Math.round(font.width(text) * UI_TEXT_SCALE / 2.0F);
        drawSmallString(guiGraphics, text, x, y, color);
    }

    // ==================================================================
    // フィルタ・ナビゲーション
    // ==================================================================

    private boolean matchesFilter(SkillData skill) {
        if (skill == null || skill.isHide()) {
            return false;
        }
        Set<SkillData.WeaponType> types = activeFilter != null ? Set.of(activeFilter) : weaponType;
        if (types == null || types.isEmpty()) {
            return false;
        }
        return skill.getAvailableWeaponTypes().stream().anyMatch(types::contains);
    }

    private int firstMatchingSkillIndex() {
        for (int index = 0; index < SwordSkillRegistry.SKILLS.size(); index++) {
            if (matchesFilter(SwordSkillRegistry.SKILLS.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private SkillData getAdjacentDisplayableSkill(int direction) {
        int size = SwordSkillRegistry.SKILLS.size();
        if (size == 0) {
            return null;
        }
        int index = selectedSkillIndex;
        for (int i = 0; i < size; i++) {
            index = (index + direction + size) % size;
            SkillData candidate = SwordSkillRegistry.SKILLS.get(index);
            if (matchesFilter(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private int getPreviousDisplayableSkillIndex(int currentIndex) {
        int size = SwordSkillRegistry.SKILLS.size();
        if (size == 0) {
            return currentIndex;
        }
        int index = currentIndex;
        for (int i = 0; i < size; i++) {
            index = (index - 1 + size) % size;
            if (matchesFilter(SwordSkillRegistry.SKILLS.get(index))) {
                return index;
            }
        }
        return currentIndex;
    }

    private int getNextDisplayableSkillIndex(int currentIndex) {
        int size = SwordSkillRegistry.SKILLS.size();
        if (size == 0) {
            return currentIndex;
        }
        int index = currentIndex;
        for (int i = 0; i < size; i++) {
            index = (index + 1) % size;
            if (matchesFilter(SwordSkillRegistry.SKILLS.get(index))) {
                return index;
            }
        }
        return currentIndex;
    }

    private void drawSkillIcon(GuiGraphics guiGraphics, SkillData skill, int x, int y, int size) {
        if (skill.getIconTexture() == null) {
            return;
        }
        float scale = size / 32.0F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.blit(skill.getIconTexture(), 0, 0, 0, 0, 32, 32, 32, 32);
        guiGraphics.pose().popPose();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    private ResourceLocation getWeaponIconTexture(SkillData.WeaponType type) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID,
                "textures/gui/system_icon/" + type.name().toLowerCase() + "_icon.png");
    }
}
