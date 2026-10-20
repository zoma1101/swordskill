package com.zoma1101.swordskill.client.gui;

import com.zoma1101.swordskill.config.ClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * SAO 風 GUI の共通テーマ。
 *
 * <p>スキル選択画面・ステータス画面・熟練度一覧画面で共有するライト／ダークの配色。
 * 選択状態はクライアント設定（{@code guiDarkTheme}）に保存される。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SaoTheme {

    public static final ResourceLocation THEME_BUTTON_ON = ResourceLocation.fromNamespaceAndPath(
            com.zoma1101.swordskill.SwordSkill.MOD_ID, "textures/gui/system_icon/theme_toggle_on.png");
    public static final ResourceLocation THEME_BUTTON_OFF = ResourceLocation.fromNamespaceAndPath(
            com.zoma1101.swordskill.SwordSkill.MOD_ID, "textures/gui/system_icon/theme_toggle_off.png");

    private SaoTheme() {
    }

    /** ダークテーマかどうか。 */
    public static boolean dark() {
        try {
            return ClientConfig.guiDarkTheme.get();
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }

    public static void toggle() {
        setDark(!dark());
    }

    public static void setDark(boolean value) {
        try {
            ClientConfig.guiDarkTheme.set(value);
            ClientConfig.SPEC.save();
        } catch (IllegalStateException notLoadedYet) {
            // コンフィグ未ロード時は何もしない
        }
    }

    /** テーマ切替ボタン。{@code afterToggle} で画面を組み直す。 */
    public static SaoButton toggleButton(int x, int y, int size, Runnable afterToggle) {
        return SaoButton.icon(
                Component.translatable("gui." + com.zoma1101.swordskill.SwordSkill.MOD_ID + ".theme_toggle"),
                dark() ? THEME_BUTTON_ON : THEME_BUTTON_OFF,
                () -> {
                    toggle();
                    afterToggle.run();
                },
                x, y, size, dark());
    }

    // ==================================================================
    // 配色
    // ==================================================================

    public static int accent() {
        return dark() ? 0xFF5ADFFF : 0xFFC06A18;
    }

    public static int accentSoft() {
        return dark() ? 0xAA5ADFFF : 0x99C06A18;
    }

    /** 派生チェーンの接続線。 */
    public static int connector() {
        return dark() ? 0x805ADFFF : 0x66C06A18;
    }

    /** パネルの外周（ダークは発光、ライトは無彩色の枠）。 */
    public static int panelBorder() {
        return dark() ? 0xFF5ADFFF : 0xFF4A4A4A;
    }

    public static int panelBody() {
        return dark() ? 0xE61A1C20 : 0xE6F4F4F4;
    }

    public static int textPrimary() {
        return dark() ? 0xFFF0F0F0 : 0xFF202020;
    }

    public static int textSecondary() {
        return dark() ? 0xFFC0C0C0 : 0xFF555555;
    }

    public static int textDim() {
        return dark() ? 0xFFA0A8B0 : 0xFF6A6A6A;
    }

    public static int textLocked() {
        return dark() ? 0xFF8A8A8A : 0xFF9A9A9A;
    }

    public static int positive() {
        return dark() ? 0xFF5ADF8A : 0xFF2E8B57;
    }

    public static int separator() {
        return dark() ? 0x40FFFFFF : 0x33000000;
    }

    public static int highlight() {
        return dark() ? 0x40FFFFFF : 0x1F000000;
    }

    public static int selectionFill() {
        return dark() ? 0x4030A8E0 : 0x33C06A18;
    }

    public static int barBackground() {
        return dark() ? 0x60000000 : 0x22000000;
    }

    public static int scrollTrack() {
        return dark() ? 0x30000000 : 0x1F000000;
    }

    public static int scrollBar() {
        return dark() ? 0x80FFFFFF : 0x88000000;
    }

    public static int dimBackground() {
        return dark() ? 0x99000000 : 0x66000000;
    }

    /** 影なしの中央揃え文字（ライトテーマでも読みやすい）。 */
    public static void drawCentered(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y, int color) {
        guiGraphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
    }
}
