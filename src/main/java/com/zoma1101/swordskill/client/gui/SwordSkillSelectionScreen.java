package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillLevelHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.payload.*;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static com.zoma1101.swordskill.item.SampleItemRegistry.UNLOCKITEM;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

@OnlyIn(Dist.CLIENT)
public class SwordSkillSelectionScreen extends Screen {
    private static final float UI_TEXT_SCALE = 0.9F;
    private static final ResourceLocation LIGHT_MODE_ICON = fromNamespaceAndPath(SwordSkill.MOD_ID,
            "textures/gui/system_icon/night.png");
    private static final ResourceLocation NIGHT_MODE_ICON = fromNamespaceAndPath(SwordSkill.MOD_ID,
            "textures/gui/system_icon/light.png");
    private static boolean blackTheme;

    public Set<Integer> unlockedSkills = new HashSet<>();
    public int selectedSkillIndex = 0;
    private Set<SkillData.WeaponType> weaponType;
    private final List<Button> hoverHighlightButtons = new ArrayList<>();
    private boolean hasDerivedUnlockButton;
    private Button themeButton;

    public SwordSkillSelectionScreen() {
        super(Component.translatable("gui." + SwordSkill.MOD_ID + ".title"));
        if (Minecraft.getInstance().player != null) {
            weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            PacketDistributor.sendToServer(new CheckSkillUnlockedPayload());
        }

        PacketDistributor.sendToServer(new SkillRequestPayload());
        PacketDistributor.sendToServer(new RequestSkillLevelsPayload());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            selectedSkillIndex = getPreviousDisplayableSkillIndex(selectedSkillIndex);
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            selectedSkillIndex = getNextDisplayableSkillIndex(selectedSkillIndex);
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers); // super.keyPressed() を先に実行

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
        return handled; // super.keyPressed() の結果を返す
    }

    private void ViewButton() {
        this.clearWidgets();
        hoverHighlightButtons.clear();
        hasDerivedUnlockButton = false;
        themeButton = null;

        int cardWidth = Math.min(340, Math.max(width - 28, 1));
        int cardLeft = (width - cardWidth) / 2;
        themeButton = Button.builder(Component.empty(), button -> blackTheme = !blackTheme)
                .bounds(cardLeft + cardWidth - 27, 10, 18, 18)
                .build();
        addRenderableWidget(themeButton);

        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);

        boolean hasDerivedButton = false;
        if (selectedSkill != null && selectedSkill.getType() == SkillData.SkillType.TRANSFORM || !UnlockedSkill.get()) {
            // TRANSFORM スキルの場合
            int index = 0;
            for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                    .equals(SkillData.SkillType.TRANSFORM_FINISH); i++) {
                index = selectedSkillIndex + i + 1;
            }

            if (!unlockedSkills.contains(index)) {
                Button unlockDerivedButton = Button
                        .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock_derived"),
                                button -> unlockDerivedSkill())
                        .bounds(width / 2 - Math.min(160, width - 24) / 2, height - 32,
                                Math.min(160, width - 24), 20)
                        .build();
                addButton(unlockDerivedButton);
                hasDerivedButton = true;
                hasDerivedUnlockButton = true;
            }
        }

        if (unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get()) {
            addSkillSlotButtons(hasDerivedButton);
        } else {
            Button unlockButton = Button
                    .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock"),
                            button -> unlockSkill(selectedSkillIndex))
                    .bounds(width / 2 - Math.min(160, width - 24) / 2, height - (hasDerivedButton ? 58 : 32),
                            Math.min(160, width - 24), 20)
                    .build();
            addButton(unlockButton);
        }
    }

    private void addSkillSlotButtons(boolean hasDerivedButton) {
        int[] currentSkillIds = ClientSkillSlotHandler.getSkillSlotInfo();
        int buttonWidth = Math.max(1, Math.min(316, width - 40));
        int buttonHeight = 18;
        int rowStep = 19;
        int slotAreaBottom = height - (hasDerivedButton ? 53 : 10);
        int firstButtonY = slotAreaBottom - rowStep * 4 - buttonHeight;
        int buttonX = width / 2 - buttonWidth / 2;

        for (int slot = 0; slot < 5; slot++) {
            int slotIndex = slot;
            int skillId = slot < currentSkillIds.length ? currentSkillIds[slot] : -1;
            SkillData assignedSkill = SwordSkillRegistry.SKILLS.get(skillId);
            Component assignedSkillName = assignedSkill == null
                    ? Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_empty")
                    : Component.translatable(SwordSkill.MOD_ID + ".skill." + assignedSkill.getName());
            Component label = Component.translatable("gui." + SwordSkill.MOD_ID + ".slot_assignment",
                    slot + 1, assignedSkillName);
            Button slotButton = Button.builder(label, button -> saveSkillToSlot(slotIndex))
                    .bounds(buttonX, firstButtonY + slot * rowStep, buttonWidth, buttonHeight)
                    .build();
            addButton(slotButton);
        }
    }

    private void addButton(Button button) {
        addRenderableWidget(button);
        hoverHighlightButtons.add(button);
    }

    private void unlockSkill(int selectedSkill) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (player.isCreative()) {
                PacketDistributor.sendToServer(new RequestUnlockSkillPayload(selectedSkill));
                return;
            }

            Inventory inventory = player.getInventory();
            NonNullList<ItemStack> allItems = NonNullList.create();
            allItems.addAll(inventory.items);
            allItems.addAll(inventory.armor);
            allItems.addAll(inventory.offhand);
            for (ItemStack itemStack : allItems) {
                if (itemStack.getItem() == UNLOCKITEM.get()) {
                    PacketDistributor.sendToServer(new RequestUnlockSkillPayload(selectedSkill));
                    return;
                }
            }
        }
    }

    private void unlockDerivedSkill() {
        int index = 0;
        for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                .equals(SkillData.SkillType.TRANSFORM_FINISH) && unlockedSkills.contains(selectedSkillIndex + i); i++) {
            index = selectedSkillIndex + i + 1;
        }
        unlockSkill(index);
    }

    private void saveSkillToSlot(int slotIndex) {
        if (minecraft != null && minecraft.player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
            if (skill != null) {
                String playerWeaponType = ClientSkillSlotHandler.getCurrentWeaponName();
                if (playerWeaponType != null && !playerWeaponType.equals("None")) { // 追加
                    PacketDistributor
                            .sendToServer(new SkillSlotSelectionPayload(skill.getId(), slotIndex, playerWeaponType));
                } // 追加
            }
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        boolean compact = height < 280;
        int cardWidth = Math.min(340, Math.max(width - 28, 1));
        int cardLeft = (width - cardWidth) / 2;
        int cardTop = 8;
        int cardBottom = Math.max(cardTop + 1, height - 8);

        guiGraphics.fill(0, 0, width, height, blackTheme ? 0x88000000 : 0x66000000);
        guiGraphics.fill(cardLeft - 3, cardTop - 3, cardLeft + cardWidth + 3, cardBottom + 3, 0x10FFFFFF);
        guiGraphics.fill(cardLeft - 2, cardTop - 2, cardLeft + cardWidth + 2, cardBottom + 2, 0x20FFFFFF);
        guiGraphics.fill(cardLeft - 1, cardTop - 1, cardLeft + cardWidth + 1, cardBottom + 1, 0x30FFFFFF);
        guiGraphics.fill(cardLeft, cardTop, cardLeft + cardWidth, cardBottom,
                blackTheme ? 0xE61A1C20 : 0xE6F4F4F4);

        Component screenTitle = Component.translatable("gui." + SwordSkill.MOD_ID + ".title");
        int primaryTextColor = blackTheme ? 0xFFF0F0F0 : 0xFF202020;
        int secondaryTextColor = blackTheme ? 0xFFC0C0C0 : 0xFF555555;
        drawSmallCenteredString(guiGraphics, screenTitle, width / 2, cardTop + (compact ? 3 : 8),
                primaryTextColor);
        if (!compact) {
            guiGraphics.fill(cardLeft + 12, cardTop + 27, cardLeft + cardWidth - 12, cardTop + 28,
                    blackTheme ? 0x55777777 : 0x55707070);
        }

        ViewButton();
        SkillData skill = selectedSkillIndex >= 0 && selectedSkillIndex < SwordSkillRegistry.SKILLS.size()
                ? SwordSkillRegistry.SKILLS.get(selectedSkillIndex)
                : null;
        Component hoveredWeapon = null;
        if (skill != null && !skill.isHide()) {
            int centerX = width / 2;
            int iconCenterY = cardTop + (compact ? 35 : 54);
            int selectedIconSize = compact ? 28 : 32;
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
            int selectionGlow = blackTheme ? 0x1838CFFF : 0x18FF8A00;
            int selectionAura = blackTheme ? 0x3848DFFF : 0x38FF8A00;
            int selectionFrame = blackTheme ? 0xA05ADFFF : 0xA0FF9A24;
            guiGraphics.fill(selectedIconX - 7, selectedIconY - 7,
                    selectedIconX + selectedIconSize + 7, selectedIconY + selectedIconSize + 7,
                    selectionGlow);
            guiGraphics.fill(selectedIconX - 5, selectedIconY - 5,
                    selectedIconX + selectedIconSize + 5, selectedIconY + selectedIconSize + 5,
                    selectionAura);
            guiGraphics.fill(selectedIconX - 3, selectedIconY - 3,
                    selectedIconX + selectedIconSize + 3, selectedIconY + selectedIconSize + 3,
                    selectionFrame);
            guiGraphics.fill(selectedIconX - 1, selectedIconY - 1,
                    selectedIconX + selectedIconSize + 1, selectedIconY + selectedIconSize + 1,
                    blackTheme ? 0xFF1A1C20 : 0xFFF4F4F4);
            drawSkillIcon(guiGraphics, skill, selectedIconX, selectedIconY, selectedIconSize);

            Component skillName = Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName());
            drawSmallCenteredString(guiGraphics, skillName, centerX,
                    cardTop + (compact ? 54 : 79), primaryTextColor);

            // === スキルレベルと現在の攻撃倍率 ===
            int skillLevel = ClientSkillLevelHandler.getLevel(skill.getId());
            int maxSkillLevel = ClientSkillLevelHandler.getMaxLevel();
            double skillLevelMultiplier = ClientSkillLevelHandler.getMultiplier(skill.getId());
            int levelAccent = blackTheme ? 0xFF5ADFFF : 0xFFD07A00;
            if (compact) {
                Component compactLevel = Component.translatable("gui." + SwordSkill.MOD_ID + ".skill_level_short",
                        skillLevel, maxSkillLevel, formatMultiplier(skillLevelMultiplier));
                int levelX = cardLeft + cardWidth - 14 - Math.round(font.width(compactLevel) * UI_TEXT_SCALE);
                drawSmallString(guiGraphics, compactLevel, levelX, cardTop + 67, levelAccent);
            } else {
                Component levelText = Component.translatable("gui." + SwordSkill.MOD_ID + ".skill_level",
                        skillLevel, maxSkillLevel, formatMultiplier(skillLevelMultiplier));
                drawSmallString(guiGraphics, levelText, cardLeft + 14, cardTop + 91, levelAccent);
                int barX = cardLeft + 14 + Math.round(font.width(levelText) * UI_TEXT_SCALE) + 8;
                int barWidth = cardLeft + cardWidth - 14 - barX;
                if (barWidth > 24) {
                    drawLevelBar(guiGraphics, barX, cardTop + 94, barWidth, skillLevel, maxSkillLevel);
                }
            }

            drawSmallString(guiGraphics,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".cooldown", skill.getCooldown()),
                    cardLeft + 14, cardTop + (compact ? 67 : 103), secondaryTextColor);
            drawSmallString(guiGraphics,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".sp_cost", (int) skill.getSpCost()),
                    centerX + 8, cardTop + (compact ? 67 : 103), secondaryTextColor);

            int iconX = cardLeft + 14;
            int weaponIconY = cardTop + (compact ? 82 : 116);
            for (SkillData.WeaponType type : skill.getAvailableWeaponTypes()) {
                if (iconX + 16 > cardLeft + cardWidth - 12) {
                    break;
                }
                guiGraphics.blit(getWeaponIconTexture(type), iconX, weaponIconY, 0, 0, 16, 16, 16, 16);
                if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= weaponIconY && mouseY < weaponIconY + 16) {
                    hoveredWeapon = Component.translatable(SwordSkill.MOD_ID + ".weapon."
                            + type.name().toLowerCase());
                }
                iconX += 20;
            }

            String descriptionKey = SwordSkill.MOD_ID + ".skill." + skill.getName() + ".description";
            String description = Component.translatable(descriptionKey).getString();
            int descriptionLeft = cardLeft + 14;
            int descriptionWidth = Math.max(1, cardWidth - 28);
            List<FormattedCharSequence> contentLines = new ArrayList<>();
            List<Integer> contentColors = new ArrayList<>();

            // === 攻撃倍率（JSONの値を参照し、スキルレベル適用後の値も併記） ===
            Component powerLine = buildPowerLine(skill, skillLevelMultiplier);
            if (powerLine != null) {
                for (FormattedCharSequence line : font.split(powerLine, descriptionWidth)) {
                    contentLines.add(line);
                    contentColors.add(levelAccent);
                }
            }

            for (String paragraph : description.split("\n")) {
                for (FormattedCharSequence line : font.split(Component.literal(paragraph), descriptionWidth)) {
                    contentLines.add(line);
                    contentColors.add(blackTheme ? 0xFFCCCCCC : 0xFF505050);
                }
            }

            HashSet<Integer> derivedSkillIds = getDerivedSkills();
            if (!derivedSkillIds.isEmpty()) {
                Component derivedTitle = Component.translatable("gui." + SwordSkill.MOD_ID + ".skill.derived")
                        .append(":");
                for (FormattedCharSequence line : font.split(derivedTitle, descriptionWidth)) {
                    contentLines.add(line);
                    contentColors.add(blackTheme ? 0xFFE0E0E0 : 0xFF303030);
                }
                for (Integer derivedSkillId : derivedSkillIds) {
                    if (derivedSkillId >= 0 && derivedSkillId < SwordSkillRegistry.SKILLS.size()) {
                        SkillData derivedSkill = SwordSkillRegistry.SKILLS.get(derivedSkillId);
                        Component derivedName = Component.literal("- ").append(Component.translatable(
                                SwordSkill.MOD_ID + ".skill." + derivedSkill.getName()));
                        int color = unlockedSkills.contains(derivedSkillId)
                                ? blackTheme ? 0xFFE0E0E0 : 0xFF383838
                                : blackTheme ? 0xFF999999 : 0xFF858585;
                        for (FormattedCharSequence line : font.split(derivedName, descriptionWidth)) {
                            contentLines.add(line);
                            contentColors.add(color);
                        }
                    }
                }
            }

            int descriptionTop = cardTop + (compact ? 103 : 139);
            guiGraphics.fill(cardLeft + 12, descriptionTop - 8, cardLeft + cardWidth - 12, descriptionTop - 7,
                    blackTheme ? 0x55777777 : 0x55707070);
            boolean showSlotButtons = unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get();
            int slotListTop = height - (hasDerivedUnlockButton ? 53 : 10) - (19 * 4 + 18);
            int footerTop = showSlotButtons ? slotListTop - 8 : compact ? height - 42 : height - 72;
            int visibleLines = Math.max(0, (footerTop - descriptionTop) / (font.lineHeight + 2));
            boolean truncated = contentLines.size() > visibleLines;
            int linesToDraw = truncated ? Math.max(0, visibleLines - 1) : visibleLines;
            for (int i = 0; i < Math.min(linesToDraw, contentLines.size()); i++) {
                drawSmallString(guiGraphics, contentLines.get(i), descriptionLeft,
                        descriptionTop + i * (font.lineHeight + 2), contentColors.get(i));
            }
            if (truncated && visibleLines > 0) {
                drawSmallString(guiGraphics, Component.literal("..."), descriptionLeft,
                        descriptionTop + (visibleLines - 1) * (font.lineHeight + 2),
                        blackTheme ? 0xFFBBBBBB : 0xFF888888);
            }

            if (!compact) {
                drawSmallCenteredString(guiGraphics, Component.literal("A  <     >  D"), width / 2, footerTop,
                        blackTheme ? 0xFFAAAAAA : 0xFF777777);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        for (Button button : hoverHighlightButtons) {
            if (button.isHoveredOrFocused()) {
                int hoverFill = blackTheme ? 0x5538CFFF : 0x55FF8A00;
                int hoverEdge = blackTheme ? 0xFF58DFFF : 0xFFFF9A24;
                guiGraphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(),
                        button.getY() + button.getHeight(), hoverFill);
                guiGraphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(),
                        button.getY() + 1, hoverEdge);
                guiGraphics.fill(button.getX(), button.getY() + button.getHeight() - 1,
                        button.getX() + button.getWidth(), button.getY() + button.getHeight(), hoverEdge);
            }
        }
        if (themeButton != null) {
            ResourceLocation icon = blackTheme ? NIGHT_MODE_ICON : LIGHT_MODE_ICON;
            guiGraphics.blit(icon, themeButton.getX() + 1, themeButton.getY() + 1, 0, 0, 16, 16, 16, 16);
            if (themeButton.isHoveredOrFocused()) {
                guiGraphics.renderTooltip(font, Component.translatable("gui." + SwordSkill.MOD_ID + ".theme_toggle"),
                        mouseX, mouseY);
            }
        }
        if (hoveredWeapon != null) {
            guiGraphics.renderTooltip(font, hoveredWeapon, mouseX, mouseY);
        }
    }

    /** レベルを10分割したセグメントバーを描く。 */
    private void drawLevelBar(GuiGraphics guiGraphics, int x, int y, int width, int level, int maxLevel) {
        int segments = Math.max(1, Math.min(maxLevel, 20));
        int gap = 2;
        int segmentWidth = Math.max(1, (width - gap * (segments - 1)) / segments);
        int filledColor = blackTheme ? 0xFF5ADFFF : 0xFFD07A00;
        int emptyColor = blackTheme ? 0x44FFFFFF : 0x33000000;
        for (int index = 0; index < segments; index++) {
            int left = x + index * (segmentWidth + gap);
            guiGraphics.fill(left, y, left + segmentWidth, y + 4, index < level ? filledColor : emptyColor);
        }
    }

    /**
     * ヒットボックス／飛び道具の倍率を発生順に並べた1行を組み立てる。
     * レベル倍率が1倍でないときは「基礎%→レベル適用後%」の形で併記する。
     */
    private Component buildPowerLine(SkillData skill, double levelMultiplier) {
        DataSkillDefinition definition = skill.getDataDefinition();
        if (definition == null) {
            return null;
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
            return null;
        }
        entries.sort(java.util.Comparator.comparingInt(PowerEntry::tick));
        boolean leveled = Math.abs(levelMultiplier - 1.0D) > 0.001D;
        net.minecraft.network.chat.MutableComponent result = Component
                .translatable("gui." + SwordSkill.MOD_ID + ".power_prefix");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                result.append(Component.literal("  "));
            }
            double baseMultiplier = entries.get(index).multiplier();
            if (leveled) {
                result.append(Component.translatable("gui." + SwordSkill.MOD_ID + ".power_hit_leveled",
                        index + 1, formatPercent(baseMultiplier),
                        formatPercent(baseMultiplier * levelMultiplier)));
            } else {
                result.append(Component.translatable("gui." + SwordSkill.MOD_ID + ".power_hit",
                        index + 1, formatPercent(baseMultiplier)));
            }
        }
        return result;
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

    private SkillData getAdjacentDisplayableSkill(int direction) {
        if (SwordSkillRegistry.SKILLS.isEmpty() || weaponType == null) {
            return null;
        }
        int index = selectedSkillIndex;
        for (int i = 0; i < SwordSkillRegistry.SKILLS.size(); i++) {
            index = (index + direction + SwordSkillRegistry.SKILLS.size()) % SwordSkillRegistry.SKILLS.size();
            SkillData candidate = SwordSkillRegistry.SKILLS.get(index);
            if (candidate != null && !candidate.isHide()
                    && candidate.getAvailableWeaponTypes().stream().anyMatch(weaponType::contains)) {
                return candidate;
            }
        }
        return null;
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

    private int getPreviousDisplayableSkillIndex(int currentIndex) {
        int previousIndex = currentIndex;
        do {
            previousIndex = (previousIndex - 1 + SwordSkillRegistry.SKILLS.size()) % SwordSkillRegistry.SKILLS.size();
        } while (SwordSkillRegistry.SKILLS.get(previousIndex).isHide() || SwordSkillRegistry.SKILLS.get(previousIndex)
                .getAvailableWeaponTypes().stream().noneMatch(weaponType::contains));
        return previousIndex;
    }

    private int getNextDisplayableSkillIndex(int currentIndex) {
        int nextIndex = currentIndex;
        do {
            nextIndex = (nextIndex + 1) % SwordSkillRegistry.SKILLS.size();
        } while (SwordSkillRegistry.SKILLS.get(nextIndex).isHide() || SwordSkillRegistry.SKILLS.get(nextIndex)
                .getAvailableWeaponTypes().stream().noneMatch(weaponType::contains));
        return nextIndex;
    }

    // 武器種ごとのアイコンテクスチャを取得するメソッド
    private ResourceLocation getWeaponIconTexture(SkillData.WeaponType type) {
        return fromNamespaceAndPath(SwordSkill.MOD_ID,
                "textures/gui/system_icon/" + type.name().toLowerCase() + "_icon.png");
    }

    private HashSet<Integer> getDerivedSkills() {
        HashSet<Integer> TransformSkillList = new HashSet<>();
        if (SwordSkillRegistry.SKILLS.get(selectedSkillIndex).getType().equals(SkillData.SkillType.TRANSFORM)) {
            for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                    .equals(SkillData.SkillType.TRANSFORM_FINISH); i++) {

                TransformSkillList.add(selectedSkillIndex + i + 1);
            }
        }
        return TransformSkillList;
    }

}