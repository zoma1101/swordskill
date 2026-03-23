package com.zoma1101.swordskill.client.renderer.layer;

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
            session.lastOrigin = null; // スラスト・ブースト用の原点をリセット
            session.active = true;

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
                // JSONのanimation_lengthを優先したい場合は下記のコメントを外す
                // session.animationLength = animData.animationLength;
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
}
