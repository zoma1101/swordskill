package com.zoma1101.swordskill.entity.custom;

import com.zoma1101.swordskill.config.ServerConfig;
import com.zoma1101.swordskill.server.handler.SkillClashManager;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * 鍔迫り合い（ブレードロック）の実装テスト用マネキン。
 *
 * <p>プレイヤーと同じ体格・スキンで描画され、右クリックで「押し込み力」を切り替えられる。
 * スキル未設定時の {@code clashRatio} は相手プレイヤーの押し込み力に対する倍率で、
 * 1.0 なら常に同等（鍔迫り合い）、1.0 未満なら一方的に負け、1.0 超なら一方的に勝つ。
 * 0 にすると鍔迫り合いをせず通常どおりダメージが通る（通常攻撃の確認用）。</p>
 *
 * <p>{@code /swordskill mannequin skill <名前>} で常時使用スキルを設定すると、
 * そのスキルのダメージ倍率が押し込み力に含まれ、近くのスキル使用中プレイヤーへ
 * こちらからも鍔迫り合いを仕掛ける。設定したスキルのアニメーションと武器トレイルを
 * 定期的に再生するが、スキル自体のダメージやヒットボックスは発生しない。</p>
 *
 * <p>マルチプレイでも訓練用の置物として使えるが、AI は持たず攻撃もしない。</p>
 */
public class SkillMannequinEntity extends PathfinderMob {
    /** 押し込み力の倍率。0 以下は「鍔迫り合いしない」。 */
    public static final EntityDataAccessor<Float> DATA_CLASH_RATIO =
            SynchedEntityData.defineId(SkillMannequinEntity.class, EntityDataSerializers.FLOAT);
    /** 鍔迫り合い中に両腕を突き出すポーズを取るか（描画専用）。 */
    public static final EntityDataAccessor<Boolean> DATA_CLASH_POSE =
            SynchedEntityData.defineId(SkillMannequinEntity.class, EntityDataSerializers.BOOLEAN);
    /** 常時使用するソードスキル名。空文字なら「スキル未使用」。 */
    public static final EntityDataAccessor<String> DATA_SKILL_ID =
            SynchedEntityData.defineId(SkillMannequinEntity.class, EntityDataSerializers.STRING);
    /**
     * 直近にスキルを使用したサーバーゲーム時間。0 は未使用。
     * クライアントはこの値と {@code level.getGameTime()} の差からスキルアニメーションとトレイルを再生する。
     */
    public static final EntityDataAccessor<Integer> DATA_SWING_START =
            SynchedEntityData.defineId(SkillMannequinEntity.class, EntityDataSerializers.INT);

    /** 右クリックで切り替える倍率。0 は鍔迫り合い無効。 */
    private static final float[] RATIOS = {1.0F, 0.5F, 1.5F, 0.0F};
    private static final String[] RATIO_KEYS = {"draw", "weak", "strong", "off"};
    /** 見た目用の剣。インベントリには入らず、死亡時にもドロップしない。 */
    private static final ItemStack DISPLAY_SWORD = new ItemStack(Items.IRON_SWORD);
    /** 常時スキル使用時に、スキルを振る間隔（tick）。アニメーションが短いスキルでも最低これだけ空ける。 */
    private static final int MIN_SWING_INTERVAL = 20;
    /** 長いスキルでもテストしやすいよう、間隔の上限を設ける。 */
    private static final int MAX_SWING_INTERVAL = 60;
    private int nextSwingTick;

    public SkillMannequinEntity(EntityType<? extends SkillMannequinEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // プレイヤーの方を向くだけ。移動・攻撃はしない。
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 12.0F, 1.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLASH_RATIO, 1.0F);
        builder.define(DATA_CLASH_POSE, false);
        builder.define(DATA_SKILL_ID, "");
        builder.define(DATA_SWING_START, 0);
    }

    public float getClashRatio() {
        return this.entityData.get(DATA_CLASH_RATIO);
    }

    public void setClashRatio(float ratio) {
        this.entityData.set(DATA_CLASH_RATIO, Math.max(0.0F, ratio));
    }

    /** このマネキンが鍔迫り合いの相手として成立するか。 */
    public boolean isClashReady() {
        return getClashRatio() > 0.0F;
    }

    public boolean isClashPose() {
        return this.entityData.get(DATA_CLASH_POSE);
    }

    public void setClashPose(boolean pose) {
        this.entityData.set(DATA_CLASH_POSE, pose);
    }

    /** 常時使用するソードスキル名（空文字なら未設定）。 */
    public String getSkillId() {
        return this.entityData.get(DATA_SKILL_ID);
    }

    public void setSkillId(String skillId) {
        this.entityData.set(DATA_SKILL_ID, skillId == null ? "" : skillId);
        this.entityData.set(DATA_SWING_START, 0);
        this.nextSwingTick = 0;
    }

    /** ソードスキルを常時使用している扱いか（鍔迫り合いの判定に使う）。 */
    public boolean isUsingSkill() {
        return !getSkillId().isEmpty();
    }

    /** 直近のスキル使用開始時刻（サーバーゲーム時間）。0 は未使用。 */
    public int getSwingStart() {
        return this.entityData.get(DATA_SWING_START);
    }

    /** クライアント側のスキルアニメーション／トレイル用に、スキル使用開始を記録する。 */
    public void startSkillSwing() {
        this.entityData.set(DATA_SWING_START, (int) this.level().getGameTime());
    }

    @Override
    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int index = currentRatioIndex();
        index = (index + 1) % RATIOS.length;
        setClashRatio(RATIOS[index]);
        this.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.3F);
        Component mode = Component.translatable(
                "message.swordskill.skill_mannequin." + RATIO_KEYS[index],
                String.format("%.2f", RATIOS[index]));
        Component skill = isUsingSkill()
                ? Component.translatable("message.swordskill.skill_mannequin.skill", getSkillId())
                : Component.translatable("message.swordskill.skill_mannequin.no_skill");
        player.displayClientMessage(mode.copy().append(Component.literal(" / ")).append(skill), true);
        return InteractionResult.CONSUME;
    }

    private int currentRatioIndex() {
        float ratio = getClashRatio();
        for (int index = 0; index < RATIOS.length; index++) {
            if (Math.abs(RATIOS[index] - ratio) < 1.0E-4F) {
                return index;
            }
        }
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        // テスト中に壊れきらないよう、時間経過でゆっくり自己修復する。
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
        }
        // スキルを設定されているときは、定期的にスキルを振り、近くのプレイヤーへ鍔迫り合いを仕掛ける。
        if (this.isUsingSkill() && this.tickCount >= this.nextSwingTick
                && !SkillClashManager.isClashing(this.getUUID())) {
            this.nextSwingTick = this.tickCount + swingIntervalTicks();
            this.startSkillSwing();
            if (this.isClashReady()) {
                this.tryClashNearbyPlayer();
            }
        }
    }

    /** スキルアニメーションが終わる程度の間隔。長いスキルはその分だけ間を空ける。 */
    private int swingIntervalTicks() {
        int duration = 0;
        String skillId = getSkillId();
        for (SkillData skill : SwordSkillRegistry.SKILLS.values()) {
            if (skill.getName().equals(skillId)) {
                if (skill.getDataDefinition() != null) {
                    duration = skill.getDataDefinition().durationTicks();
                }
                break;
            }
        }
        return Math.max(MIN_SWING_INTERVAL, Math.min(MAX_SWING_INTERVAL, duration + 4));
    }

    /** 近くでスキルを使っているプレイヤーに鍔迫り合いを仕掛ける。 */
    private void tryClashNearbyPlayer() {
        double range = ServerConfig.clashRange.get();
        AABB area = this.getBoundingBox().inflate(range);
        for (Player player : this.level().getEntitiesOfClass(Player.class, area)) {
            SkillClashManager.tryStartClash(this, player);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public @NotNull ItemStack getMainHandItem() {
        return DISPLAY_SWORD;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ClashRatio", getClashRatio());
        tag.putString("SkillId", getSkillId());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ClashRatio")) {
            setClashRatio(tag.getFloat("ClashRatio"));
        }
        if (tag.contains("SkillId")) {
            setSkillId(tag.getString("SkillId"));
        }
    }
}
