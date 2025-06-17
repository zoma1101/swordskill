package com.zoma1101.swordskill.client.gui; // または client.event など適切な場所へ

import com.mojang.blaze3d.systems.RenderSystem;
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.handler.ClientTickHandler;
import com.zoma1101.swordskill.config.ClientConfig;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


// MODイベントバスに登録し、クライアントサイドでのみ動作
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Hud_Registry {

    private static final Logger LOGGER = LogManager.getLogger();

    // HUDレイヤー用のユニークなID
    public static final ResourceLocation SKILL_SLOT_DISPLAY_LAYER = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "skill_slot_display");

    private static final int SLOT_COUNT = 5;
    // private static final int SLOT_SIZE = 0; // 元コードで未使用 or hudSize計算に含まれる？ ClientConfig.hudScale がサイズと仮定
    private static final int SLOT_SPACING = 4;

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {

        event.registerAbove(VanillaGuiLayers.HOTBAR, SKILL_SLOT_DISPLAY_LAYER, (guiGraphics, partialTick) -> {
            // --- ここからがHUD描画ロジック ---
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return; // プレイヤー不在またはHUD非表示なら描画しない
            }

            // RenderSystem設定
            RenderSystem.enableBlend(); // アルファブレンディングを有効化
            RenderSystem.defaultBlendFunc(); // デフォルトのブレンド関数

            String weaponType = WeaponTypeDetector.getWeaponName(mc.player.getMainHandItem());

            // 有効な武器を持っている場合のみ描画
            if (weaponType != null && !weaponType.equals("None")) {
                int[] skillIds = ClientSkillSlotHandler.getSkillSlotInfo(); // スキルID取得
                int selectedSlot = ClientTickHandler.getSelectedSlot();    // 選択中スロット取得
                int hudPosXConfig = ClientConfig.hudPosX.get();                     // HUD X座標オフセット (Configから)
                int hudY = ClientConfig.hudPosY.get() + 10;                     // HUD Y座標 (Configから)
                int hudSize = ClientConfig.hudScale.get();                 // スロットサイズ (Configから, 元のSLOT_SIZE=0は無視)

                int hudX = getHudX(hudSize, mc, hudPosXConfig);


                // スロット数と取得したスキルID配列の長さが異なる場合のエラーハンドリング
                if (skillIds == null || skillIds.length < SLOT_COUNT) {
                    // LOGGER.warn("Skill ID array is null or too short (expected {}, got {})", SLOT_COUNT, skillIds == null ? "null" : skillIds.length);
                    skillIds = new int[SLOT_COUNT];
                    java.util.Arrays.fill(skillIds, -1); // -1で埋める
                    // return; // または描画中断
                }


                // 各スロットを描画
                for (int i = 0; i < SLOT_COUNT; i++) {
                    // スキルIDが有効かチェック (-1 は空スロットと仮定)
                    if (skillIds[i] != -1) {
                        SkillData skill = SwordSkillRegistry.SKILLS.get(skillIds[i]); // スキルデータ取得

                        if (skill != null) {
                            ResourceLocation icon = skill.getIconTexture(); // アイコンテクスチャ取得

                            // スロットの描画位置計算
                            // hudX をスキルバー全体の左上座標とする
                            int x = hudX + i * (hudSize + SLOT_SPACING);

                            // 選択中のスロットをハイライト
                            if (i == selectedSlot) {
                                // 半透明の白で枠を描画
                                guiGraphics.fill(x - 2, hudY - 2, x + hudSize + 2, hudY + hudSize + 2, 0x80FFFFFF);
                            }

                            // スキルアイコンを描画
                            RenderSystem.setShaderTexture(0, icon); // テクスチャを設定

                            guiGraphics.blit(
                                    icon,
                                    x, hudY,
                                    0, 0,
                                    hudSize,
                                    hudSize,
                                    hudSize,
                                    hudSize
                            );

                            // クールダウン表示
                            float cooldownRatio = ClientForgeHandler.getCooldownRatio(i); // クールダウン率取得
                            if (cooldownRatio < 1.0f) {
                                // クールダウン量に基づいてオーバーレイの高さを計算 (下から満ちる)
                                int cooldownHeight = (int) (hudSize * (1.0f - cooldownRatio));
                                // 半透明の黒でクールダウンオーバーレイを描画
                                guiGraphics.fill(x, hudY + hudSize - cooldownHeight, x + hudSize, hudY + hudSize, 0x80000000);
                            }
                        }
                    }
                }
            }

            RenderSystem.disableBlend();
        });
        LOGGER.info("Registered Skill Slot Display GUI Layer");
    }

    private static int getHudX(int hudSize, Minecraft mc, int hudPosXConfig) {
        int totalSkillBarWidth = (hudSize * SLOT_COUNT) + (SLOT_SPACING * (SLOT_COUNT - 1));
        if (SLOT_COUNT <= 0) { // スロット数が0以下の場合のエッジケース
            totalSkillBarWidth = 0;
        } else if (SLOT_COUNT == 1) { // スロット数が1の場合
            totalSkillBarWidth = hudSize;
        }


        // HUDのX座標を画面中心に設定し、Configからのオフセットを適用
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        return (screenWidth - totalSkillBarWidth) / 2 + hudPosXConfig;
    }
}