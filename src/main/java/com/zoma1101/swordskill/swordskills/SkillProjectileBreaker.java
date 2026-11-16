package com.zoma1101.swordskill.swordskills;

import com.zoma1101.swordskill.config.ServerConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * ソードスキルの判定に触れた飛翔体（矢・弾・魔法弾など）を破壊する。
 *
 * <p>{@link Projectile} を対象にしているため、バニラの矢・火球・雪玉・ポーションに加え、
 * 大多数の Mod 製魔法弾もまとめて処理できる。自分が撃った飛翔体と釣り針は除外する。</p>
 */
public final class SkillProjectileBreaker {
    private SkillProjectileBreaker() {
    }

    /** {@code area} 内にある破壊可能な飛翔体を消す。戻り値は破壊した数。 */
    public static int destroyIn(Level level, @Nullable Entity owner, AABB area) {
        return destroyIn(level, owner, area, projectile -> true);
    }

    /** {@code area} 内のうち追加条件を満たす飛翔体を消す（向き付きヒットボックス用）。 */
    public static int destroyIn(Level level, @Nullable Entity owner, AABB area,
            Predicate<Projectile> extraFilter) {
        if (!canRun(level)) {
            return 0;
        }
        int destroyed = 0;
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
                projectile -> isDestructible(projectile, owner) && extraFilter.test(projectile))) {
            breakProjectile(level, projectile);
            destroyed++;
        }
        return destroyed;
    }

    /**
     * 線分 {@code from} → {@code to} の周囲 {@code radius} にある飛翔体を消す。
     * 突進や高速な飛翔斬撃が1tickで通り抜けても取りこぼさないようにするためのもの。
     */
    public static int destroyAlong(Level level, @Nullable Entity owner, Vec3 from, Vec3 to, double radius) {
        if (!canRun(level)) {
            return 0;
        }
        AABB area = new AABB(from, to).inflate(radius + 1.0D);
        int destroyed = 0;
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
                projectile -> isDestructible(projectile, owner))) {
            double half = projectile.getBbWidth() * 0.5D;
            if (distanceToSegment(projectile.position(), from, to) <= radius + half) {
                breakProjectile(level, projectile);
                destroyed++;
            }
        }
        return destroyed;
    }

    private static boolean canRun(Level level) {
        return !level.isClientSide && ServerConfig.skillBreaksProjectiles.get();
    }

    private static boolean isDestructible(Projectile projectile, @Nullable Entity owner) {
        if (projectile.isRemoved() || projectile instanceof FishingHook) {
            return false;
        }
        // 自分（スキル実行者）が撃った飛翔体は消さない
        return owner == null || projectile.getOwner() != owner;
    }

    private static void breakProjectile(Level level, Projectile projectile) {
        Vec3 position = projectile.position();
        projectile.discard();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    position.x, position.y, position.z, 6, 0.15D, 0.15D, 0.15D, 0.05D);
        }
    }

    private static double distanceToSegment(Vec3 point, Vec3 from, Vec3 to) {
        Vec3 segment = to.subtract(from);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-9D) {
            return point.distanceTo(from);
        }
        double t = Mth.clamp(point.subtract(from).dot(segment) / lengthSqr, 0.0D, 1.0D);
        return point.distanceTo(from.add(segment.scale(t)));
    }
}
