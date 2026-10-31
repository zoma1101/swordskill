package com.zoma1101.swordskill;

import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.client.handler.WeaponTrailHandler;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.Objects;
import java.util.Set;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class AnimationUtils {
    private static final Set<String> FIRST_PERSON_RENDER_MODS = Set.of("bettercombat", "epicfight", "firstperson");
    private static final FirstPersonConfiguration FIRST_PERSON_CONFIGURATION = new FirstPersonConfiguration()
            .setShowRightArm(true)
            .setShowLeftArm(true);

    //rightitemはZ軸で後ろY+いくらしい
    //Y軸でY- Z+ 方向に行くらしい
    //X軸で-X方向に行くらしい
    //手の方向そのままに剣配置　(0,3,2) -90

    /** 予測再生のエコーを取り消すまでの猶予。サーバーが発動を拒否した場合に永久に残らないようにする。 */
    private static final long ECHO_TIMEOUT_MILLIS = 500L;

    /** 予測再生したアニメーションの (スキルID, type)。 */
    private static int echoedSkillId = Integer.MIN_VALUE;
    private static String echoedType = null;
    private static long echoDeadline = 0L;

    private static boolean rushMoveActive = false;
    private static int rushMoveSkillId = 0;

    /**
     * クライアントが予測再生した直後にサーバーから届く「同じ再生指示」を 1 回だけ無視する。
     *
     * <p>発動時はクライアントが即座にアニメーションを再生し、サーバーも
     * {@code PlayAnimationPayload} を発動者自身へ送る。そのまま適用すると再生位置が 0 に
     * 巻き戻るため（ping が高いほど目立つ）、サーバー発のエコーだけを読み飛ばす。</p>
     */
    public static void expectServerEcho(int skillId, String type) {
        echoedSkillId = skillId;
        echoedType = type == null ? "" : type;
        echoDeadline = net.minecraft.Util.getMillis() + ECHO_TIMEOUT_MILLIS;
    }

    private static boolean consumeServerEcho(Player player, int skillId, String type) {
        if (echoedType == null) {
            return false;
        }
        if (net.minecraft.Util.getMillis() > echoDeadline) {
            echoedType = null;
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // 自分のエコーのみ対象（他人のアニメーションは通常どおり再生する）
        if (minecraft.player == null || player != minecraft.player) {
            return false;
        }
        if (skillId != echoedSkillId || !echoedType.equals(type == null ? "" : type)) {
            return false;
        }
        echoedType = null;
        return true;
    }

    public static boolean isRushMoveActive() { return rushMoveActive; }
    public static void setRushMoveActive(boolean v) { rushMoveActive = v; }
    public static int getRushMoveSkillId() { return rushMoveSkillId; }

    public static void PlayerAnim(Player player,int SkillID, String type){
        // skillId 0 は「アニメーション解除」の合図。ここで色とトレイル長まで戻すと、
        // 消えかけのトレイルが既定色（青）で描かれてしまうので触らない。
        if (SkillID <= 0) {
            if (player instanceof AbstractClientPlayer clientPlayer) {
                IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                        .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
                if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
                    animationLayer.setAnimation(null);
                }
            }
            rushMoveActive = false;
            rushMoveSkillId = 0;
            return;
        }
        rushMoveSkillId = SkillID;
        rushMoveActive = "move".equals(type);
        if (consumeServerEcho(player, SkillID, type)) {
            return;
        }
        var skill = SwordSkillRegistry.SKILLS.get(SkillID);
        if (skill == null) {
            return;
        }
        if (player instanceof AbstractClientPlayer clientPlayer) {
            if (skill != null && skill.getDataDefinition() != null) {
                WeaponTrailHandler.setSkillColor(clientPlayer.getId(), skill.getDataDefinition().color());
                WeaponTrailHandler.setTrailLength(clientPlayer.getId(), skill.getDataDefinition().trailLength());
            } else {
                WeaponTrailHandler.setSkillColor(clientPlayer.getId(), "#54D8FF");
                WeaponTrailHandler.clearTrailLength(clientPlayer.getId());
            }
        }
        ResourceLocation animationId;
        String AnimationName = skill != null && skill.getDataDefinition() != null
                ? skill.getDataDefinition().animation()
                : skill.getName();
        if (skill.getDataDefinition() != null && skill.getType() == SkillData.SkillType.RUSH) {
            var definition = skill.getDataDefinition();
            type = switch (type) {
                case "start" -> definition.rushStartAnimation();
                case "move" -> definition.rushMoveAnimation();
                case "finish" -> definition.rushFinishAnimation();
                default -> type;
            };
        }
        if (type.isEmpty()) {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName);
        } else {
            animationId = fromNamespaceAndPath(SwordSkill.MOD_ID, AnimationName + "." + type);
        }


        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayer) player)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));

        if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer
                && PlayerAnimationRegistry.getAnimation(animationId) != null) {
            KeyframeAnimationPlayer animationPlayer = new KeyframeAnimationPlayer(
                    (dev.kosmx.playerAnim.core.data.KeyframeAnimation) Objects.requireNonNull(PlayerAnimationRegistry.getAnimation(animationId)));
            animationPlayer.setFirstPersonMode(shouldUsePlayerAnimatorFirstPerson()
                    ? FirstPersonMode.THIRD_PERSON_MODEL
                    : FirstPersonMode.NONE);
            animationPlayer.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
            animationLayer.setAnimation(animationPlayer);
        }

    }

    /**
     * 鍔迫り合い用のアニメーションを再生する。スキル ID を経由せず、名前から直接解決する。
     * {@code animationName} は ClientConfig の {@code clashAnimation}（例: {@code clash_lock}）。
     */
    public static void playClashAnimation(Player player, String animationName) {
        if (!(player instanceof AbstractClientPlayer clientPlayer) || animationName == null) {
            return;
        }
        String name = animationName.trim();
        if (name.isEmpty()) {
            return;
        }
        ResourceLocation animationId = name.contains(":")
                ? ResourceLocation.tryParse(name)
                : fromNamespaceAndPath(SwordSkill.MOD_ID, name);
        if (animationId == null) {
            return;
        }
        var animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null) {
            return;
        }
        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
        if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
            // 鍔迫り合いは「その場で押し合う」姿勢なので、先頭フレームで固定して
            // ロック時間がアニメーション長を超えても姿勢が戻らないようにする。
            FrozenKeyframeAnimationPlayer animationPlayer = new FrozenKeyframeAnimationPlayer(
                    (dev.kosmx.playerAnim.core.data.KeyframeAnimation) animation);
            animationPlayer.setFirstPersonMode(shouldUsePlayerAnimatorFirstPerson()
                    ? FirstPersonMode.THIRD_PERSON_MODEL
                    : FirstPersonMode.NONE);
            animationPlayer.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
            animationLayer.setAnimation(animationPlayer);
        }
    }

    /** 鍔迫り合いのアニメーションだけを止める（トレイル色は触らない）。 */
    public static void stopClashAnimation(Player player) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
        if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
            animationLayer.setAnimation(null);
        }
    }

    public static void PlayerChargeAnim(Player player, int skillId, String type) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        FrozenKeyframeAnimationPlayer animationPlayer = getChargeAnimationPlayer(player, skillId, type);
        IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
        if (animationPlayer != null && playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
            animationLayer.setAnimation(animationPlayer);
        }
    }

    public static void StopPlayerAnimation(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer) {
            WeaponTrailHandler.clearTrailLength(clientPlayer.getId());
            IAnimation playerAnimation = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer)
                    .get(fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"));
            if (playerAnimation instanceof SwordSkillAnimationLayer animationLayer) {
                animationLayer.setAnimation(null);
            }
        }
    }

    private static FrozenKeyframeAnimationPlayer getChargeAnimationPlayer(Player player, int skillId, String type) {
        // skillId 0 はスキルなし。既定色で上書きしないようにする。
        if (skillId <= 0) {
            return null;
        }
        var skill = SwordSkillRegistry.SKILLS.get(skillId);
        if (skill == null || !(player instanceof AbstractClientPlayer clientPlayer)) {
            return null;
        }

        String animationName = skill.getDataDefinition() != null
                ? skill.getDataDefinition().animation()
                : skill.getName();
        if (skill.getDataDefinition() != null && skill.getType() == SkillData.SkillType.RUSH) {
            var definition = skill.getDataDefinition();
            type = switch (type) {
                case "start" -> definition.rushStartAnimation();
                case "move" -> definition.rushMoveAnimation();
                case "finish" -> definition.rushFinishAnimation();
                default -> type;
            };
        }
        ResourceLocation animationId = type.isEmpty()
                ? fromNamespaceAndPath(SwordSkill.MOD_ID, animationName)
                : fromNamespaceAndPath(SwordSkill.MOD_ID, animationName + "." + type);
        var animation = PlayerAnimationRegistry.getAnimation(animationId);
        if (animation == null) {
            return null;
        }

        FrozenKeyframeAnimationPlayer animationPlayer = new FrozenKeyframeAnimationPlayer(
                (dev.kosmx.playerAnim.core.data.KeyframeAnimation) animation);
        animationPlayer.setFirstPersonMode(shouldUsePlayerAnimatorFirstPerson()
                ? FirstPersonMode.THIRD_PERSON_MODEL
                : FirstPersonMode.NONE);
        animationPlayer.setFirstPersonConfiguration(FIRST_PERSON_CONFIGURATION);
        if (skill.getDataDefinition() != null) {
            WeaponTrailHandler.setSkillColor(clientPlayer.getId(), skill.getDataDefinition().color());
            WeaponTrailHandler.setTrailLength(clientPlayer.getId(), skill.getDataDefinition().trailLength());
        }
        return animationPlayer;
    }

    private static class FrozenKeyframeAnimationPlayer extends KeyframeAnimationPlayer {
        private FrozenKeyframeAnimationPlayer(dev.kosmx.playerAnim.core.data.KeyframeAnimation animation) {
            super(animation);
        }

        @Override
        public void tick() {
        }

        @Override
        public void setupAnim(float tickDelta) {
            super.setupAnim(0.0F);
        }
    }

    private static boolean shouldUsePlayerAnimatorFirstPerson() {
        return FIRST_PERSON_RENDER_MODS.stream().noneMatch(ModList.get()::isLoaded);
    }

    private static class SwordSkillAnimationLayer extends ModifierLayer<IAnimation> {
        @Override
        public FirstPersonMode getFirstPersonMode(float tickDelta) {
            return shouldUsePlayerAnimatorFirstPerson()
                    ? FirstPersonMode.THIRD_PERSON_MODEL
                    : super.getFirstPersonMode(tickDelta);
        }

        @Override
        public FirstPersonConfiguration getFirstPersonConfiguration(float tickDelta) {
            return shouldUsePlayerAnimatorFirstPerson()
                    ? FIRST_PERSON_CONFIGURATION
                    : super.getFirstPersonConfiguration(tickDelta);
        }
    }

    public static class AnimationRegister {
        public static void AnimationSetup() {
                //Set the player construct callback. It can be a lambda function.
                PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                        fromNamespaceAndPath(SwordSkill.MOD_ID, "animation"),
                        42,
                        AnimationRegister::registerPlayerAnimation);
        }
        private static IAnimation registerPlayerAnimation(AbstractClientPlayer player) {
            return new SwordSkillAnimationLayer();
        }
    }
}