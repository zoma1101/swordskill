package com.zoma1101.swordskill.smithing;

import net.minecraft.nbt.CompoundTag;

/**
 * 武器記憶。与えたダメージ・受けたダメージと攻撃の傾向（突き / 凪払い / 飛翔 / クリティカル / 短CT）を蓄積する。
 *
 * <p>この記録から隠しパラメータ（{@link HiddenParams}）が導出される。武器を溶かして再鍛造しても、
 * インゴット経由でこの記録が引き継がれる。</p>
 */
public record WeaponMemory(
        double damageDealt,
        double damageTaken,
        int hits,
        int thrustHits,
        int sweepHits,
        int flyingHits,
        int critHits,
        int shortCooldownHits,
        int kills) {

    public static final WeaponMemory EMPTY = new WeaponMemory(0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0);

    public WeaponMemory addDealt(double damage, WeaponMemoryCategory category, boolean critical,
            boolean shortCooldown) {
        return new WeaponMemory(
                damageDealt + damage,
                damageTaken,
                hits + 1,
                thrustHits + (category == WeaponMemoryCategory.THRUST ? 1 : 0),
                sweepHits + (category == WeaponMemoryCategory.SWEEP ? 1 : 0),
                flyingHits + (category == WeaponMemoryCategory.FLYING ? 1 : 0),
                critHits + (critical ? 1 : 0),
                shortCooldownHits + (shortCooldown ? 1 : 0),
                kills);
    }

    public WeaponMemory addTaken(double damage) {
        return new WeaponMemory(damageDealt, damageTaken + damage, hits, thrustHits, sweepHits, flyingHits,
                critHits, shortCooldownHits, kills);
    }

    public WeaponMemory addKill() {
        return new WeaponMemory(damageDealt, damageTaken, hits, thrustHits, sweepHits, flyingHits, critHits,
                shortCooldownHits, kills + 1);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("dealt", damageDealt);
        tag.putDouble("taken", damageTaken);
        tag.putInt("hits", hits);
        tag.putInt("thrust", thrustHits);
        tag.putInt("sweep", sweepHits);
        tag.putInt("flying", flyingHits);
        tag.putInt("crit", critHits);
        tag.putInt("short_ct", shortCooldownHits);
        tag.putInt("kills", kills);
        return tag;
    }

    public static WeaponMemory load(CompoundTag tag) {
        return new WeaponMemory(
                tag.getDouble("dealt"),
                tag.getDouble("taken"),
                tag.getInt("hits"),
                tag.getInt("thrust"),
                tag.getInt("sweep"),
                tag.getInt("flying"),
                tag.getInt("crit"),
                tag.getInt("short_ct"),
                tag.getInt("kills"));
    }
}
