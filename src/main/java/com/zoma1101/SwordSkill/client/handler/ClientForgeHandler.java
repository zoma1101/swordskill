package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.HudPositionSettingScreen;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailManager;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailRenderer;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.network.*;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import com.zoma1101.swordskill.client.renderer.layer.SwordTrailLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.zoma1101.swordskill.client.handler.ClientTickHandler.getSelectedSlot;
import static com.zoma1101.swordskill.data.WeaponTypeUtils.*;
import static com.zoma1101.swordskill.swordskills.SkillData.SkillType.*;

@Mod.EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeHandler {

    // =========================================================================
    // 既存フィールド
    // =========================================================================

    private static final Map<Integer, Integer> cooldowns = new HashMap<>();
    private static int addSkillIndex = 0;
    private static Integer skillUsedTicks = null;
    private static Integer limitTickMax = 20;
    private static final Integer limitTickMin = 2;
    private static boolean SetWeaponType = false;

    // =========================================================================
    // 既存ハンドラー
    // =========================================================================

    public static void setSelectedSkillIndex(int index) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getPersistentData().putInt("selectedSkillIndex", index);
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkillSlotHandler.reset();
        cooldowns.clear();
        addSkillIndex = 0;
        skillUsedTicks = null;
        SetWeaponType = false;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            updateCooldowns();

            if (SetWeaponType && event.phase == TickEvent.Phase.END) {
                if (WeaponTypeDetector.isReady() || Minecraft.getInstance().player == null)
                    return;
                String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
                setWeaponType(player);
                NetworkHandler.sendToServer(new SkillLoadSlotPacket(weaponName));
                SetWeaponType = false;
            }

            while (Keybindings.INSTANCE.SwordSkill_Use_Key.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[getSelectedSlot()]);
            }
            while (Keybindings.INSTANCE.SwordSkill_Use_Key_0.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[0]);
            }
            while (Keybindings.INSTANCE.SwordSkill_Use_Key_1.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[1]);
            }
            while (Keybindings.INSTANCE.SwordSkill_Use_Key_2.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[2]);
            }
            while (Keybindings.INSTANCE.SwordSkill_Use_Key_3.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[3]);
            }
            while (Keybindings.INSTANCE.SwordSkill_Use_Key_4.consumeClick()) {
                UseSkill(ClientSkillSlotHandler.getSkillSlotInfo()[4]);
            }

            if (skillUsedTicks != null) {
                skillUsedTicks++;
                if (skillUsedTicks > limitTickMax) {
                    addSkillIndex = 0;
                    skillUsedTicks = null;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        // 描画のみ（一人称のキャプチャは onRenderHand で行う）
        SwordTrailRenderer.render(event);
    }

    // RenderHandEvent を使い、ItemInHandRenderer と同じ数式でビュー空間の剣先座標を計算して記録する。
    // これにより Player Animator のボーン依存を完全に排除し、一人称で確実にトレイルが動く。
    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson())
            return;

        player.getCapability(com.zoma1101.swordskill.capability.PlayerSkillsProvider.PLAYER_SKILLS)
                .ifPresent(skills -> {
                    if (!skills.isTrailEnabled()) {
                        SwordTrailManager.clear(player.getUUID());
                        return;
                    }

                    // 攻撃アニメーション中のみ記録
                    if (player.getAttackAnim(event.getPartialTick()) > 0) {
                        SwordTrailLayer.TrailSession session = SwordTrailManager.getSession(player.getUUID());

                        if (!session.active) {
                            session.filterOldPoints();
                            return;
                        }

                        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();

                        // 最初期のコードが持っていた「アニメーションデータから座標を出す」メソッドを呼び出す
                        // これにより、最も動作が安定していた頃のロジックが一人称で動きます
                        SwordTrailLayer.captureFirstPersonFromKeyframe(
                                session,
                                player,
                                player.getViewYRot(event.getPartialTick()));
                    } else {
                        // アニメーションが終わったら古いポイントを消去
                        SwordTrailManager.getSession(player.getUUID()).filterOldPoints();
                    }
                });
    }
    // =========================================================================
    // 既存ハンドラー（続き）
    // =========================================================================

    private static void updateCooldowns() {
        cooldowns.forEach((skillId, remainingTicks) -> {
            if (remainingTicks > 0)
                cooldowns.put(skillId, remainingTicks - 1);
        });
    }

    public static void setCooldowns(int skillID, int setCoolDown) {
        cooldowns.put(skillID, setCoolDown);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        NetworkHandler.INSTANCE.sendToServer(new CheckSkillUnlockedPacket());
        SetWeaponType = true;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
        if (weaponName != null && !weaponName.equals("None")) {
            if (Keybindings.INSTANCE.SwordSkill_Selector_Key.isDown()) {
                Minecraft.getInstance().setScreen(new SwordSkillSelectionScreen());
            } else if (Keybindings.INSTANCE.SwordSkill_HUD_Setting.isDown()) {
                Minecraft.getInstance().setScreen(new HudPositionSettingScreen());
            }
        }
    }

    public static float getCooldownRatio(int slotIndex) {
        int skillId = ClientSkillSlotHandler.getSkillSlotInfo()[slotIndex];
        if (cooldowns.containsKey(skillId)) {
            int remainingTicks = cooldowns.get(skillId);
            SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
            if (skill != null) {
                if (skill.getType() == TRANSFORM) {
                    for (int i = skillId + 1; i < SwordSkillRegistry.SKILLS.size(); i++) {
                        SkillData nextSkill = SwordSkillRegistry.SKILLS.get(i);
                        if (nextSkill.getType() == TRANSFORM_FINISH) {
                            return 1.0f - (float) remainingTicks / getCoolDown(nextSkill);
                        }
                    }
                }
                return 1.0f - (float) remainingTicks / getCoolDown(skill);
            }
        }
        return 1.0f;
    }

    private static void ExecuteSkill(int skillID, int coolDownSkillID) {
        SkillData skillData = SwordSkillRegistry.SKILLS.get(skillID);
        if (skillData != null) {
            Set<SkillData.WeaponType> weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
            if (weaponName != null && skillData.getAvailableWeaponTypes().stream()
                    .anyMatch(Objects.requireNonNull(weaponType)::contains)) {
                NetworkHandler.sendToServer(new UseSkillPacket(skillData.getId(), skillData.getFinalTick()));
                cooldowns.put(coolDownSkillID, getCoolDown(skillData));
                limitTickMax = skillData.getTransformLimitTick();
            }
        }
    }

    private static void UseSkill(int selectedSkillIndex) {
        if (selectedSkillIndex >= 0 && selectedSkillIndex < SwordSkillRegistry.SKILLS.size()) {
            int currentSkillIndex = selectedSkillIndex + addSkillIndex;
            if (!cooldowns.containsKey(selectedSkillIndex) || cooldowns.get(selectedSkillIndex) <= 0) {
                ExecuteSkill(selectedSkillIndex, selectedSkillIndex);
                skillUsedTicks = 0;
            } else if (skillUsedTicks != null) {
                if (skillUsedTicks < limitTickMax && skillUsedTicks > limitTickMin
                        && SwordSkillRegistry.SKILLS.get(currentSkillIndex).getType() == TRANSFORM) {
                    addSkillIndex++;
                    currentSkillIndex = selectedSkillIndex + addSkillIndex;
                    ExecuteSkill(currentSkillIndex, selectedSkillIndex);
                    skillUsedTicks = 0;
                } else if (skillUsedTicks < limitTickMax && skillUsedTicks > limitTickMin
                        && SwordSkillRegistry.SKILLS.get(currentSkillIndex).getType() == TRANSFORM_FINISH) {
                    ExecuteSkill(currentSkillIndex, selectedSkillIndex);
                    addSkillIndex = 0;
                    skillUsedTicks = null;
                }
            }
        }
    }

    static int getCoolDown(SkillData skillData) {
        LocalPlayer player = Minecraft.getInstance().player;
        double cooldown = player != null
                ? player.getAttributeBaseValue(SwordSkillAttribute.COOLDOWN_ATTRIBUTE.get())
                : 0;
        return (int) (skillData.getCooldown() * cooldown);
    }
}