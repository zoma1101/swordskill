package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.AnimationUtils;
import com.zoma1101.swordskill.client.gui.HudPositionSettingScreen;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.payload.RequestUnlockSkillPayload;
import com.zoma1101.swordskill.payload.SkillLoadSlotPayload;
import com.zoma1101.swordskill.payload.SkillRequestPayload;
import com.zoma1101.swordskill.payload.UseSkillPayload;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
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
    private static Integer transformChainBaseId = null;
    private static Integer skillUsedTicks = null;
    private static Integer limitTickMax = 20;
    private static final Integer limitTickMin = 0; // 0に短縮して反応を改善
    private static boolean SetWeaponType = false;
    private static SkillCharge activeSkillCharge;

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkillSlotHandler.reset();
        ClientUnlockedSkillsHandler.reset(); // アンロック情報もリセット
        ClientSkillLevelHandler.reset(); // スキルレベルもリセット
        cooldowns.clear();
        addSkillIndex = 0;
        transformChainBaseId = null;
        skillUsedTicks = null;
        clearSkillCharge(Minecraft.getInstance().player);
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

        handleSkillUseInputs(player);

        // コンボ受付時間のカウント
        if (skillUsedTicks != null) {
            skillUsedTicks++;
            if (skillUsedTicks > limitTickMax) {
                addSkillIndex = 0;
                transformChainBaseId = null;
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
        PacketDistributor.sendToServer(new com.zoma1101.swordskill.payload.RequestSkillLevelsPayload());
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

    private static void ExecuteSkill(int skillID, int coolDownSkillID, int chargeTicks) {
        SkillData skillData = SwordSkillRegistry.SKILLS.get(skillID);
        if (skillData != null) {
            Set<SkillData.WeaponType> weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
            if (weaponName != null && skillData.getAvailableWeaponTypes().stream()
                    .anyMatch(Objects.requireNonNull(weaponType)::contains)) {

                // サーバーへ送信
                int appliedChargeTicks = skillData.getDataDefinition() == null ? 0 : chargeTicks;
                PacketDistributor.sendToServer(
                        new UseSkillPayload(skillData.getId(), skillData.getFinalTick(), appliedChargeTicks));

                // クールダウン設定（スロットに対して設定）
                cooldowns.put(coolDownSkillID, getCoolDown(skillData));

                // 受付時間更新
                limitTickMax = skillData.getTransformLimitTick();

                // アニメーション再生
                if (skillData.getType().equals(RUSH)) {
                    PlayerAnimation(Minecraft.getInstance().player.getId(), skillID, "start");
                } else {
                    PlayerAnimation(Minecraft.getInstance().player.getId(), skillID, "");
                }
            }
        }
    }

    private static void UseSkill(int selectedSkillIndex, int chargeTicks) {
        if (selectedSkillIndex < 0 || selectedSkillIndex >= SwordSkillRegistry.SKILLS.size())
            return;

        int currentSkillId = (transformChainBaseId == null ? selectedSkillIndex : transformChainBaseId)
                + addSkillIndex;
        SkillData currentSkill = SwordSkillRegistry.SKILLS.get(currentSkillId);

        // 1. クールダウン中でないなら、コンボを開始
        if (!cooldowns.containsKey(selectedSkillIndex) || cooldowns.get(selectedSkillIndex) <= 0) {
            ExecuteSkill(selectedSkillIndex, selectedSkillIndex, chargeTicks);
            addSkillIndex = 0;
            transformChainBaseId = null;
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
                    ExecuteSkill(nextSkillId, selectedSkillIndex, chargeTicks);
                    skillUsedTicks = 0;
                }

            } else if (currentSkill != null && currentSkill.getType() == TRANSFORM_FINISH) {
                // フィニッシュ発動後の再入力はリセット
                addSkillIndex = 0;
                transformChainBaseId = null;
                skillUsedTicks = null;
            }
        }
    }

    private static SkillData getChargeTransformVariant(int selectedSkillId, int chargeTicks, boolean crouching) {
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillId);
        if (selectedSkill == null || selectedSkill.getDataDefinition() == null) return null;
        String name = selectedSkill.getName();
        String sourceAxis;
        if (name.equals("horizontal") || name.startsWith("horizontal_")) {
            sourceAxis = "horizontal";
        } else if (name.equals("vertical") || name.startsWith("vertical_")) {
            sourceAxis = "vertical";
        } else {
            return null;
        }

        String targetAxis = crouching
                ? (sourceAxis.equals("horizontal") ? "vertical" : "horizontal")
                : sourceAxis;
        int stage = chargeTicks < 14 ? 0 : chargeTicks < 27 ? 1 : 2;
        String targetName = targetAxis + (stage == 0 ? "" : stage == 1 ? "_arc" : "_square");
        SkillData target = findSkillByName(targetName);
        if (target == null || target.getDataDefinition() == null) return null;

        // If the charge tier is locked, use the highest unlocked tier instead.
        boolean canUseLockedSkills = ServerConfig.UnlockedSkill != null && !ServerConfig.UnlockedSkill.get();
        while (target != null && stage >= 0
                && !canUseLockedSkills && !ClientUnlockedSkillsHandler.isUnlocked(target.getId())) {
            if (stage == 0) return null;
            stage--;
            targetName = targetAxis + (stage == 0 ? "" : stage == 1 ? "_arc" : "_square");
            target = findSkillByName(targetName);
        }
        return target;
    }

    private static void UseChargedTransformSkill(int selectedSkillId, int chargeTicks, boolean crouching) {
        SkillData targetSkill = getChargeTransformVariant(selectedSkillId, chargeTicks, crouching);
        if (targetSkill == null) {
            UseSkill(selectedSkillId, chargeTicks);
            return;
        }

        int baseSkillId = targetSkill.getName().startsWith("vertical")
                ? findSkillIdByName("vertical") : findSkillIdByName("horizontal");
        if (baseSkillId < 0) {
            UseSkill(selectedSkillId, chargeTicks);
            return;
        }

        boolean cooldownReady = cooldowns.getOrDefault(selectedSkillId, 0) <= 0;
        int activeChainId = transformChainBaseId == null ? selectedSkillId : transformChainBaseId;
        SkillData activeChainSkill = SwordSkillRegistry.SKILLS.get(activeChainId + addSkillIndex);
        boolean comboWindowOpen = skillUsedTicks != null && skillUsedTicks < limitTickMax
                && skillUsedTicks >= limitTickMin && activeChainSkill != null
                && (activeChainSkill.getType() == TRANSFORM || activeChainSkill.getType() == TRANSFORM_FINISH);
        if (!cooldownReady && !comboWindowOpen) return;

        ExecuteSkill(targetSkill.getId(), selectedSkillId, chargeTicks);
        String targetName = targetSkill.getName();
        if (targetName.endsWith("_square")) {
            addSkillIndex = 0;
            transformChainBaseId = null;
            skillUsedTicks = null;
        } else {
            addSkillIndex = targetName.endsWith("_arc") ? 1 : 0;
            transformChainBaseId = baseSkillId;
            skillUsedTicks = 0;
        }
    }

    private static int findSkillIdByName(String name) {
        SkillData skill = findSkillByName(name);
        return skill == null ? -1 : skill.getId();
    }

    private static SkillData findSkillByName(String name) {
        return SwordSkillRegistry.SKILLS.values().stream()
                .filter(skill -> skill.getName().equals(name))
                .findFirst().orElse(null);
    }

    private static void handleSkillUseInputs(LocalPlayer player) {
        Keybindings bindings = Keybindings.INSTANCE;
        SkillUseKey[] keys = {
                new SkillUseKey(bindings.SwordSkill_Use_Key, -1),
                new SkillUseKey(bindings.SwordSkill_Use_Key_0, 0),
                new SkillUseKey(bindings.SwordSkill_Use_Key_1, 1),
                new SkillUseKey(bindings.SwordSkill_Use_Key_2, 2),
                new SkillUseKey(bindings.SwordSkill_Use_Key_3, 3),
                new SkillUseKey(bindings.SwordSkill_Use_Key_4, 4)
        };
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !player.isAlive()) {
            clearSkillCharge(player);
            for (SkillUseKey key : keys) {
                while (key.mapping().consumeClick()) {
                }
            }
            return;
        }

        for (SkillUseKey key : keys) {
            while (key.mapping().consumeClick()) {
                if (activeSkillCharge != null) {
                    continue;
                }
                int skillIndex = key.slot() < 0
                        ? ClientSkillSlotHandler.getSkillSlotInfo()[getSelectedSlot()]
                        : ClientSkillSlotHandler.getSkillSlotInfo()[key.slot()];
                SkillData skill = SwordSkillRegistry.SKILLS.get(skillIndex);
                if (skill != null && skill.getDataDefinition() != null) {
                    String animationType = skill.getType() == RUSH ? "start" : "";
                    AnimationUtils.PlayerChargeAnim(player, skillIndex, animationType);
                    ItemStack weapon = player.getMainHandItem();
                    Boolean originalGlint = weapon.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                    if (!weapon.isEmpty()) {
                        weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    }
                    SkillChargeSound sound = new SkillChargeSound(player);
                    Minecraft.getInstance().getSoundManager().play(sound);
                    activeSkillCharge = new SkillCharge(key.mapping(), skillIndex, player.tickCount, sound,
                            weapon, originalGlint);
                } else {
                    UseSkill(skillIndex, 0);
                }
            }
        }

        if (activeSkillCharge != null && !activeSkillCharge.mapping().isDown()) {
            SkillCharge charge = activeSkillCharge;
            int chargeTicks = Math.max(0, Math.min(DataDrivenSkill.MAX_CHARGE_TICKS,
                    player.tickCount - charge.startTick()));
            clearSkillCharge(player);
            UseChargedTransformSkill(charge.skillIndex(), chargeTicks, player.isCrouching());
        }
    }

    private static void clearSkillCharge(LocalPlayer player) {
        if (activeSkillCharge == null) {
            return;
        }
        activeSkillCharge.sound().stopPlaying();
        ItemStack weapon = activeSkillCharge.weapon();
        if (!weapon.isEmpty()) {
            if (activeSkillCharge.originalGlint() == null) {
                weapon.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            } else {
                weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, activeSkillCharge.originalGlint());
            }
        }
        if (player != null) {
            AnimationUtils.StopPlayerAnimation(player);
        }
        activeSkillCharge = null;
    }

    private record SkillUseKey(KeyMapping mapping, int slot) {
    }

    private record SkillCharge(KeyMapping mapping, int skillIndex, int startTick, SkillChargeSound sound,
            ItemStack weapon, Boolean originalGlint) {
    }

    private static class SkillChargeSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        private final int startTick;

        private SkillChargeSound(LocalPlayer player) {
            super(SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, RandomSource.create());
            this.player = player;
            this.startTick = player.tickCount;
            this.looping = true;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = 0.35F;
            updatePosition();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !player.isAlive()) {
                stop();
                return;
            }
            updatePosition();
            float progress = Math.min(1.0F,
                    (float) (player.tickCount - startTick) / DataDrivenSkill.MAX_CHARGE_TICKS);
            this.pitch = 0.75F + progress * 0.5F;
        }

        private void updatePosition() {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        private void stopPlaying() {
            stop();
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
