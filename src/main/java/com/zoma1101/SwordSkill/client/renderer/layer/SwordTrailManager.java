package com.zoma1101.swordskill.client.renderer.layer;

import com.zoma1101.swordskill.SwordSkill;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordTrailManager {
    public static final Map<UUID, SwordTrailLayer.TrailSession> SESSIONS = new HashMap<>();

    // キャッシュ：同じアニメーション名のJSONを毎回読み込まないようにする
    private static final Map<String, AnimationKeyframeTrack.AnimationData> ANIM_CACHE = new HashMap<>();

    public static SwordTrailLayer.TrailSession getSession(UUID uuid) {
        return SESSIONS.computeIfAbsent(uuid, SwordTrailLayer.TrailSession::new);
    }

    public static void clear(UUID uuid) {
        SESSIONS.remove(uuid);
    }

    public static Collection<SwordTrailLayer.TrailSession> getAll() {
        return SESSIONS.values();
    }

    /**
     * リソースパックの再読み込み時などにキャッシュをクリアする。
     */
    public static void clearAnimCache() {
        ANIM_CACHE.clear();
    }

    public static void updateSkillSettings(UUID playerUUID, int skillId) {
        com.zoma1101.swordskill.swordskills.SkillData data = com.zoma1101.swordskill.swordskills.SwordSkillRegistry.SKILLS
                .get(skillId);
        if (data != null) {
            SwordTrailLayer.TrailSession session = getSession(playerUUID);
            session.color = data.getTrailColor();
            session.texture = data.getTrailTexture();
            session.maxPoints = data.getTrailMaxLength();
            session.trailBaseOffset = data.getTrailBaseOffset();
            session.trailTipOffset = data.getTrailTipOffset();
            session.arcAngle = data.getTrailArcAngle();
            session.pointCount = data.getTrailPointCount();
            session.animationName = data.getName();
            session.animationLength = data.getFinalTick() + 30;
            session.active = true;

            // 現在の装備状態を詳しくチェック
            net.minecraft.client.player.LocalPlayer clientPlayer = net.minecraft.client.Minecraft.getInstance().player;
            net.minecraft.world.entity.player.Player targetPlayer = null;
            if (clientPlayer != null && clientPlayer.getUUID().equals(playerUUID)) {
                targetPlayer = clientPlayer;
            } else if (net.minecraft.client.Minecraft.getInstance().level != null) {
                targetPlayer = net.minecraft.client.Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
            }

            if (targetPlayer != null) {
                net.minecraft.world.item.ItemStack mainStack = targetPlayer.getMainHandItem();
                net.minecraft.world.item.ItemStack offStack = targetPlayer.getOffhandItem();
                
                // 属性エンチャントによるトレイル色のオーバーライド
                int fireLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.ENMA_JIN.get());
                int waterLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.SUI_JIN.get());
                int windLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.FU_JIN.get());
                int holyLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.KO_JIN.get());
                int darkLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.AN_JIN.get());
                int soulLevel = mainStack.getEnchantmentLevel(com.zoma1101.swordskill.enchantment.ModEnchantments.KON_JIN.get());

                if (fireLevel > 0) session.color = 0xFFFE210C;
                else if (waterLevel > 0) session.color = 0xFF0690F8;
                else if (windLevel > 0) session.color = 0xFF27D480;
                else if (holyLevel > 0) session.color = 0xFFFFD700;
                else if (darkLevel > 0) session.color = 0xFF1A1A1A;
                else if (soulLevel > 0) session.color = 0xFF27D4C6;

                // 属性エンチャントがある場合はテクスチャを enchant_trail に変更
                if (fireLevel > 0 || waterLevel > 0 || windLevel > 0 || holyLevel > 0 || darkLevel > 0 || soulLevel > 0) {
                    session.texture = ResourceLocation.fromNamespaceAndPath(SwordSkill.MOD_ID, "textures/entity/enchant_trail.png");
                }

                java.util.Set<com.zoma1101.swordskill.swordskills.SkillData.WeaponType> mainTypes = 
                    com.zoma1101.swordskill.data.WeaponTypeDetector.detectWeaponTypes(mainStack);
                com.zoma1101.swordskill.data.WeaponTypeDetector.detectWeaponTypes(offStack);

                // 爪武器の追加トレイルフラグ判定
                session.isClaw = mainTypes.contains(com.zoma1101.swordskill.swordskills.SkillData.WeaponType.CLAW);

                boolean isMainWeapon = isWeapon(mainStack);
                boolean isOffWeapon = isWeapon(offStack);

                String mainItemName = mainStack.getDescriptionId();
                boolean hasDualItem = mainItemName.contains("dual") || mainItemName.contains("twin");
                
                // 二刀流判定: 厳格に現在の装備のみを基準にする
                session.isDual = (isMainWeapon && isOffWeapon) || hasDualItem;
            } else {
                session.isClaw = false;
                session.isDual = false;
            }

            // player_animation/<animationName>.json を読み込んでトラックを差し替える。
            // キャッシュ済みであれば再読み込みしない。
            // JSONが存在しない場合は TrailSession のデフォルトトラックをそのまま使用する。
            AnimationKeyframeTrack.AnimationData animData = loadAnimData(data.getName());
            if (animData != null) {
                session.armRotTrack = animData.armRotTrack();
                session.armPosTrack = animData.armPosTrack();
                session.bodyRotTrack = animData.bodyRotTrack();
                session.bodyPosTrack = animData.bodyPosTrack();
                session.itemRotTrack = animData.itemRotTrack();
                session.itemPosTrack = animData.itemPosTrack();
                session.trailTrack = animData.trailTrack();
                session.trailRotTrack = animData.trailRotTrack();
                session.trailScaleTrack = animData.trailScaleTrack();

                // 二刀流時は左手用のアニメーショントラックを設定
                if (session.isDual) {
                    // GeckoLibのJSONに left_arm / leftItem ボーンが含まれているか確認
                    if (!animData.leftArmRotTrack().isEmpty() || !animData.leftItemRotTrack().isEmpty()) {
                        session.leftArmRotTrack = animData.leftArmRotTrack();
                        session.leftArmPosTrack = animData.leftArmPosTrack();
                        session.leftItemRotTrack = animData.leftItemRotTrack();
                        session.leftItemPosTrack = animData.leftItemPosTrack();
                    } else {
                        // 含まれていない場合は右腕用を左右反転させて代用 (fallback)
                        AnimationKeyframeTrack.AnimationData leftData = animData.getMirrored();
                        session.leftArmRotTrack = leftData.armRotTrack();
                        session.leftArmPosTrack = leftData.armPosTrack();
                        session.leftItemRotTrack = leftData.itemRotTrack();
                        session.leftItemPosTrack = leftData.itemPosTrack();
                    }
                } else {
                    session.leftArmRotTrack = null;
                    session.leftArmPosTrack = null;
                    session.leftItemRotTrack = null;
                    session.leftItemPosTrack = null;
                }

                // JSONのanimation_lengthを優先（秒単位をチック単位に変換）
                session.animationLength = animData.animationLength() * 20.0f;
            } else {
                // アニメーションデータが見つからない場合はトラックをリセット
                session.armRotTrack = null;
                session.armPosTrack = null;
                session.bodyRotTrack = null;
                session.bodyPosTrack = null;
                session.itemRotTrack = null;
                session.itemPosTrack = null;
                session.trailTrack = null;
                session.trailRotTrack = null;
                session.trailScaleTrack = null;
                
                session.leftArmRotTrack = null;
                session.leftArmPosTrack = null;
                session.leftItemRotTrack = null;
                session.leftItemPosTrack = null;
            }

            // スキル発動時刻を記録（captureFirstPersonFromKeyframe が参照する）
            session.animStartMs = System.currentTimeMillis();
        }
    }

    /**
     * アニメーション名に対応する AnimationData をキャッシュ付きで取得する。
     * player_animation/<animationName>.json が存在しない場合は null を返す。
     */
    private static AnimationKeyframeTrack.AnimationData loadAnimData(String animationName) {
        // containsKey で null 番兵を管理し、存在しないJSONへの繰り返しアクセスを防ぐ
        if (ANIM_CACHE.containsKey(animationName)) {
            return ANIM_CACHE.get(animationName);
        }
        AnimationKeyframeTrack.AnimationData loaded = AnimationKeyframeTrack.AnimationData.load(animationName);
        ANIM_CACHE.put(animationName, loaded);
        return loaded;
    }
    private static boolean isWeapon(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        java.util.Set<com.zoma1101.swordskill.swordskills.SkillData.WeaponType> types = 
            com.zoma1101.swordskill.data.WeaponTypeDetector.detectWeaponTypes(stack);
        return types.contains(com.zoma1101.swordskill.swordskills.SkillData.WeaponType.ONE_HANDED_SWORD) || 
               types.contains(com.zoma1101.swordskill.swordskills.SkillData.WeaponType.TWO_HANDED_SWORD) || 
               types.contains(com.zoma1101.swordskill.swordskills.SkillData.WeaponType.CLAW);
    }
}
