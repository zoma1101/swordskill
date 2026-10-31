package com.zoma1101.swordskill.client.handler;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.SwordSkillFeatures;
import com.zoma1101.swordskill.AnimationUtils;
import com.zoma1101.swordskill.client.gui.HudPositionSettingScreen;
import com.zoma1101.swordskill.client.gui.ProficiencyOverviewScreen;
import com.zoma1101.swordskill.client.gui.StatusScreen;
import com.zoma1101.swordskill.client.gui.SwordSkillSelectionScreen;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.data.WeaponTypeDetector;
import com.zoma1101.swordskill.effects.SwordSkillAttribute;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.payload.CheckSkillUnlockedPayload;
import com.zoma1101.swordskill.payload.CounterChargePayload;
import com.zoma1101.swordskill.payload.SkillLoadSlotPayload;
import com.zoma1101.swordskill.payload.SkillRequestPayload;
import com.zoma1101.swordskill.payload.UseSkillPayload;
import com.zoma1101.swordskill.swordskills.SkillChains;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.KeyMapping;

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
import java.util.List;
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
    private static boolean SetWeaponType = false;
    private static SkillCharge activeSkillCharge;

    /** チャージ中にメインハンド武器を着色発光させるための色 (RGB)。-1 は非チャージ。 */
    public static int chargingItemColor = -1;

    /** 現在スキルをチャージ中かどうか。 */
    public static boolean isCharging = false;

    /** スキル使用キー（-1 = 選択中のスロット、0〜4 = 固定スロット）。 */
    private static final SkillUseKey[] SKILL_KEYS = {
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key, -1),
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key_0, 0),
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key_1, 1),
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key_2, 2),
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key_3, 3),
            new SkillUseKey(Keybindings.INSTANCE.SwordSkill_Use_Key_4, 4)
    };

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSkillSlotHandler.reset();
        ClientTickHandler.reset();
        ClientUnlockedSkillsHandler.reset(); // アンロック情報もリセット
        ClientSkillLevelHandler.reset(); // スキルレベルもリセット
        ClientProficiencyHandler.reset(); // 熟練度もリセット
        ClientPlayerLevelHandler.reset(); // プレイヤーレベルもリセット
        cooldowns.clear();
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
        PacketDistributor.sendToServer(new CheckSkillUnlockedPayload());
        PacketDistributor.sendToServer(new com.zoma1101.swordskill.payload.RequestSkillLevelsPayload());
        SetWeaponType = true;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleSystemKeyInteraction(event.getKey(), false);
            suppressVanillaHotbarSwitch(event.getKey(), event.getScanCode());
        }
    }

    /**
     * スキル使用キーがバニラのホットバー選択キー（1〜9）と重なっている場合、
     * バニラ側のホットバー切り替えを打ち消す。今の武器でそのスキルを撃てないときは
     * 通常どおりホットバーを切り替える。
     */
    private static void suppressVanillaHotbarSwitch(int key, int scanCode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.options == null) {
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            KeyMapping hotbar = minecraft.options.keyHotbarSlots[slot];
            if (!hotbar.matches(key, scanCode)) {
                continue;
            }
            if (!isSkillUsableFromKey(key)) {
                return;
            }
            // KeyMapping.click() はこのイベントの直前に積まれているので、ここで消費して打ち消す
            while (hotbar.consumeClick()) {
                // 何もしない
            }
            return;
        }
    }

    /** そのキーに割り当てたスキルを、今装備している武器種で撃てるか。 */
    private static boolean isSkillUsableFromKey(int key) {
        Minecraft minecraft = Minecraft.getInstance();
        int[] skillSlots = ClientSkillSlotHandler.getSkillSlotInfo();
        Set<SkillData.WeaponType> equipped = ClientSkillSlotHandler.getCurrentWeaponTypes();
        if (skillSlots == null || equipped == null || equipped.isEmpty()) {
            return false;
        }
        for (SkillUseKey skillKey : SKILL_KEYS) {
            if (skillKey.mapping().getKey().getValue() != key) {
                continue;
            }
            int slot = skillKey.slot() < 0 ? minecraft.player.getInventory().selected : skillKey.slot();
            if (slot < 0 || slot >= skillSlots.length) {
                return false;
            }
            SkillData skill = SwordSkillRegistry.SKILLS.get(skillSlots[slot]);
            if (skill == null || !skill.isAssignable() || skill.getDataDefinition() == null) {
                return false;
            }
            return skill.getAvailableWeaponTypes().stream().anyMatch(equipped::contains);
        }
        return false;
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
        } else if (Keybindings.INSTANCE.SwordSkill_Status_Key.isActiveAndMatches(inputKey)) {
            // ステータス画面が無効なリリースでは、熟練度一覧を直接開く
            if (SwordSkillFeatures.STATUS_SCREEN) {
                mc.setScreen(new StatusScreen());
            } else {
                mc.setScreen(new ProficiencyOverviewScreen());
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

    private static void ExecuteSkill(int skillID, int coolDownSkillID, int chargeTicks) {
        SkillData skillData = SwordSkillRegistry.SKILLS.get(skillID);
        if (skillData != null && canAffordSkill(skillData)) {
            Set<SkillData.WeaponType> weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            String weaponName = ClientSkillSlotHandler.getCurrentWeaponName();
            if (weaponName != null && skillData.getAvailableWeaponTypes().stream()
                    .anyMatch(Objects.requireNonNull(weaponType)::contains)) {

                // サーバーへ送信
                int appliedChargeTicks = (skillData.getDataDefinition() == null || !skillData.isChargeable()) ? 0 : chargeTicks;
                PacketDistributor.sendToServer(
                        new UseSkillPayload(skillData.getId(), appliedChargeTicks));

                // クールダウン設定（スロットに対して設定）
                cooldowns.put(coolDownSkillID, getCoolDown(skillData));

                // アニメーション再生（サーバーからも同じ指示が届くのでエコーとして読み飛ばす）
                String predictedType = skillData.getType().equals(RUSH) ? "start" : "";
                PlayerAnimation(Minecraft.getInstance().player.getId(), skillID, predictedType);
                AnimationUtils.expectServerEcho(skillID, predictedType);
            }
        }
    }

    private static void UseSkill(int selectedSkillIndex, int chargeTicks) {
        UseSkill(selectedSkillIndex, chargeTicks, -1);
    }

    private static void UseSkill(int selectedSkillIndex, int chargeTicks, int comboNextSkillId) {
        if (selectedSkillIndex < 0 || selectedSkillIndex >= SwordSkillRegistry.SKILLS.size())
            return;
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        if (selectedSkill == null || selectedSkill.getDataDefinition() == null)
            return;

        // クールダウン中でないなら、通常発動
        if (cooldowns.getOrDefault(selectedSkillIndex, 0) <= 0) {
            // 派生先は元スキルからの派生時のみ使用できる（古いスロットデータ対策）
            if (SkillChains.isDerived(selectedSkillIndex)) {
                return;
            }
            ExecuteSkill(selectedSkillIndex, selectedSkillIndex, chargeTicks);
            return;
        }

        // クールダウン中は派生しない（古いコンボ方式は廃止）
        return;
    }

    /** horizontal / vertical 系のチャージ対象スキルから、元の軸名を取り出す。 */
    private static String chargeAxisOf(String name) {
        if (name.equals("horizontal") || name.startsWith("horizontal_")) {
            return "horizontal";
        }
        if (name.equals("vertical") || name.startsWith("vertical_")) {
            return "vertical";
        }
        return null;
    }

    private static boolean isChargeSkill(SkillData skill) {
        return chargeAxisOf(skill.getName()) != null && skill.isChargeable();
    }

    /** チャージ時に実際に狙う軸の元スキルID。チャージ対象外なら -1。 */
    private static int chargeTargetBaseId(SkillData skill, boolean crouching) {
        String sourceAxis = chargeAxisOf(skill.getName());
        if (sourceAxis == null) {
            return -1;
        }
        String targetAxis = crouching
                ? (sourceAxis.equals("horizontal") ? "vertical" : "horizontal")
                : sourceAxis;
        return findSkillIdByName(targetAxis);
    }

    private static boolean isSameChain(int firstSkillId, int secondSkillId) {
        return firstSkillId >= 0 && secondSkillId >= 0
                && SkillChains.start(firstSkillId) == SkillChains.start(secondSkillId);
    }

    private static boolean isNextUnlocked(int skillId) {
        return skillId >= 0
                && (ClientUnlockedSkillsHandler.isUnlocked(skillId)
                        || (ServerConfig.UnlockedSkill != null && !ServerConfig.UnlockedSkill.get()));
    }

    private static boolean isWeaponUsable(SkillData skill, Set<SkillData.WeaponType> weaponTypes) {
        return skill != null && weaponTypes != null && !weaponTypes.isEmpty()
                && skill.getAvailableWeaponTypes().stream().anyMatch(weaponTypes::contains);
    }

    /**
     * クライアントが保持している SP で発動できるか。同期値はサーバーより僅かに遅れるが、
     * 常にサーバー値以下なので、ここで足りなければサーバーでも必ず拒否される。
     */
    private static boolean canAffordSkill(SkillData skill) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (skill == null) {
            return false;
        }
        if (player == null || player.isCreative()) {
            return true;
        }
        return com.zoma1101.swordskill.network.toClient.ClientSPData.get() >= skill.getSpCost();
    }

    /** SP が足りない段を選んでいたら、同じチェーン内の払える段まで下げる。 */
    private static SkillData highestAffordableTier(SkillData target) {
        SkillData candidate = target;
        while (candidate != null && !canAffordSkill(candidate)) {
            int parentId = SkillChains.parentOf(candidate.getId());
            if (parentId < 0) {
                return null;
            }
            candidate = SwordSkillRegistry.SKILLS.get(parentId);
        }
        return candidate;
    }

    /**
     * チャージ開始前に、そのスキルが実際に発動できる状態かを判定する。
     * 発動できないときにチャージ演出（構え・発光・効果音）だけが再生されるのを防ぐ。
     */
    private static boolean canStartSkillUse(int selectedSkillIndex, boolean crouching) {
        if (selectedSkillIndex < 0 || selectedSkillIndex >= SwordSkillRegistry.SKILLS.size()) {
            return false;
        }
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
        if (selectedSkill == null || selectedSkill.getDataDefinition() == null) {
            return false;
        }
        Set<SkillData.WeaponType> weaponTypes = ClientSkillSlotHandler.getCurrentWeaponTypes();
        if (!isWeaponUsable(selectedSkill, weaponTypes)) {
            return false;
        }

        if (cooldowns.getOrDefault(selectedSkillIndex, 0) <= 0) {
            if (SkillChains.isDerived(selectedSkillIndex)) {
                return false;
            }
            if (!canAffordSkill(selectedSkill)) {
                return false;
            }
            // しゃがみ中のチャージは別軸のスキルを発動するため、その軸のクールダウン・武器種・SPも必要
            if (isChargeSkill(selectedSkill)) {
                int targetBaseId = chargeTargetBaseId(selectedSkill, crouching);
                return targetBaseId < 0
                        || (cooldowns.getOrDefault(targetBaseId, 0) <= 0
                                && isWeaponUsable(SwordSkillRegistry.SKILLS.get(targetBaseId), weaponTypes)
                                && canAffordSkill(SwordSkillRegistry.SKILLS.get(targetBaseId)));
            }
            return true;
        }

        // クールダウン中は派生しない（古いコンボ方式は廃止）
        return false;
    }

    private static SkillData getChargeTransformVariant(int selectedSkillId, int chargeTicks, boolean crouching) {
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillId);
        if (selectedSkill == null || selectedSkill.getDataDefinition() == null || !selectedSkill.isChargeable()) return null;
        String sourceAxis = chargeAxisOf(selectedSkill.getName());
        if (sourceAxis != null) {
            return getAxisChargeVariant(sourceAxis, chargeTicks, crouching);
        }
        return getChainChargeVariant(selectedSkill, chargeTicks);
    }

    /** horizontal / vertical 系のチャージ派生（しゃがみで軸を入れ替える）。 */
    private static SkillData getAxisChargeVariant(String sourceAxis, int chargeTicks, boolean crouching) {
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

    /**
     * excite など、horizontal / vertical 以外の TRANSFORM チェーンをチャージ段階で進める。
     * 選択中スキルがチェーン先頭のときだけ 1 段目・2 段目を返す。
     */
    private static SkillData getChainChargeVariant(SkillData selectedSkill, int chargeTicks) {
        if (!SkillChains.isChainMember(selectedSkill)
                || SkillChains.start(selectedSkill.getId()) != selectedSkill.getId()) {
            return null;
        }
        List<Integer> chain = SkillChains.chainOf(selectedSkill.getId());
        if (chain.isEmpty()) {
            return null;
        }
        int stage = Math.min(chargeTicks < 14 ? 0 : chargeTicks < 27 ? 1 : 2, chain.size() - 1);
        boolean canUseLockedSkills = ServerConfig.UnlockedSkill != null && !ServerConfig.UnlockedSkill.get();
        while (stage >= 0) {
            SkillData target = SwordSkillRegistry.SKILLS.get(chain.get(stage));
            if (target != null && target.getDataDefinition() != null
                    && (canUseLockedSkills || ClientUnlockedSkillsHandler.isUnlocked(target.getId()))) {
                return target;
            }
            stage--;
        }
        return null;
    }

    private static void UseChargedTransformSkill(int selectedSkillId, int chargeTicks, boolean crouching) {
        UseChargedTransformSkill(selectedSkillId, chargeTicks, crouching, -1);
    }

    private static void UseChargedTransformSkill(int selectedSkillId, int chargeTicks, boolean crouching,
            int comboNextSkillId) {
        SkillData targetSkill = getChargeTransformVariant(selectedSkillId, chargeTicks, crouching);
        if (targetSkill == null) {
            UseSkill(selectedSkillId, chargeTicks);
            return;
        }

        // horizontal/vertical は軸の元スキルのクールダウンも共有する。
        // それ以外のチェーン（excite など）は選択中スキル自身がチェーン先頭。
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillId);
        String sourceAxis = selectedSkill == null ? null : chargeAxisOf(selectedSkill.getName());
        int baseSkillId;
        if (sourceAxis != null) {
            baseSkillId = targetSkill.getName().startsWith("vertical")
                    ? findSkillIdByName("vertical") : findSkillIdByName("horizontal");
            if (baseSkillId < 0) {
                UseSkill(selectedSkillId, chargeTicks);
                return;
            }
        } else {
            baseSkillId = selectedSkillId;
        }

        boolean cooldownReady = cooldowns.getOrDefault(selectedSkillId, 0) <= 0
                && cooldowns.getOrDefault(baseSkillId, 0) <= 0;
        if (!cooldownReady) return;

        // 長押しで選んだ段の SP が足りない場合は、払える段まで下げて発動する
        targetSkill = highestAffordableTier(targetSkill);
        if (targetSkill == null) {
            return;
        }

        ExecuteSkill(targetSkill.getId(), selectedSkillId, chargeTicks);
        // 別のキーからチャージ派生した場合も、派生元チェーンのスロットをクールダウンさせる
        if (baseSkillId != selectedSkillId) {
            cooldowns.put(baseSkillId, getCoolDown(targetSkill));
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
        SkillUseKey[] keys = SKILL_KEYS;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || !player.isAlive()) {
            clearSkillCharge(player);
            for (SkillUseKey key : keys) {
                while (key.mapping().consumeClick()) {
                }
            }
            return;
        }

        // 鍔迫り合い中はスキル入力を消費する（サーバーも発動を拒否する）
        if (com.zoma1101.swordskill.client.handler.SkillClashHandler.isActive()) {
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
                    // クールダウン中・派生受付外・武器不一致などで発動できない場合は、
                    // チャージ演出だけが再生されないようにここで消費する
                    if (!canStartSkillUse(skillIndex, player.isCrouching())) {
                        continue;
                    }
                    // チャージ不可スキルは長押し charge を行わず即時発動
                    if (!skill.isChargeable()) {
                        UseSkill(skillIndex, 0);
                        continue;
                    }
                    String animationType = skill.getType() == RUSH ? "start" : "";
                    AnimationUtils.PlayerChargeAnim(player, skillIndex, animationType);
                    if (skill.isCounter()) {
                        PacketDistributor.sendToServer(new CounterChargePayload(skillIndex, true));
                    }
                    ItemStack weapon = player.getMainHandItem();
                    Boolean originalGlint = weapon.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                    if (!weapon.isEmpty()) {
                        weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    }
                    // スキル色も保持（将来的な直接着色用）
                    if (skill.getDataDefinition() != null) {
                        String hex = skill.getDataDefinition().color();
                        if (hex != null && hex.startsWith("#") && hex.length() == 7) {
                            try {
                                chargingItemColor = Integer.parseInt(hex.substring(1), 16);
                            } catch (NumberFormatException ignored) {
                                chargingItemColor = -1;
                            }
                        }
                    }
                    isCharging = true;
                    activeSkillCharge = new SkillCharge(key.mapping(), skillIndex, player.tickCount,
                            weapon, null, -1);
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
            UseChargedTransformSkill(charge.skillIndex(), chargeTicks, player.isCrouching(),
                    charge.comboNextSkillId());
        }
    }

    private static void clearSkillCharge(LocalPlayer player) {
        if (activeSkillCharge == null) {
            return;
        }
        SkillData chargeSkill = SwordSkillRegistry.SKILLS.get(activeSkillCharge.skillIndex());
        if (chargeSkill != null && chargeSkill.isCounter()) {
            PacketDistributor.sendToServer(new CounterChargePayload(activeSkillCharge.skillIndex(), false));
        }
        isCharging = false;
        ItemStack weapon = activeSkillCharge.weapon();
        if (!weapon.isEmpty()) {
            if (activeSkillCharge.originalGlint() == null) {
                weapon.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            } else {
                weapon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, activeSkillCharge.originalGlint());
            }
        }
        chargingItemColor = -1;
        if (player != null) {
            AnimationUtils.StopPlayerAnimation(player);
        }
        activeSkillCharge = null;
    }

    private record SkillUseKey(KeyMapping mapping, int slot) {
    }

    private record SkillCharge(KeyMapping mapping, int skillIndex, int startTick,
            ItemStack weapon, Boolean originalGlint, int comboNextSkillId) {
    }

    static int getCoolDown(SkillData skillData) {
        LocalPlayer player = Minecraft.getInstance().player;
        double cooldownAttr = (player != null)
                ? player.getAttributeBaseValue(SwordSkillAttribute.COOLDOWN_ATTRIBUTE)
                : 1.0;
        return (int) (skillData.getCooldown() * cooldownAttr);
    }
}
