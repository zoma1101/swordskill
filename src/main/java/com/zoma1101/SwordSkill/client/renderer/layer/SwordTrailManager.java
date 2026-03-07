package com.zoma1101.swordskill.client.renderer.layer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwordTrailManager {
    public static final Map<UUID, SwordTrailLayer.TrailSession> SESSIONS = new HashMap<>();

    public static SwordTrailLayer.TrailSession getSession(UUID uuid) {
        return SESSIONS.computeIfAbsent(uuid, SwordTrailLayer.TrailSession::new);
    }

    public static void clear(UUID uuid) {
        SESSIONS.remove(uuid);
    }

    public static Collection<SwordTrailLayer.TrailSession> getAll() {
        return SESSIONS.values();
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
            session.animationName = data.getName();
        }
    }
}
