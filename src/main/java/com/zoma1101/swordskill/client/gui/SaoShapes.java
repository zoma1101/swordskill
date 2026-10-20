package com.zoma1101.swordskill.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 選択画面系 GUI で使う図形描画ヘルパ。角を斜めにカットした矩形（SAO 風パネル）を
 * {@link GuiGraphics#fill(int, int, int, int, int)} の重ね塗りだけで描く。
 */
@OnlyIn(Dist.CLIENT)
final class SaoShapes {

    private SaoShapes() {
    }

    /** 角を斜めにカットした矩形を塗る。 */
    static void fillClipped(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color, int cut) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        int c = Math.max(0, Math.min(cut, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        int topEnd = Math.min(y1 + c, y2);
        for (int y = y1; y < topEnd; y++) {
            int inset = c - (y - y1);
            if (x1 + inset < x2 - inset) {
                guiGraphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
            }
        }
        int bottomStart = Math.max(topEnd, y2 - c);
        if (topEnd < bottomStart) {
            guiGraphics.fill(x1, topEnd, x2, bottomStart, color);
        }
        for (int y = bottomStart; y < y2; y++) {
            int inset = c - (y2 - 1 - y);
            if (x1 + inset < x2 - inset) {
                guiGraphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
            }
        }
    }

    /** 角カット矩形を上下グラデーションで塗る。 */
    static void fillClippedGradient(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int colorTop,
            int colorBottom, int cut) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        int c = Math.max(0, Math.min(cut, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        float span = Math.max(1, y2 - y1 - 1);
        for (int y = y1; y < y2; y++) {
            int inset = 0;
            if (y - y1 < c) {
                inset = c - (y - y1);
            }
            if (y2 - 1 - y < c) {
                inset = Math.max(inset, c - (y2 - 1 - y));
            }
            if (x1 + inset >= x2 - inset) {
                continue;
            }
            int color = lerpColor(colorTop, colorBottom, (y - y1) / span);
            guiGraphics.fill(x1 + inset, y, x2 - inset, y + 1, color);
        }
    }

    private static int lerpColor(int from, int to, float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        int a = lerpChannel(from >>> 24, to >>> 24, clamped);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, clamped);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, clamped);
        int b = lerpChannel(from & 0xFF, to & 0xFF, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return (int) (from + (to - from) * t);
    }
}
