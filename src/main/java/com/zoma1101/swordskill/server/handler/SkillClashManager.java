package com.zoma1101.swordskill.server.handler;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.entity.custom.AttackEffectEntity;
import com.zoma1101.swordskill.entity.custom.SkillMannequinEntity;
import com.zoma1101.swordskill.payload.SkillClashPayload;
import com.zoma1101.swordskill.swordskills.DataDrivenSkill;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SkillUtils;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ソードスキルの衝突と鍔迫り合い（ブレードロック）を管理する。
 *
 * <p>プレイヤー同士（またはテスト用マネキン）のスキル判定がぶつかったとき、
 * 押し込み力の差が {@code clashLockThreshold} 未満なら双方のスキルを中断して
 * {@code clashLockTicks} のあいだ停止させる。差がそれ以上ならロックせず、
 * 金属の火花だけ散らして強い側のスキルを通し、弱い側のスキルを中断する
 * （{@link #oneSidedWin}）。押し込み力は実ダメージ相当値
 * （基礎ダメージ × スキルのダメージ倍率 × チャージ倍率）で、ロック中の最終判定は
 * {@code clashDrawThreshold} 未満の差を引き分けとして自動で決める。
 * Mob や飛翔斬撃はロックせず、ダメージなしで相殺する。</p>
 *
 * <p>訓練用の {@link SkillMannequinEntity} は「スキル実行中」の代わりに
 * {@link SkillMannequinEntity#isClashReady()} を満たせば参加できる。スキル未設定なら
 * 押し込み力は相手プレイヤーの値をミラーするので 1.0 で常に同等（鍔迫り合い）になり、
 * スキルを設定するとそのスキルのダメージ倍率が掛かる。</p>
 */
public final class SkillClashManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final double BROADCAST_RADIUS = 64.0D;
    /** ロック中の位置ずれ許容値（0.02 ブロック）。これを超えたらサーバーから送り返す。 */
    private static final double FREEZE_TOLERANCE_SQR = 4.0E-4D;
    private static final int DEFAULT_COLOR = 0x54D8FF;
    /** 訓練用マネキンのゲージ色（プレイヤーのスキル色と区別する）。 */
    private static final int MANNEQUIN_COLOR = 0xD9C08A;
    /** 一方的な勝敗のあと、同じ相手と連続で判定しないための間隔（tick）。 */
    private static final int ONESIDED_COOLDOWN_TICKS = 20;

    private static final Map<UUID, Clash> BY_PARTICIPANT = new HashMap<>();
    private static final Map<Integer, Clash> BY_ID = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();
    private static int nextClashId = 1;

    private SkillClashManager() {
    }

    /** このプレイヤー／マネキンが現在鍔迫り合い中か。 */
    public static boolean isClashing(UUID participantId) {
        return BY_PARTICIPANT.containsKey(participantId);
    }

    /** 2 体が同じ鍔迫り合いに入っているか。 */
    public static boolean isSameClash(UUID first, UUID second) {
        Clash clash = BY_PARTICIPANT.get(first);
        return clash != null && (clash.uuidA.equals(second) || clash.uuidB.equals(second));
    }

    /**
     * スキル判定がぶつかったときに鍔迫り合いを試す。成立したら両者のスキルを中断してロックを開始する。
     *
     * <p>ダメージ差が {@code clashLockThreshold} 未満なら鍔迫り合い（ロック）に入り、
     * それ以上ならロックせず、金属の火花だけ散らして強い側の一方的な勝ちになる。</p>
     *
     * @return このヒットのダメージを打ち消すなら true（ロック開始、または攻撃側が一方的に負けた）
     */
    public static boolean tryStartClash(LivingEntity attacker, LivingEntity target) {
        if (!ServerConfig.clashEnabled.get() || attacker == target) {
            return false;
        }
        if (!(attacker.level() instanceof ServerLevel level) || target.level() != level) {
            return false;
        }
        if (attacker.isSpectator() || target.isSpectator()) {
            return false;
        }
        if (isClashing(attacker.getUUID()) || isClashing(target.getUUID())) {
            return false;
        }
        if (!isSkillActive(attacker) || !isSkillActive(target)) {
            return false;
        }

        long now = level.getGameTime();
        if (isOnCooldown(attacker.getUUID(), now) || isOnCooldown(target.getUUID(), now)) {
            return false;
        }

        SkillData skillA = runningSkill(attacker);
        SkillData skillB = runningSkill(target);
        if (!isClashable(attacker, skillA) || !isClashable(target, skillB)) {
            return false;
        }

        double range = ServerConfig.clashRange.get();
        if (attacker.distanceToSqr(target) > range * range) {
            return false;
        }
        if (!isFacing(attacker, target) || !isFacing(target, attacker)) {
            return false;
        }

        // マネキンは「相手の押し込み力をミラー」できるので、プレイヤー側を先に計算する
        double powerA;
        double powerB;
        if (attacker instanceof SkillMannequinEntity) {
            powerB = clashPower(target, attacker, skillB, 0.0D);
            powerA = clashPower(attacker, target, skillA, powerB);
        } else {
            powerA = clashPower(attacker, target, skillA, 0.0D);
            powerB = clashPower(target, attacker, skillB, powerA);
        }

        double highestPower = Math.max(powerA, powerB);
        double relativeDifference = highestPower <= 0.0D
                ? 0.0D
                : Math.abs(powerA - powerB) / highestPower;
        Vec3 midpoint = midpointOf(attacker, target);
        if (relativeDifference >= ServerConfig.clashLockThreshold.get()) {
            // ダメージ差が大きい: 鍔迫り合いにはならず、金属の火花だけ散って強い側の一方的な勝ち
            return oneSidedWin(level, attacker, target, powerA >= powerB,
                    colorOf(attacker, skillA), colorOf(target, skillB), midpoint);
        }

        int winner = decideWinner(powerA, powerB);
        int lockTicks = ServerConfig.clashLockTicks.get();
        Vec3 anchorA = attacker.position();
        Vec3 anchorB = target.position();
        Clash clash = new Clash(nextClashId++, attacker.getUUID(), target.getUUID(),
                level.dimension(),
                now + lockTicks, winner, colorOf(attacker, skillA), colorOf(target, skillB),
                midpoint.x, midpoint.y, midpoint.z, slotOf(attacker), slotOf(target),
                anchorA, anchorB);
        BY_PARTICIPANT.put(attacker.getUUID(), clash);
        BY_PARTICIPANT.put(target.getUUID(), clash);
        BY_ID.put(clash.id, clash);
        markPose(attacker, target);

        // 判定が出た瞬間に両者のスキルを止める。以降のヒットボックスは発生しない。
        if (attacker instanceof ServerPlayer player) {
            SkillExecutionManager.cancelRush(player);
        }
        if (target instanceof ServerPlayer player) {
            SkillExecutionManager.cancelRush(player);
        }
        freeze(attacker, clash.anchorA, clash.slotA);
        freeze(target, clash.anchorB, clash.slotB);

        broadcast(level, clash.x, clash.y, clash.z,
                new SkillClashPayload(clash.id, SkillClashPayload.PHASE_LOCK_START,
                        attacker.getId(), target.getId(), -1, lockTicks,
                        clash.colorA, clash.colorB, (float) clash.x, (float) clash.y, (float) clash.z));
        level.playSound(null, clash.x, clash.y, clash.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.75F);
        LOGGER.debug("Blade lock started: {} vs {} ({} ticks, winner={})",
                attacker.getName().getString(), target.getName().getString(), lockTicks, winner);
        return true;
    }

    /** 飛翔斬撃同士の相殺。両方を消して火花だけを周囲へ通知する。 */
    public static void cancelProjectiles(AttackEffectEntity first, AttackEffectEntity second) {
        if (!ServerConfig.clashEnabled.get() || !ServerConfig.clashCancelProjectiles.get()) {
            return;
        }
        if (first.isRemoved() || second.isRemoved()) {
            return;
        }
        if (!(first.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 midpoint = first.position().add(second.position()).scale(0.5D);
        int colorA = first.getSkillColor();
        int colorB = second.getSkillColor();
        first.discard();
        second.discard();
        broadcast(level, midpoint.x, midpoint.y, midpoint.z,
                new SkillClashPayload(0, SkillClashPayload.PHASE_SPARK, 0, 0, 0, 0,
                        colorA, colorB, (float) midpoint.x, (float) midpoint.y, (float) midpoint.z));
        level.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.25F);
    }

    /** サーバー tick。ロック中の参加者を固定し、時間切れで勝敗を確定する。 */
    public static void tick(MinecraftServer server) {
        if (BY_ID.isEmpty()) {
            return;
        }
        for (Clash clash : List.copyOf(BY_ID.values())) {
            ServerLevel level = server.getLevel(clash.dimension);
            if (level == null) {
                finish(clash, -1, server);
                continue;
            }
            LivingEntity participantA = participant(level, clash.uuidA);
            LivingEntity participantB = participant(level, clash.uuidB);
            if (participantA == null || participantB == null) {
                finish(clash, -1, server);
                continue;
            }
            freeze(participantA, clash.anchorA, clash.slotA);
            freeze(participantB, clash.anchorB, clash.slotB);
            if (level.getGameTime() >= clash.endTick) {
                finish(clash, clash.winner, server);
            }
        }
    }

    /** ログアウト・死亡時にロックを解除する。 */
    public static void forget(UUID participantId) {
        COOLDOWN_UNTIL.remove(participantId);
        Clash clash = BY_PARTICIPANT.get(participantId);
        if (clash == null) {
            return;
        }
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            remove(clash);
            return;
        }
        finish(clash, -1, server);
    }

    private static @Nullable LivingEntity participant(ServerLevel level, UUID participantId) {
        Entity entity = level.getEntity(participantId);
        if (!(entity instanceof LivingEntity living) || living.isRemoved() || !living.isAlive()
                || living.isSpectator()) {
            return null;
        }
        return living;
    }

    /** スキルを実行中か（マネキンは「鍔迫り合いを受け付ける」状態か）。 */
    private static boolean isSkillActive(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return SkillExecutionManager.isSkillActive(player);
        }
        if (entity instanceof SkillMannequinEntity mannequin) {
            return mannequin.isClashReady();
        }
        return false;
    }

    private static @Nullable SkillData runningSkill(LivingEntity entity) {
        if (entity instanceof ServerPlayer player && SkillExecutionManager.isSkillActive(player)) {
            return SwordSkillRegistry.SKILLS.get(SkillExecutionManager.getRunningSkillId(player));
        }
        if (entity instanceof SkillMannequinEntity mannequin) {
            return skillByName(mannequin.getSkillId());
        }
        return null;
    }

    /** スキル名から定義を引く（データパック再読み込み後も名前で追従する）。 */
    private static @Nullable SkillData skillByName(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return null;
        }
        for (SkillData skill : SwordSkillRegistry.SKILLS.values()) {
            if (skill.getName().equals(skillId)) {
                return skill;
            }
        }
        return null;
    }

    private static boolean isClashable(LivingEntity entity, @Nullable SkillData skill) {
        if (entity instanceof SkillMannequinEntity mannequin) {
            return mannequin.isClashReady();
        }
        return skill != null && skill.getDataDefinition() != null
                && !skill.getDataDefinition().hitboxes().isEmpty();
    }

    private static int decideWinner(double powerA, double powerB) {
        double highest = Math.max(powerA, powerB);
        if (highest <= 0.0D) {
            return -1;
        }
        double threshold = ServerConfig.clashDrawThreshold.get();
        if (Math.abs(powerA - powerB) / highest < threshold) {
            return -1;
        }
        return powerA > powerB ? 1 : 2;
    }

    /**
     * 自動判定に使う押し込み力。スキルのダメージ倍率・チャージ倍率を掛けた実ダメージ相当値。
     *
     * <p>訓練用マネキンにスキルが設定されていれば「相手プレイヤーの基礎ダメージ × そのスキル倍率 ×
     * マネキンの倍率」、未設定なら「相手プレイヤーの押し込み力 × マネキンの倍率」を使う。
     * 未設定時の 1.00 は常に相手と同等（＝鍔迫り合い）になる。</p>
     *
     * @param opponentPower 先に計算した相手の押し込み力（マネキンのミラー用）
     */
    private static double clashPower(LivingEntity entity, LivingEntity opponent, @Nullable SkillData skill,
            double opponentPower) {
        if (entity instanceof SkillMannequinEntity mannequin) {
            double ratio = mannequin.getClashRatio();
            if (!mannequin.isUsingSkill()) {
                return finiteOrZero(opponentPower * ratio);
            }
            if (!(opponent instanceof ServerPlayer player)) {
                return 0.0D;
            }
            return finiteOrZero(baseDamageOf(player) * skillDamageMultiplier(skill) * ratio);
        }
        if (!(entity instanceof ServerPlayer player)) {
            return 0.0D;
        }
        double charge = DataDrivenSkill.chargeMultiplierOf(player.getUUID());
        double power = playerSkillBase(player, skill) * skillDamageMultiplier(skill) * charge;
        double randomness = ServerConfig.clashRandomness.get();
        if (randomness > 0.0D && Double.isFinite(power)) {
            power *= 1.0D + (player.getRandom().nextDouble() * 2.0D - 1.0D) * randomness;
        }
        return finiteOrZero(power);
    }

    /** スキルの実ダメージ基礎値。RUSH ヒットボックスを持つスキルは移動速度込みの値を使う。 */
    private static double playerSkillBase(ServerPlayer player, @Nullable SkillData skill) {
        boolean rush = skill != null && skill.getDataDefinition() != null
                && skill.getDataDefinition().hitboxes().stream()
                        .anyMatch(hitbox -> "RUSH".equals(hitbox.damageType()));
        if (!rush) {
            return baseDamageOf(player);
        }
        try {
            return finiteOrZero(SkillUtils.RushDamage(player));
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to calculate rush clash power for {}: {}",
                    player.getName().getString(), exception.getMessage());
            return baseDamageOf(player);
        }
    }

    /** スキルのヒットボックスのうち最大のダメージ倍率。未定義なら 1.0。 */
    private static double skillDamageMultiplier(@Nullable SkillData skill) {
        if (skill == null || skill.getDataDefinition() == null) {
            return 1.0D;
        }
        double max = 0.0D;
        for (DataSkillDefinition.Hitbox hitbox : skill.getDataDefinition().hitboxes()) {
            max = Math.max(max, hitbox.damageMultiplier());
        }
        return max > 0.0D ? max : 1.0D;
    }

    /** 負けた側のスキルを中断する（マネキンは常時スキル扱いなので何もしない）。 */
    private static void cancelSkill(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            SkillExecutionManager.cancelRush(player);
        }
    }

    private static double baseDamageOf(ServerPlayer player) {
        try {
            return finiteOrZero(SkillUtils.BaseDamage(player));
        } catch (RuntimeException exception) {
            // 武器データが未同期などで算出できない場合は引き分け扱いにする
            LOGGER.warn("Failed to calculate clash power for {}: {}",
                    player.getName().getString(), exception.getMessage());
            return 0.0D;
        }
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    /**
     * ダメージ差が大きいときの一方的な勝敗。鍔迫り合いには入らず、火花だけを出して
     * 強い側のスキルをそのまま通し、弱い側のスキルを中断する。
     *
     * @return 呼び出し元のダメージ処理を打ち消すなら true（＝攻撃側が負けた）
     */
    private static boolean oneSidedWin(ServerLevel level, LivingEntity attacker, LivingEntity target,
            boolean attackerWins, int colorA, int colorB, Vec3 midpoint) {
        LivingEntity winner = attackerWins ? attacker : target;
        LivingEntity loser = attackerWins ? target : attacker;
        cancelSkill(loser);
        // 鍔迫り合いほどではないが、押し負けた側が弾かれる程度のノックバック
        pushApart(loser, winner, ServerConfig.clashKnockback.get() * 0.5D);
        // マネキンが負けた場合は「常時スキル」のままなので、連続で火花が出ないよう間隔を空ける
        COOLDOWN_UNTIL.put(loser.getUUID(), level.getGameTime() + ONESIDED_COOLDOWN_TICKS);
        broadcast(level, midpoint.x, midpoint.y, midpoint.z,
                new SkillClashPayload(0, SkillClashPayload.PHASE_SPARK, 0, 0, 0, 0,
                        colorA, colorB, (float) midpoint.x, (float) midpoint.y, (float) midpoint.z));
        level.playSound(null, midpoint.x, midpoint.y, midpoint.z,
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.45F);
        LOGGER.debug("Skill clash decided one-sided: winner={}, loser={}",
                winner.getName().getString(), loser.getName().getString());
        // 攻撃側が負けたときだけ、このヒット（ダメージ）を打ち消す
        return !attackerWins;
    }

    private static Vec3 midpointOf(LivingEntity first, LivingEntity second) {
        return first.position().add(second.position()).scale(0.5D)
                .add(0.0D, (first.getBbHeight() + second.getBbHeight()) * 0.5D, 0.0D);
    }

    private static boolean isOnCooldown(UUID participantId, long now) {
        Long until = COOLDOWN_UNTIL.get(participantId);
        return until != null && until > now;
    }

    /**
     * 相手の方向を向いているか（水平方向だけで判定）。
     * 訓練用マネキンはテストしやすいよう常に正対している扱いにする。
     */
    private static boolean isFacing(LivingEntity source, LivingEntity target) {
        if (source instanceof SkillMannequinEntity) {
            return true;
        }
        Vec3 toward = target.position().subtract(source.position());
        toward = new Vec3(toward.x, 0.0D, toward.z);
        Vec3 look = source.getLookAngle();
        look = new Vec3(look.x, 0.0D, look.z);
        if (toward.lengthSqr() < 1.0E-6D || look.lengthSqr() < 1.0E-6D) {
            return false;
        }
        double halfAngle = ServerConfig.clashFacingDegrees.get() * 0.5D;
        return look.normalize().dot(toward.normalize()) >= Math.cos(Math.toRadians(halfAngle));
    }

    private static int colorOf(LivingEntity entity, @Nullable SkillData skill) {
        if (skill != null && skill.getDataDefinition() != null) {
            return DataDrivenSkill.parseColor(skill.getDataDefinition().color());
        }
        return entity instanceof SkillMannequinEntity ? MANNEQUIN_COLOR : DEFAULT_COLOR;
    }

    /** ホットバースロット（プレイヤー以外は -1）。 */
    private static int slotOf(LivingEntity entity) {
        return entity instanceof ServerPlayer player ? player.getInventory().selected : -1;
    }

    private static void freeze(LivingEntity entity, Vec3 anchor, int slot) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
        // 位置そのものをサーバー側で固定する。速度を 0 にするだけではクライアントの
        // 移動パケットで抜けられてしまうため、アンカーから離れたら送り返す。
        // （テレポート確認が返るまでの移動はサーバーに無視される）
        if (entity.position().distanceToSqr(anchor) > FREEZE_TOLERANCE_SQR) {
            if (entity instanceof ServerPlayer player) {
                player.connection.teleport(anchor.x, anchor.y, anchor.z,
                        player.getYRot(), player.getXRot());
            } else {
                entity.teleportTo(anchor.x, anchor.y, anchor.z);
            }
            entity.setDeltaMovement(Vec3.ZERO);
        }
        if (slot >= 0 && entity instanceof ServerPlayer player && player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
            player.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
    }

    private static void markPose(LivingEntity first, LivingEntity second) {
        if (first instanceof SkillMannequinEntity mannequin) {
            mannequin.setClashPose(true);
        }
        if (second instanceof SkillMannequinEntity mannequin) {
            mannequin.setClashPose(true);
        }
    }

    private static void clearPose(@Nullable LivingEntity entity) {
        if (entity instanceof SkillMannequinEntity mannequin) {
            mannequin.setClashPose(false);
        }
    }

    private static void finish(Clash clash, int winner, MinecraftServer server) {
        remove(clash);
        ServerLevel level = server.getLevel(clash.dimension);
        if (level != null) {
            long cooldownUntil = level.getGameTime() + ServerConfig.clashCooldownTicks.get();
            COOLDOWN_UNTIL.put(clash.uuidA, cooldownUntil);
            COOLDOWN_UNTIL.put(clash.uuidB, cooldownUntil);
        }

        LivingEntity participantA = level == null ? null : participant(level, clash.uuidA);
        LivingEntity participantB = level == null ? null : participant(level, clash.uuidB);
        clearPose(participantA);
        clearPose(participantB);

        if (participantA == null || participantB == null) {
            // 片方がログアウト・死亡した場合は勝敗を付けずに終了通知だけ送る
            if (level != null) {
                broadcast(level, clash.x, clash.y, clash.z, endPayload(clash, 0, 0, -1));
            }
            return;
        }

        int winnerId = winner == 1 ? participantA.getId() : winner == 2 ? participantB.getId() : -1;
        double knockback = ServerConfig.clashKnockback.get();
        int stunTicks = ServerConfig.clashStunTicks.get();
        if (winnerId < 0) {
            pushApart(participantA, participantB, knockback * 0.7D);
            pushApart(participantB, participantA, knockback * 0.7D);
            stun(participantA, stunTicks);
            stun(participantB, stunTicks);
        } else {
            LivingEntity winnerEntity = winner == 1 ? participantA : participantB;
            LivingEntity loserEntity = winner == 1 ? participantB : participantA;
            pushApart(loserEntity, winnerEntity, knockback);
            stun(loserEntity, stunTicks);
            winnerEntity.setDeltaMovement(Vec3.ZERO);
            winnerEntity.invulnerableTime = Math.max(winnerEntity.invulnerableTime, 12);
        }

        if (level != null) {
            broadcast(level, clash.x, clash.y, clash.z,
                    endPayload(clash, participantA.getId(), participantB.getId(), winnerId));
            level.playSound(null, clash.x, clash.y, clash.z,
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9F, 1.35F);
        }
        LOGGER.debug("Blade lock resolved: winner={}", winnerId);
    }

    private static SkillClashPayload endPayload(Clash clash, int entityA, int entityB, int winnerId) {
        return new SkillClashPayload(clash.id, SkillClashPayload.PHASE_LOCK_END,
                entityA, entityB, winnerId, 0, clash.colorA, clash.colorB,
                (float) clash.x, (float) clash.y, (float) clash.z);
    }

    /** {@code loser} を {@code winner} から遠ざける。 */
    private static void pushApart(LivingEntity loser, LivingEntity winner, double strength) {
        Vec3 direction = loser.position().subtract(winner.position());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        loser.setDeltaMovement(loser.getDeltaMovement().add(direction.scale(strength)).add(0.0D, 0.35D, 0.0D));
        loser.hurtMarked = true;
    }

    private static void stun(LivingEntity entity, int ticks) {
        if (ticks <= 0) {
            return;
        }
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, true));
    }

    private static void remove(Clash clash) {
        BY_ID.remove(clash.id);
        BY_PARTICIPANT.remove(clash.uuidA, clash);
        BY_PARTICIPANT.remove(clash.uuidB, clash);
    }

    private static void broadcast(ServerLevel level, double x, double y, double z, SkillClashPayload payload) {
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (viewer.level() != level) {
                continue;
            }
            if (viewer.distanceToSqr(x, y, z) <= BROADCAST_RADIUS * BROADCAST_RADIUS) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }

    private static final class Clash {
        final int id;
        final UUID uuidA;
        final UUID uuidB;
        final ResourceKey<Level> dimension;
        final long endTick;
        final int winner;
        final int colorA;
        final int colorB;
        final double x;
        final double y;
        final double z;
        final int slotA;
        final int slotB;
        final Vec3 anchorA;
        final Vec3 anchorB;

        Clash(int id, UUID uuidA, UUID uuidB, ResourceKey<Level> dimension,
                long endTick, int winner, int colorA, int colorB,
                double x, double y, double z, int slotA, int slotB, Vec3 anchorA, Vec3 anchorB) {
            this.id = id;
            this.uuidA = uuidA;
            this.uuidB = uuidB;
            this.dimension = dimension;
            this.endTick = endTick;
            this.winner = winner;
            this.colorA = colorA;
            this.colorB = colorB;
            this.x = x;
            this.y = y;
            this.z = z;
            this.slotA = slotA;
            this.slotB = slotB;
            this.anchorA = anchorA;
            this.anchorB = anchorB;
        }
    }
}
