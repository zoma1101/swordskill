package com.zoma1101.swordskill.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * スキル選択画面の SAO 風パネルに合わせたボタン。
 *
 * <p>バニラのボタンテクスチャ（グレーの箱）ではなく、パネルと同じ「角を斜めにカットした枠＋
 * 上下グラデーションの本体」を自前で描く。テーマ（ライト／ダーク）に応じて配色が変わる。</p>
 *
 * <p>{@link #icon} で作ったボタンは、テクスチャをボタン全面に描くアイコンボタンになる
 * （テーマ切替ボタンなど）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class SaoButton extends Button {

    /** 角のカット量（px）。 */
    private static final int CORNER_CUT = 4;
    /** 枠の太さ（px）。 */
    private static final int FRAME = 2;
    /** アイコンボタン用テクスチャの既定実寸（theme_toggle_*.png は 22x22）。 */
    private static final int DEFAULT_ICON_TEXTURE_SIZE = 22;

    private final boolean dark;
    @Nullable
    private final ResourceLocation background;
    /** アイコンテクスチャの実寸。ボタンサイズとは独立して全面をサンプリングする。 */
    private final int iconTextureWidth;
    private final int iconTextureHeight;
    /** 文字が入りきらないときのツールチップを一度だけ設定するためのフラグ。 */
    private boolean clippedTooltipSet;

    private SaoButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean dark,
            @Nullable ResourceLocation background, int iconTextureWidth, int iconTextureHeight) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.dark = dark;
        this.background = background;
        this.iconTextureWidth = iconTextureWidth;
        this.iconTextureHeight = iconTextureHeight;
    }

    /** テキスト付きの通常ボタン。 */
    public static SaoButton of(Component message, Runnable action, int x, int y, int width, int height, boolean dark) {
        return new SaoButton(x, y, width, height, message, button -> action.run(), dark, null, 0, 0);
    }

    /** テクスチャを全面に描くアイコンボタン。テクスチャ実寸は PNG ヘッダから取得する。 */
    public static SaoButton icon(Component tooltip, ResourceLocation texture, Runnable action, int x, int y, int size,
            boolean dark) {
        int[] textureSize = iconTextureSize(texture);
        return icon(tooltip, texture, action, x, y, size, dark, textureSize[0], textureSize[1]);
    }

    /** テクスチャ実寸を明示するアイコンボタン。 */
    public static SaoButton icon(Component tooltip, ResourceLocation texture, Runnable action, int x, int y, int size,
            boolean dark, int textureWidth, int textureHeight) {
        SaoButton button = new SaoButton(x, y, size, size, Component.empty(), b -> action.run(), dark, texture,
                Math.max(1, textureWidth), Math.max(1, textureHeight));
        button.setTooltip(Tooltip.create(tooltip));
        return button;
    }

    /**
     * アイコンテクスチャの実寸を PNG ヘッダ（IHDR）から読む。
     *
     * <p>テーマアイコンは 22x22 / 32x32 の両方があり得るため、ボタンサイズとは別に
     * テクスチャ全体の実寸を渡す必要がある。読めない場合は既定サイズにフォールバックする。</p>
     */
    private static int[] iconTextureSize(ResourceLocation texture) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open()) {
                    byte[] header = in.readNBytes(24);
                    if (header.length >= 24
                            && (header[0] & 0xFF) == 0x89
                            && header[1] == 'P'
                            && header[2] == 'N'
                            && header[3] == 'G') {
                        int width = ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
                                | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
                        int height = ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
                                | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
                        if (width > 0 && height > 0) {
                            return new int[] {width, height};
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // リソースが読めない場合は既定サイズで描画する
        }
        return new int[] {DEFAULT_ICON_TEXTURE_SIZE, DEFAULT_ICON_TEXTURE_SIZE};
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (background != null) {
            // テクスチャ全体をボタンサイズへ拡縮する。uWidth/vWidth を実寸にしないと
            // 左上の一部だけが切り出されてアイコンが見切れる。
            guiGraphics.blit(background, getX(), getY(), getWidth(), getHeight(), 0, 0,
                    iconTextureWidth, iconTextureHeight, iconTextureWidth, iconTextureHeight);
            if (isHoveredOrFocused()) {
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        dark ? 0x2238CFFF : 0x22C06A18);
            }
            return;
        }

        int x1 = getX();
        int y1 = getY();
        int x2 = x1 + getWidth();
        int y2 = y1 + getHeight();
        int cut = Math.min(CORNER_CUT, Math.min(getWidth(), getHeight()) / 2);
        boolean hovered = isHoveredOrFocused();

        // 枠（アクセント色のグラデーション）
        SaoShapes.fillClippedGradient(guiGraphics, x1, y1, x2, y2, accentTop(), accentBottom(), cut);
        // 本体
        int body = this.active ? (hovered ? bodyHover() : bodyColor()) : bodyDisabled();
        SaoShapes.fillClipped(guiGraphics, x1 + FRAME, y1 + FRAME, x2 - FRAME, y2 - FRAME, body,
                Math.max(0, cut - FRAME));
        if (hovered && this.active) {
            // 左端のアクセントバーでホバーを強調する
            guiGraphics.fill(x1 + FRAME, y1 + cut, x1 + FRAME + 2, y2 - cut, hoverBarColor());
        }

        // ライトテーマでは影付き文字が読みにくいので、影なしで中央に描く
        Font font = Minecraft.getInstance().font;
        int textColor = !this.active ? textDisabled() : (dark ? 0xFFF0F0F0 : 0xFF16181C);
        int maxTextWidth = Math.max(8, getWidth() - (FRAME + 2) * 2);
        Component message = getMessage();
        boolean clipped = font.width(message) > maxTextWidth;
        String label = clipped ? font.plainSubstrByWidth(message.getString(), maxTextWidth) : message.getString();
        int labelWidth = font.width(label);
        guiGraphics.drawString(font, label, x1 + (getWidth() - labelWidth) / 2, y1 + (getHeight() - 8) / 2,
                textColor, false);
        if (clipped && !clippedTooltipSet) {
            setTooltip(Tooltip.create(message));
            clippedTooltipSet = true;
        }
    }

    private int hoverBarColor() {
        return dark ? accentTop() : 0xFFC06A18;
    }

    private int accentTop() {
        return dark ? 0xFF8FF0FF : 0xFFC8C8C8;
    }

    private int accentBottom() {
        return dark ? 0xFF2A72E8 : 0xFF6E6E6E;
    }

    private int bodyColor() {
        return dark ? 0xE61A1C20 : 0xF2F7F7F7;
    }

    private int bodyHover() {
        return dark ? 0xE62A3040 : 0xFFFFFFFF;
    }

    private int bodyDisabled() {
        return dark ? 0xE6131417 : 0xF2E4E4E4;
    }

    private int textDisabled() {
        return dark ? 0xFF6E747C : 0xFF8A9098;
    }
}
