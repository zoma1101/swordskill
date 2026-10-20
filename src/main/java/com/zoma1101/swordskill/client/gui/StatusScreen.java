package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientPlayerLevelHandler;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.payload.AllocateStatPayload;
import com.zoma1101.swordskill.payload.RequestPlayerLevelPayload;
import com.zoma1101.swordskill.playerlevel.StatType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * ステータス画面。プレイヤーレベル／XP と、STR・AGI へのステータスポイント割り振り、
 * および攻撃力・防御力・移動速度などの簡易的な戦闘ステータスを表示する。
 * ここから熟練度一覧画面（{@link ProficiencyOverviewScreen}）に移動できる。
 */
@OnlyIn(Dist.CLIENT)
public class StatusScreen extends Screen {

    private static final int PAD = 12;
    private static final int STAT_ROW = 40;
    private static final int SUMMARY_ROW = 14;

    private final SaoButton[] statButtons = new SaoButton[StatType.values().length];

    public StatusScreen() {
        super(Component.translatable("gui." + SwordSkill.MOD_ID + ".status_title"));
        PacketDistributor.sendToServer(new RequestPlayerLevelPayload());
    }

    @Override
    protected void init() {
        for (int index = 0; index < StatType.values().length; index++) {
            StatType type = StatType.values()[index];
            statButtons[index] = addRenderableWidget(SaoButton.of(
                    Component.literal("+"), () -> allocate(type),
                    plusButtonX(), statRowY(index) + 2, 26, 18, SaoTheme.dark()));
        }

        // テーマ切替（右上）
        addRenderableWidget(SaoTheme.toggleButton(panelRight() - 26, panelTop() + 4, 20, this::rebuildWidgets));

        int navWidth = 110;
        int gap = 8;
        int totalWidth = navWidth * 2 + gap;
        int navX = width / 2 - totalWidth / 2;
        int navY = panelBottom() - 26;

        addRenderableWidget(SaoButton.of(
                Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_open"),
                this::openProficiency,
                navX, navY, navWidth, 20, SaoTheme.dark()));
        addRenderableWidget(SaoButton.of(
                Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_close"),
                this::onClose,
                navX + navWidth + gap, navY, navWidth, 20, SaoTheme.dark()));
    }

    private void allocate(StatType type) {
        int amount = hasShiftDown() ? 10 : 1;
        PacketDistributor.sendToServer(new AllocateStatPayload(type.name(), amount));
    }

    private void openProficiency() {
        Minecraft.getInstance().setScreen(new ProficiencyOverviewScreen());
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

    private int leftX() {
        return panelLeft() + 14;
    }

    /** 割り振り欄と戦闘ステータス欄の境界。 */
    private int midX() {
        return panelLeft() + (panelRight() - panelLeft()) * 56 / 100;
    }

    private int contentTop() {
        return panelTop() + 70;
    }

    private int statRowY(int index) {
        return contentTop() + 6 + index * STAT_ROW;
    }

    private int plusButtonX() {
        return midX() - 8 - 26;
    }

    private int summaryX() {
        return midX() + 10;
    }

    private int summaryRowY(int index) {
        return contentTop() + 18 + index * SUMMARY_ROW;
    }

    // ==================================================================
    // 描画
    // ==================================================================

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fill(0, 0, width, height, SaoTheme.dimBackground());
        int left = panelLeft();
        int top = panelTop();
        int right = panelRight();
        int bottom = panelBottom();
        guiGraphics.fill(left, top, right, bottom, SaoTheme.panelBody());
        guiGraphics.fill(left, top, right, top + 1, SaoTheme.panelBorder());
        guiGraphics.fill(left, bottom - 1, right, bottom, SaoTheme.panelBorder());
        guiGraphics.fill(left, top, left + 1, bottom, SaoTheme.panelBorder());
        guiGraphics.fill(right - 1, top, right, bottom, SaoTheme.panelBorder());

        SaoTheme.drawCentered(guiGraphics, font, title, width / 2, top + 8, SaoTheme.textPrimary());

        boolean hasData = ClientPlayerLevelHandler.hasData();
        int available = ClientPlayerLevelHandler.getAvailablePoints();
        boolean canSpend = hasData && available > 0;
        for (SaoButton button : statButtons) {
            if (button != null) {
                button.active = canSpend;
            }
        }

        drawLevel(guiGraphics, hasData, available);
        drawStats(guiGraphics, hasData);
        drawCombatSummary(guiGraphics, hasData);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawLevel(GuiGraphics guiGraphics, boolean hasData, int available) {
        Component line;
        if (!hasData) {
            line = Component.translatable("gui." + SwordSkill.MOD_ID + ".status_loading");
        } else if (ClientPlayerLevelHandler.isMaxLevel()) {
            line = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_player_status_max",
                    ClientPlayerLevelHandler.getLevel(), ClientPlayerLevelHandler.getMaxLevel());
        } else {
            line = Component.translatable("gui." + SwordSkill.MOD_ID + ".proficiency_player_status",
                    ClientPlayerLevelHandler.getLevel(), ClientPlayerLevelHandler.getMaxLevel(),
                    ClientPlayerLevelHandler.getXp(), ClientPlayerLevelHandler.getXpToNext());
        }
        SaoTheme.drawCentered(guiGraphics, font, line, width / 2, panelTop() + 24, SaoTheme.accent());

        // XP バー
        int barWidth = Mth.clamp((panelRight() - panelLeft()) / 2, 140, 260);
        int barX = width / 2 - barWidth / 2;
        int barY = panelTop() + 38;
        guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 5, SaoTheme.barBackground());
        float progress = !hasData || ClientPlayerLevelHandler.isMaxLevel() ? 1.0F
                : Mth.clamp((float) ClientPlayerLevelHandler.getXp()
                        / (float) Math.max(1L, ClientPlayerLevelHandler.getXpToNext()), 0.0F, 1.0F);
        int filled = Math.round(barWidth * progress);
        if (hasData && filled > 0) {
            guiGraphics.fill(barX, barY, barX + filled, barY + 4, SaoTheme.accent());
        }

        Component points = Component.translatable("gui." + SwordSkill.MOD_ID + ".status_points",
                available, ClientPlayerLevelHandler.getSpentPoints(), ClientPlayerLevelHandler.getTotalPoints());
        SaoTheme.drawCentered(guiGraphics, font, points, width / 2, panelTop() + 52,
                available > 0 ? SaoTheme.positive() : SaoTheme.textDim());

        guiGraphics.fill(panelLeft() + 8, panelTop() + 66, panelRight() - 8, panelTop() + 67, SaoTheme.separator());
    }

    private void drawStats(GuiGraphics guiGraphics, boolean hasData) {
        int textLimit = plusButtonX() - 6;
        for (int index = 0; index < StatType.values().length; index++) {
            StatType type = StatType.values()[index];
            int rowY = statRowY(index);

            Component name = type.displayName();
            Component valueText = Component.literal(Integer.toString(hasData ? ClientPlayerLevelHandler.getStat(type) : 0));
            int valueX = leftX() + 46;
            guiGraphics.drawString(font, name, leftX(), rowY,
                    hasData ? SaoTheme.textPrimary() : SaoTheme.textDim(), false);
            guiGraphics.drawString(font, valueText, valueX, rowY, SaoTheme.accent(), false);

            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(statEffect(type, hasData).getString(), Math.max(8, textLimit - leftX())),
                    leftX(), rowY + 11, SaoTheme.textDim(), false);

            guiGraphics.fill(panelLeft() + 8, rowY + STAT_ROW - 10, midX() - 4, rowY + STAT_ROW - 9, SaoTheme.separator());
        }

        // 中央の区切り線
        guiGraphics.fill(midX(), contentTop(), midX() + 1, panelBottom() - 32, SaoTheme.separator());
    }

    /** クライアント側で計算した、現在のステータス効果。 */
    private Component statEffect(StatType type, boolean hasData) {
        int value = hasData ? ClientPlayerLevelHandler.getStat(type) : 0;
        return switch (type) {
            case STR -> {
                double bonus = Math.min(value * ServerConfig.strDamagePerPoint.get(),
                        ServerConfig.strMaxBonus.get());
                yield Component.translatable("gui." + SwordSkill.MOD_ID + ".status_str_effect",
                        formatPercent(bonus));
            }
            case AGI -> {
                double speed = Math.min(value * ServerConfig.agilityMoveSpeedPerPoint.get(),
                        ServerConfig.agilityMaxMoveSpeedBonus.get());
                yield Component.translatable("gui." + SwordSkill.MOD_ID + ".status_agi_effect",
                        formatPercent(speed));
            }
        };
    }

    // ==================================================================
    // 簡易的な戦闘ステータス
    // ==================================================================

    private void drawCombatSummary(GuiGraphics guiGraphics, boolean hasData) {
        guiGraphics.drawString(font,
                Component.translatable("gui." + SwordSkill.MOD_ID + ".status_combat_header"),
                summaryX(), contentTop() + 2, SaoTheme.textDim(), false);

        Player player = Minecraft.getInstance().player;
        int valueRight = panelRight() - 14;
        drawSummaryRow(guiGraphics, 0, "status_attack", player == null ? "-" : attackPower(player), valueRight);
        drawSummaryRow(guiGraphics, 1, "status_defense", player == null ? "-" : Integer.toString(player.getArmorValue()), valueRight);
        drawSummaryRow(guiGraphics, 2, "status_movespeed", player == null ? "-" : moveSpeed(player), valueRight);
        drawSummaryRow(guiGraphics, 3, "status_maxhp", player == null ? "-" : formatNumber(player.getMaxHealth()), valueRight);
        drawSummaryRow(guiGraphics, 4, "status_maxsp",
                player == null ? "-" : formatNumber(player.getAttributeValue(SwordSkillAttribute.MAX_SP)), valueRight);

        if (hasData && ClientPlayerLevelHandler.getSpBonus() > 0) {
            // レベル分の SP も表示に含まれている旨の補足
            guiGraphics.drawString(font,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".status_maxsp_note",
                            ClientPlayerLevelHandler.getSpBonus()),
                    summaryX(), summaryRowY(4) + 12, SaoTheme.textDim(), false);
        }
    }

    private void drawSummaryRow(GuiGraphics guiGraphics, int index, String labelKey, String value, int valueRight) {
        int rowY = summaryRowY(index);
        Component label = Component.translatable("gui." + SwordSkill.MOD_ID + "." + labelKey);
        guiGraphics.drawString(font, label, summaryX(), rowY, SaoTheme.textPrimary(), false);
        Component valueText = Component.literal(value);
        int valueX = Math.max(summaryX() + 40, valueRight - font.width(valueText));
        guiGraphics.drawString(font, valueText, valueX, rowY, SaoTheme.accent(), false);
    }

    /** ソードスキルの実効攻撃力（武器攻撃力 × STR 補正）。 */
    private String attackPower(Player player) {
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double str = 1.0D + Math.min(
                ClientPlayerLevelHandler.getStat(StatType.STR) * ServerConfig.strDamagePerPoint.get(),
                ServerConfig.strMaxBonus.get());
        return String.format(java.util.Locale.ROOT, "%.1f", base * str);
    }

    /** 移動速度を素の歩行速度（0.1）に対する倍率で表す。 */
    private String moveSpeed(Player player) {
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return String.format(java.util.Locale.ROOT, "\u00d7%.2f", speed / 0.1D);
    }

    private static String formatNumber(double value) {
        return String.format(java.util.Locale.ROOT, "%.0f", value);
    }

    private static String formatPercent(double ratio) {
        return String.format(java.util.Locale.ROOT, "%.1f", ratio * 100.0D);
    }
}
