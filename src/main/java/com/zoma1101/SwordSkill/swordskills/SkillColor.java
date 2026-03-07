package com.zoma1101.swordskill.swordskills;

public class SkillColor {
    // --- プレミアムカラーパレット (発光に映える鮮やかな色) ---
    public static final int ELECTRIC_BLUE = fromRGB(0, 204, 255); // 鮮やかな電撃水色
    public static final int NEON_RED = 0xFFFF1744; // 燃えるような赤
    public static final int LIME_GLOW = 0xFF76FF03; // 毒々しい黄緑
    public static final int SOLAR_ORANGE = 0xFFFFAB00; // 太陽のようなオレンジ
    public static final int ETHEREAL_PURPLE = 0xFFD500F9; // 幻想的な紫
    public static final int DEEP_SEA = 0xFF1DE9B6; // 深海のようなエメラルド
    public static final int PURE_WHITE = 0xFFFFFFFF; // 純白（最も眩しい）
    public static final int SHADOW_DARK = 0xFF333333; // 暗い影の色

    // --- 便利メソッド: Hex文字列から色を生成 ("#FF00FF" など) ---
    public static int fromHex(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        // ARGB形式にするため、Alphaがなければ FF を補完
        if (hex.length() == 6) {
            hex = "FF" + hex;
        }
        return (int) Long.parseLong(hex, 16);
    }

    // --- 便利メソッド: RGBから生成 ---
    public static int fromRGB(int r, int g, int b) {
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }
}
