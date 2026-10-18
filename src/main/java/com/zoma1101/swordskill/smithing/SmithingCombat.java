package com.zoma1101.swordskill.smithing;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 鍛造武器の戦闘補正と武器記憶の記録をまとめる。
 *
 * <p>命中の直前に {@link #pushContext(WeaponMemoryCategory, int)} を呼んでおくと、
 * {@code LivingIncomingDamageEvent} 側で攻撃の傾向を判別できる。突き / 凪払い /
 * 飛翔はこの経路で分類し、それ以外（通常攻撃など）は {@link WeaponMemoryCategory#NORMAL}
 * として扱う。</p>
 */
public final class SmithingCombat {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private record Context(WeaponMemoryCategory category, int cooldownTicks) {
    }

    private SmithingCombat() {
    }

    /** 鍛造武器かどうか（溶解インゴットは含まない）。 */
    public static boolean isForgedWeapon(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && SmithingRegistry.FORGED_WEAPON.isBound()
                && stack.is(SmithingRegistry.FORGED_WEAPON.get())
                && ForgedWeaponData.get(stack) != null;
    }

    public static void pushContext(WeaponMemoryCategory category, int cooldownTicks) {
        CONTEXT.set(new Context(category, cooldownTicks));
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

    public static WeaponMemoryCategory currentCategory() {
        Context context = CONTEXT.get();
        return context == null ? WeaponMemoryCategory.NORMAL : context.category();
    }

    /** 命中時のスキルクールダウン。押し込まれていなければ実行中スキルから拾う。 */
    public static int resolveCooldown(ServerPlayer player) {
        Context context = CONTEXT.get();
        if (context != null && context.cooldownTicks() >= 0) {
            return context.cooldownTicks();
        }
        return runningSkillCooldown(player);
    }

    /** 実行中スキルのクールダウン。実行していなければ -1。 */
    public static int runningSkillCooldown(ServerPlayer player) {
        int skillId = SkillExecutionManager.getRunningSkillId(player);
        if (skillId < 0) {
            return -1;
        }
        SkillData skill = SwordSkillRegistry.SKILLS.get(skillId);
        return skill == null ? -1 : skill.getCooldown();
    }

    public static boolean isShortCooldown(int cooldownTicks) {
        return cooldownTicks >= 0 && cooldownTicks <= ServerConfig.shortCooldownTicks.get();
    }

    /** 武器記憶から隠しパラメータを導出する。 */
    public static HiddenParams hiddenParams(WeaponMemory memory) {
        double perHit = ServerConfig.memoryDamageBonusPerHit.get();
        double damageCap = ServerConfig.memoryDamageBonusCap.get();
        return new HiddenParams(
                Math.min(ServerConfig.memoryCritDamageCap.get(),
                        memory.critHits() * ServerConfig.memoryCritDamagePerHit.get()),
                Math.min(damageCap, memory.thrustHits() * perHit),
                Math.min(damageCap, memory.sweepHits() * perHit),
                Math.min(damageCap, memory.flyingHits() * perHit),
                Math.min(ServerConfig.memoryCooldownReductionCap.get(),
                        memory.shortCooldownHits() * ServerConfig.memoryCooldownReductionPerHit.get()));
    }

    public static HiddenParams hiddenParams(ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        return data == null ? HiddenParams.NONE : hiddenParams(data.memory());
    }

    /** クリティカル判定。 */
    public static boolean rollCritical(ServerPlayer attacker, ItemStack weapon) {
        if (!isForgedWeapon(weapon)) {
            return false;
        }
        double chance = ServerConfig.critBaseChance.get();
        return chance > 0.0D && attacker.getRandom().nextDouble() < chance;
    }

    /** 傾向補正とクリティカルをまとめた与ダメージ倍率。 */
    public static double offenseMultiplier(ItemStack weapon, WeaponMemoryCategory category, boolean critical) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return 1.0D;
        }
        HiddenParams params = hiddenParams(data.memory());
        double multiplier = 1.0D + params.damageBonus(category);
        if (critical) {
            multiplier *= ServerConfig.critBaseMultiplier.get() + params.criticalDamageBonus();
        }
        return multiplier;
    }

    /** 武器記憶によるクールダウン倍率（1.0 未満で短縮）。 */
    public static double cooldownMultiplier(ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return 1.0D;
        }
        return 1.0D - hiddenParams(data.memory()).cooldownReduction();
    }

    public static void recordDealt(ItemStack weapon, WeaponMemoryCategory category, double damage,
            boolean critical, int cooldownTicks) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return;
        }
        ForgedWeaponData.set(weapon, data.withMemory(data.memory().addDealt(damage, category, critical,
                isShortCooldown(cooldownTicks))));
    }

    public static void recordTaken(ItemStack weapon, double damage) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return;
        }
        ForgedWeaponData.set(weapon, data.withMemory(data.memory().addTaken(damage)));
    }

    public static void recordKill(ItemStack weapon) {
        ForgedWeaponData data = ForgedWeaponData.get(weapon);
        if (data == null) {
            return;
        }
        ForgedWeaponData.set(weapon, data.withMemory(data.memory().addKill()));
    }
}
