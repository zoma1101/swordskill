package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.playerlevel.PlayerLevelManager;
import com.zoma1101.swordskill.playerlevel.StatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鍛造武器・防具の STR 要件を判定する。
 *
 * <ul>
 *   <li>STR &ge; 必要量 … 通常どおり装備できる</li>
 *   <li>必要量の半分 &le; STR &lt; 必要量 … 攻撃速度とソードスキル速度が低下</li>
 *   <li>STR &lt; 必要量の半分 … 装備できず、持った瞬間に弾かれて落ちる</li>
 * </ul>
 */
public final class SmithingEquipmentHandler {

    private static final ResourceLocation PENALTY_ID = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID,
            "weight_penalty");
    private static final Set<UUID> REJECTING = ConcurrentHashMap.newKeySet();

    private SmithingEquipmentHandler() {
    }

    public static int requiredStr(ItemStack stack) {
        // 溶解インゴットにも鍛造データが乗るが、装備判定の対象は武器だけ
        if (!SmithingCombat.isForgedWeapon(stack)) {
            return 0;
        }
        ForgedWeaponData data = ForgedWeaponData.get(stack);
        return data == null ? 0 : data.requiredStr();
    }

    public static int strengthOf(ServerPlayer player) {
        return PlayerLevelManager.getStat(player, StatType.STR);
    }

    /** STR 要件が有効かどうか（鍛冶・レベルシステムが有効なときだけ）。 */
    private static boolean gatingEnabled() {
        return ServerConfig.smithingEnabled.get() && ServerConfig.levelSystemEnabled.get();
    }

    /** 装備できるか（必要 STR の半分未満なら不可）。 */
    public static boolean canEquip(ServerPlayer player, ItemStack stack) {
        if (!gatingEnabled()) {
            return true;
        }
        int required = requiredStr(stack);
        return required <= 0 || strengthOf(player) * 2 >= required;
    }

    /** 必要 STR に届かず、攻撃速度・スキル速度が落ちているか。 */
    public static boolean isOverweight(ServerPlayer player) {
        if (!gatingEnabled()) {
            return false;
        }
        int required = requiredStr(player.getMainHandItem());
        return required > 0 && strengthOf(player) < required;
    }

    /** ソードスキル速度の倍率（1.0 で等速、小さいほど遅い）。 */
    public static double skillSpeedMultiplier(ServerPlayer player) {
        return isOverweight(player) ? ServerConfig.overweightSkillSpeedMultiplier.get() : 1.0D;
    }

    /** 装備変更時に呼ぶ。装備できない武器はその場で弾く。 */
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack to = event.getTo();
        if (to.isEmpty() || requiredStr(to) <= 0) {
            return;
        }
        if (!canEquip(player, to)) {
            reject(player, event.getSlot(), to);
        }
    }

    /** 毎 tick 呼ぶ。メインハンドの装備不可チェックと、重量ペナルティの反映を行う。 */
    public static void update(ServerPlayer player) {
        if (!gatingEnabled()) {
            removePenalty(player);
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        int required = requiredStr(mainHand);
        if (required > 0 && strengthOf(player) * 2 < required) {
            reject(player, EquipmentSlot.MAINHAND, mainHand);
            removePenalty(player);
            return;
        }

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }
        boolean overweight = required > 0 && strengthOf(player) < required;
        AttributeModifier existing = attackSpeed.getModifier(PENALTY_ID);
        if (overweight && existing == null) {
            attackSpeed.addTransientModifier(new AttributeModifier(PENALTY_ID,
                    ServerConfig.overweightAttackSpeedPenalty.get(),
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!overweight && existing != null) {
            attackSpeed.removeModifier(PENALTY_ID);
        }
    }

    private static void removePenalty(ServerPlayer player) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null && attackSpeed.getModifier(PENALTY_ID) != null) {
            attackSpeed.removeModifier(PENALTY_ID);
        }
    }

    /** 装備不可の武器を弾いて落とす。 */
    private static void reject(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (!REJECTING.add(player.getUUID())) {
            return;
        }
        try {
            ItemStack dropped = stack.copy();
            player.setItemSlot(slot, ItemStack.EMPTY);
            player.drop(dropped, false);
            player.displayClientMessage(Component.translatable("message." + SwordSkill.MOD_ID
                    + ".smithing.overweight_equip", requiredStr(stack), strengthOf(player)), true);
        } finally {
            REJECTING.remove(player.getUUID());
        }
    }
}
