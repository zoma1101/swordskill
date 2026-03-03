package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.gui.HudPositionSettingScreen;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.payload.RequestUnlockSkillPayload;
import com.zoma1101.swordskill.payload.SkillLoadSlotPayload;
import com.zoma1101.swordskill.payload.SkillRequestPayload;
import com.zoma1101.swordskill.payload.UseSkillPayload;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.zoma1101.swordskill.IsAnimation.PlayerAnimation;
import static com.zoma1101.swordskill.client.handler.ClientTickHandler.getSelectedSlot;
import static com.zoma1101.swordskill.data.WeaponTypeUtils.*;
import static com.zoma1101.swordskill.swordskills.SkillData.SkillType.*;

@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientForgeHandler {

    private static final Map<Integer, Integer> cooldowns = new HashMap<>();
    private static int addSkillIndex = 0;
    private static Integer skillUsedTicks = null;
    private static Integer limitTickMax = 20;
    private static final Integer limitTickMin = 0; // 0に短縮して反応を改善
    private static boolean SetWeaponType = false;

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkillSlotHandler.reset();
        ClientUnlockedSkillsHandler.reset(); // アンロック情報もリセット
        cooldowns.clear();
        addSkillIndex = 0;
        skillUsedTicks = null;
        SetWeaponType = false;
    }

    public static void setSelectedSkillIndex(int index) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getPersistentData().putInt("selectedSkillIndex", index);
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        updateCooldowns();

        // 武器タイプの初期同期
        if (SetWeaponType) {
            if (WeaponTypeDetector.isReady()) {
                String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
                setWeaponType(player);
                PacketDistributor.sendToServer(new SkillLoadSlotPayload(weaponName));
                SetWeaponType = false;
            }
        }

        // --- スキル使用キーの判定 ---
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

        // コンボ受付時間のカウント
        if (skillUsedTicks != null) {
            skillUsedTicks++;
            if (skillUsedTicks > limitTickMax) {
                addSkillIndex = 0;
                skillUsedTicks = null;
            }
        }
    }

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
        PacketDistributor.sendToServer(new SkillRequestPayload());
        PacketDistributor.sendToServer(new RequestUnlockSkillPayload(0)); // アンロック情報の要求
        SetWeaponType = true;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleSystemKeyInteraction(event.getKey(), false);
        }
    }

    @SubscribeEvent
    public static void onMouseButtonInput(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleSystemKeyInteraction(event.getButton(), true);
        }
    }

    /**
     * GUIを開くなどのシステムキー操作のみハンドル（スキル使用は clientTick で consumeClick する）
     */
    private static void handleSystemKeyInteraction(int code, boolean isMouse) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null)
            return;

        InputConstants.Key inputKey = isMouse ? InputConstants.Type.MOUSE.getOrCreate(code)
                : InputConstants.Type.KEYSYM.getOrCreate(code);

        if (Keybindings.INSTANCE.SwordSkill_Selector_Key.isActiveAndMatches(inputKey)) {
            mc.setScreen(new SwordSkillSelectionScreen());
        } else if (Keybindings.INSTANCE.SwordSkill_HUD_Setting.isActiveAndMatches(inputKey)) {
            mc.setScreen(new HudPositionSettingScreen());
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

                // サーバーへ送信
                PacketDistributor.sendToServer(new UseSkillPayload(skillData.getId(), skillData.getFinalTick()));

                // クールダウン設定（スロットに対して設定）
                cooldowns.put(coolDownSkillID, getCoolDown(skillData));

                // 受付時間更新
                limitTickMax = skillData.getTransformLimitTick();

                // アニメーション再生
                if (skillData.getType().equals(RUSH)) {
                    PlayerAnimation(skillID, "start");
                } else {
                    PlayerAnimation(skillID, "");
                }
            }
        }
    }

    private static void UseSkill(int selectedSkillIndex) {
        if (selectedSkillIndex < 0 || selectedSkillIndex >= SwordSkillRegistry.SKILLS.size())
            return;

        int currentSkillId = selectedSkillIndex + addSkillIndex;
        SkillData currentSkill = SwordSkillRegistry.SKILLS.get(currentSkillId);

        // 1. クールダウン中でないなら、コンボを開始
        if (!cooldowns.containsKey(selectedSkillIndex) || cooldowns.get(selectedSkillIndex) <= 0) {
            ExecuteSkill(selectedSkillIndex, selectedSkillIndex);
            addSkillIndex = 0;
            skillUsedTicks = 0;
        }
        // 2. クールダウン中に同じキーが押された場合、派生をチェック
        else if (skillUsedTicks != null && skillUsedTicks < limitTickMax && skillUsedTicks >= limitTickMin) {
            if (currentSkill != null && currentSkill.getType() == TRANSFORM) {
                int nextSkillId = currentSkillId + 1;

                // 次の段がアンロックされているかチェック（コンフィグでアンロック不要ならスルー）
                boolean isNextUnlocked = ClientUnlockedSkillsHandler.isUnlocked(nextSkillId)
                        || (ServerConfig.UnlockedSkill != null && !ServerConfig.UnlockedSkill.get());

                if (nextSkillId < SwordSkillRegistry.SKILLS.size() && isNextUnlocked) {
                    addSkillIndex++;
                    ExecuteSkill(nextSkillId, selectedSkillIndex);
                    skillUsedTicks = 0;
                }
            } else if (currentSkill != null && currentSkill.getType() == TRANSFORM_FINISH) {
                // フィニッシュ発動後の再入力はリセット
                addSkillIndex = 0;
                skillUsedTicks = null;
            }
        }
    }

    static int getCoolDown(SkillData skillData) {
        LocalPlayer player = Minecraft.getInstance().player;
        double cooldownAttr = (player != null)
                ? player.getAttributeBaseValue(SwordSkillAttribute.COOLDOWN_ATTRIBUTE)
                : 1.0;
        return (int) (skillData.getCooldown() * cooldownAttr);
    }
}