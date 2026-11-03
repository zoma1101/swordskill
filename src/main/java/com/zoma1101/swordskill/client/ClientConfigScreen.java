package com.zoma1101.swordskill.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Mods 一覧の「Config」ボタンから開ける設定画面を登録する。
 *
 * <p>TOML を直接開かなくても、ゲーム内で
 * {@code trailRenderMode} / {@code trailGlow} / {@code trailOpacity} などを
 * 変更できるようにするためのもの。</p>
 */
public final class ClientConfigScreen {
    private ClientConfigScreen() {
    }

    public static void register(ModContainer container) {
        // Supplier 版と取り違えないよう、明示的に IConfigScreenFactory へキャストする
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) ConfigurationScreen::new);
    }
}
