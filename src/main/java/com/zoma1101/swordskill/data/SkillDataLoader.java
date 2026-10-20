package com.zoma1101.swordskill.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.zoma1101.swordskill.proficiency.ProficiencyUnlockTable;
import com.zoma1101.swordskill.swordskills.DataSkillDefinition;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SkillHitEffectRegistry;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SkillDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static volatile boolean syncPending;

    public SkillDataLoader() {
        super(GSON, "skilldata");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler) {
        List<DataSkillDefinition> definitions = new ArrayList<>();
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        LOGGER.warn("Skipping non-object skilldata file {}: expected a JSON object", entry.getKey());
                        return;
                    }
                    try {
                        definitions.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(),
                                entry.getKey().toString())));
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to load skill data {}: {}", entry.getKey(), exception.getMessage());
                    }
                });
        try {
            ProficiencyUnlockTable.reload(resourceManager);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to load proficiency unlock tables: {}", exception.getMessage());
        }
        List<DataSkillDefinition> effective = ProficiencyUnlockTable.apply(definitions);
        try {
            SwordSkillRegistry.replaceDataDrivenSkills(effective);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid data-driven skill ID sequence; no data-driven skills were loaded: {}",
                    exception.getMessage());
            SwordSkillRegistry.replaceDataDrivenSkills(List.of());
        }
        syncPending = true;
        LOGGER.info("Loaded {} data-driven SwordSkill definitions",
                SwordSkillRegistry.getDataDrivenDefinitions().size());
    }

    public static String toSyncJson(DataSkillDefinition definition) {
        return GSON.toJson(definition);
    }

    public static boolean consumeSyncPending() {
        boolean pending = syncPending;
        syncPending = false;
        return pending;
    }

    public static List<DataSkillDefinition> parseSyncJson(String json) {
        DataSkillDefinition[] definitions = GSON.fromJson(json, DataSkillDefinition[].class);
        return definitions == null ? List.of() : List.of(definitions);
    }

    private static DataSkillDefinition parse(ResourceLocation resourceId, JsonObject json) {
        int version = GsonHelper.getAsInt(json, "schema_version", 1);
        if (version != 1) {
            throw new JsonParseException("Unsupported schema_version: " + version);
        }
        int id = GsonHelper.getAsInt(json, "id");
        boolean replaceBuiltin = GsonHelper.getAsBoolean(json, "replace_builtin", false);
        if (id < 0) {
            throw new JsonParseException("id must be 0 (auto-assign) or a positive skill ID");
        }
        String name = GsonHelper.getAsString(json, "name", resourceId.getPath());
        if (!name.matches("[a-z0-9_]+")) {
            throw new JsonParseException("name must use lowercase letters, digits, and underscores");
        }
        int cooldown = GsonHelper.getAsInt(json, "cooldown", 0);
        double spCost = GsonHelper.getAsDouble(json, "sp_cost", 0);
        int duration = GsonHelper.getAsInt(json, "duration_ticks", 1);
        if (cooldown < 0 || spCost < 0 || duration < 1 || duration > 1200) {
            throw new JsonParseException("cooldown/sp_cost/duration_ticks is outside the supported range");
        }

        List<SkillData.WeaponType> weapons = new ArrayList<>();
        JsonArray weaponValues = GsonHelper.getAsJsonArray(json, "weapons");
        for (JsonElement element : weaponValues) {
            try {
                SkillData.WeaponType weapon = SkillData.WeaponType.valueOf(element.getAsString());
                if (weapon == SkillData.WeaponType.NONE) {
                    throw new JsonParseException("NONE is not a valid weapon type");
                }
                weapons.add(weapon);
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Unknown weapon type: " + element.getAsString(), exception);
            }
        }
        if (weapons.isEmpty()) {
            throw new JsonParseException("weapons must contain at least one weapon type");
        }

        SkillData.SkillType type;
        try {
            type = SkillData.SkillType.valueOf(GsonHelper.getAsString(json, "type", "SIMPLE"));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown skill type", exception);
        }
        DataSkillDefinition.Motion motion = parseMotion(GsonHelper.getAsJsonObject(json, "motion",
                new JsonObject()));
        List<DataSkillDefinition.MotionEvent> motions = parseMotions(
                GsonHelper.getAsJsonArray(json, "motions", new JsonArray()), duration);
        List<DataSkillDefinition.Hitbox> hitboxes = parseHitboxes(GsonHelper.getAsJsonArray(json, "hitboxes"),
                duration);
        List<DataSkillDefinition.ProjectileEvent> projectiles = parseProjectiles(
                GsonHelper.getAsJsonArray(json, "projectiles", new JsonArray()), duration);
        List<DataSkillDefinition.SoundEvent> sounds = parseSounds(
                GsonHelper.getAsJsonArray(json, "sounds", new JsonArray()), duration);
        List<DataSkillDefinition.AnimationEvent> animations = parseAnimations(
                GsonHelper.getAsJsonArray(json, "animations", new JsonArray()), duration);
        List<DataSkillDefinition.PlayerEffectEvent> playerEffects = parsePlayerEffects(
                GsonHelper.getAsJsonArray(json, "player_effects", new JsonArray()), duration);
        boolean legacyFallback = GsonHelper.getAsBoolean(json, "legacy_fallback", false);
        if (legacyFallback) {
            throw new JsonParseException("legacy_fallback is no longer supported; describe the skill with JSON events");
        }

        String color = GsonHelper.getAsString(json, "color", "#54D8FF");
        if (!color.matches("#[0-9a-fA-F]{6}")) {
            throw new JsonParseException("color must be a #RRGGBB value");
        }
        int rushStartTicks = GsonHelper.getAsInt(json, "rush_start_ticks", 5);
        if (type == SkillData.SkillType.RUSH && (rushStartTicks < 0 || rushStartTicks > duration)) {
            throw new JsonParseException("rush_start_ticks must be between 0 and duration_ticks");
        }
        DataSkillDefinition.RushCombo rushCombo = parseRushCombo(
                json.has("rush_combo") ? GsonHelper.getAsJsonObject(json, "rush_combo") : null, type, duration);
        double trailLength = GsonHelper.getAsDouble(json, "trail_length", 1.5);
        int shockwaveTicks = GsonHelper.getAsInt(json, "shockwave_ticks", 0);
        int hitstopTicks = GsonHelper.getAsInt(json, "hitstop_ticks", -1);
        if (!Double.isFinite(trailLength) || trailLength <= 0 || trailLength > 32
                || shockwaveTicks < 0 || shockwaveTicks > 1200
                || hitstopTicks < -1 || hitstopTicks > 40) {
            throw new JsonParseException("trail_length/shockwave_ticks/hitstop_ticks is outside the supported range");
        }

        // --- 熟練度 -----------------------------------------------------
        SkillData.WeaponType proficiencyCategory = null;
        if (json.has("proficiency_category")) {
            String categoryName = GsonHelper.getAsString(json, "proficiency_category").trim();
            try {
                SkillData.WeaponType category = SkillData.WeaponType.valueOf(categoryName);
                if (category == SkillData.WeaponType.NONE) {
                    throw new JsonParseException("NONE is not a valid proficiency category");
                }
                proficiencyCategory = category;
            } catch (IllegalArgumentException exception) {
                throw new JsonParseException("Unknown proficiency category: " + categoryName, exception);
            }
        }
        int unlockProficiency = GsonHelper.getAsInt(json, "unlock_proficiency", 0);
        if (unlockProficiency < 0 || unlockProficiency > 1000000) {
            throw new JsonParseException("unlock_proficiency is outside the supported range");
        }
        boolean assignable = GsonHelper.getAsBoolean(json, "assignable", true);
        int invulnerabilityTicks = GsonHelper.getAsInt(json, "invulnerability_ticks", 0);
        if (invulnerabilityTicks < 0 || invulnerabilityTicks > 1200) {
            throw new JsonParseException("invulnerability_ticks is outside the supported range");
        }

        return new DataSkillDefinition(id, name, cooldown, spCost, type, weapons,
                GsonHelper.getAsBoolean(json, "hidden", false), duration,
                GsonHelper.getAsInt(json, "transform_limit_ticks", 20),
                GsonHelper.getAsString(json, "animation", name), color, motion, motions, hitboxes, projectiles,
                sounds, replaceBuiltin,
                rushStartTicks,
                GsonHelper.getAsString(json, "rush_start_animation", "start"),
                GsonHelper.getAsString(json, "rush_move_animation", "move"),
                GsonHelper.getAsString(json, "rush_finish_animation", "finish"), rushCombo,
                trailLength, shockwaveTicks, hitstopTicks,
                proficiencyCategory, unlockProficiency, assignable,
                animations, playerEffects, invulnerabilityTicks,
                GsonHelper.getAsBoolean(json, "end_skill_on_ground", false),
                GsonHelper.getAsBoolean(json, "default_unlocked", false));
    }

    private static DataSkillDefinition.RushCombo parseRushCombo(JsonObject json, SkillData.SkillType type,
            int duration) {
        if (json == null) return null;
        if (type != SkillData.SkillType.RUSH) {
            throw new JsonParseException("rush_combo is only valid for RUSH skills");
        }
        JsonObject startMotionJson = GsonHelper.getAsJsonObject(json, "start_motion", new JsonObject());
        DataSkillDefinition.Motion startMotion = parseMotion(startMotionJson);
        double pitchLimit = GsonHelper.getAsDouble(startMotionJson, "pitch_limit_degrees", 90);
        boolean useChargeMultiplier = GsonHelper.getAsBoolean(json, "use_charge_multiplier", true);
        int invulnerabilityTicks = GsonHelper.getAsInt(json, "invulnerability_ticks", 0);
        double searchRadius = GsonHelper.getAsDouble(json, "contact_search_radius", 8);
        double contactRadius = GsonHelper.getAsDouble(json, "contact_radius", 1.5);
        JsonObject contactMotion = GsonHelper.getAsJsonObject(json, "contact_motion", new JsonObject());
        double contactForward = GsonHelper.getAsDouble(contactMotion, "forward", -0.5);
        int latestContactTick = GsonHelper.getAsInt(json, "latest_contact_tick", duration);
        int timeoutTick = GsonHelper.getAsInt(json, "no_contact_timeout_tick", duration + 1);
        int contactAnimationOffset = GsonHelper.getAsInt(json, "contact_animation_offset", 1);
        int finishContactOffset = GsonHelper.getAsInt(json, "finish_contact_offset", duration);
        if (!Double.isFinite(pitchLimit) || pitchLimit < 0 || pitchLimit > 90
                || invulnerabilityTicks < 0 || invulnerabilityTicks > 1200
                || !Double.isFinite(searchRadius) || searchRadius <= 0 || searchRadius > 64
                || !Double.isFinite(contactRadius) || contactRadius <= 0 || contactRadius > searchRadius
                || !Double.isFinite(contactForward) || Math.abs(contactForward) > 16
                || latestContactTick < 1 || latestContactTick > duration
                || timeoutTick < 1 || timeoutTick > duration + 1
                || contactAnimationOffset < 0 || contactAnimationOffset > duration
                || finishContactOffset < contactAnimationOffset || finishContactOffset > duration
                || latestContactTick + finishContactOffset > duration) {
            throw new JsonParseException("rush_combo fields are outside the supported range");
        }
        return new DataSkillDefinition.RushCombo(startMotion, pitchLimit, useChargeMultiplier,
                invulnerabilityTicks, searchRadius,
                contactRadius, contactForward, latestContactTick, timeoutTick,
                contactAnimationOffset, finishContactOffset);
    }

    private static DataSkillDefinition.Motion parseMotion(JsonObject json) {
        return new DataSkillDefinition.Motion(
                GsonHelper.getAsDouble(json, "forward", 0),
                GsonHelper.getAsDouble(json, "strafe", 0),
                GsonHelper.getAsDouble(json, "vertical", 0));
    }

    private static List<DataSkillDefinition.MotionEvent> parseMotions(JsonArray json, int duration) {
        List<DataSkillDefinition.MotionEvent> motions = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject motion = GsonHelper.convertToJsonObject(element, "motion");
            int tick = GsonHelper.getAsInt(motion, "tick");
            double forward = GsonHelper.getAsDouble(motion, "forward", 0);
            double strafe = GsonHelper.getAsDouble(motion, "strafe", 0);
            double vertical = GsonHelper.getAsDouble(motion, "vertical", 0);
            if (tick < 0 || tick > duration || !Double.isFinite(forward)
                    || !Double.isFinite(strafe) || !Double.isFinite(vertical)) {
                throw new JsonParseException("motion tick or vector is outside the supported range");
            }
            motions.add(new DataSkillDefinition.MotionEvent(tick, forward, strafe, vertical,
                    GsonHelper.getAsBoolean(motion, "set", false),
                    GsonHelper.getAsBoolean(motion, "repeat", false),
                    parseEventTrigger(motion)));
        }
        return motions;
    }

    private static List<DataSkillDefinition.Hitbox> parseHitboxes(JsonArray json, int duration) {
        List<DataSkillDefinition.Hitbox> hitboxes = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject hitbox = GsonHelper.convertToJsonObject(element, "hitbox");
            int tick = GsonHelper.getAsInt(hitbox, "tick");
            double[] offset = readVector(GsonHelper.getAsJsonArray(hitbox, "offset"), "offset");
            double[] size = readVector(GsonHelper.getAsJsonArray(hitbox, "size"), "size");
            if (tick < 0 || tick > duration || size[0] <= 0 || size[1] <= 0 || size[2] <= 0
                    || size[0] > 32 || size[1] > 32 || size[2] > 32) {
                throw new JsonParseException("hitbox tick/size is outside the supported range");
            }
            List<DataSkillDefinition.Effect> effects = parseEffects(
                    GsonHelper.getAsJsonArray(hitbox, "effects", new JsonArray()));
            double multiplier = GsonHelper.getAsDouble(hitbox, "damage_multiplier", 1);
            String damageType = GsonHelper.getAsString(hitbox, "damage_type", "BASE");
            double knockback = GsonHelper.getAsDouble(hitbox, "knockback", 0);
            double knockbackMultiplier = GsonHelper.getAsDouble(hitbox, "knockback_multiplier", -1);
            if (!Double.isFinite(multiplier) || multiplier < 0 || !Double.isFinite(knockback)
                    || Math.abs(knockback) > 16
                    || !Double.isFinite(knockbackMultiplier)
                    || (knockbackMultiplier != -1 && Math.abs(knockbackMultiplier) > 16)
                    || !(damageType.equals("BASE") || damageType.equals("RUSH"))) {
                throw new JsonParseException("hitbox damage or knockback values are invalid");
            }
            String sound = GsonHelper.getAsString(hitbox, "sound", "").trim();
            String trigger = parseEventTrigger(hitbox);
            if (!sound.isEmpty()) {
                ResourceLocation soundId = ResourceLocation.tryParse(sound);
                if (soundId == null || !BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                    throw new JsonParseException("Unknown attack sound: " + sound);
                }
            }
            hitboxes.add(new DataSkillDefinition.Hitbox(tick, offset, size, multiplier, damageType, knockback,
                    knockbackMultiplier, GsonHelper.getAsBoolean(hitbox, "repeat_while_rushing", false), effects,
                    GsonHelper.getAsBoolean(hitbox, "hitbox_enabled", true), sound, trigger));
        }
        return hitboxes;
    }

    private static List<DataSkillDefinition.ProjectileEvent> parseProjectiles(JsonArray json, int duration) {
        List<DataSkillDefinition.ProjectileEvent> projectiles = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject projectile = GsonHelper.convertToJsonObject(element, "projectile");
            int tick = GsonHelper.getAsInt(projectile, "tick");
            double[] offset = readVector(GsonHelper.getAsJsonArray(projectile, "offset"), "offset");
            JsonArray rotationJson = GsonHelper.getAsJsonArray(projectile, "rotation", new JsonArray());
            if (rotationJson.isEmpty()) {
                rotationJson.add(0);
                rotationJson.add(0);
                rotationJson.add(0);
            }
            double[] rotation = readVector(rotationJson, "rotation");
            double playerPitchXMultiplier = GsonHelper.getAsDouble(
                    projectile, "player_pitch_x_multiplier", 0);
            double playerPitchZMultiplier = GsonHelper.getAsDouble(
                    projectile, "player_pitch_z_multiplier", 0);
            double renderPitch = GsonHelper.getAsDouble(projectile, "render_pitch", 0);
            boolean horizontal = GsonHelper.getAsBoolean(projectile, "horizontal", false);
            double[] size = readVector(GsonHelper.getAsJsonArray(projectile, "size"), "size");
            double damageMultiplier = GsonHelper.getAsDouble(projectile, "damage_multiplier", 1);
            String damageType = GsonHelper.getAsString(projectile, "damage_type", "BASE");
            double knockback = GsonHelper.getAsDouble(projectile, "knockback", 0);
            double knockbackMultiplier = GsonHelper.getAsDouble(projectile, "knockback_multiplier", -1);
            int projectileDuration = GsonHelper.getAsInt(projectile, "duration_ticks", 12);
            double travelDistance = GsonHelper.getAsDouble(projectile, "travel_distance", 16);
            double[] movement = projectile.has("movement")
                    ? readVector(GsonHelper.getAsJsonArray(projectile, "movement"), "movement") : null;
            String particle = GsonHelper.getAsString(projectile, "particle", "skill_particle_sword/blue").trim();
            String sound = GsonHelper.getAsString(projectile, "sound", "").trim();
            String trigger = parseEventTrigger(projectile);
            if (tick < 0 || tick > duration || size[0] <= 0 || size[1] <= 0 || size[2] <= 0
                    || size[0] > 32 || size[1] > 32 || size[2] > 32
                    || !allFinite(offset) || !allFinite(rotation)
                    || !Double.isFinite(playerPitchXMultiplier) || Math.abs(playerPitchXMultiplier) > 4
                    || !Double.isFinite(playerPitchZMultiplier) || Math.abs(playerPitchZMultiplier) > 4
                    || !Double.isFinite(renderPitch)
                    || renderPitch < -180 || renderPitch > 180 || !allFinite(size)
                    || damageMultiplier < 0 || !Double.isFinite(damageMultiplier)
                    || (!damageType.equals("BASE") && !damageType.equals("RUSH"))
                    || Math.abs(knockback) > 16 || !Double.isFinite(knockback)
                    || (knockbackMultiplier != -1 && Math.abs(knockbackMultiplier) > 16)
                    || !Double.isFinite(knockbackMultiplier)
                    || projectileDuration < 1 || projectileDuration > 1200
                    || travelDistance < 0 || travelDistance > 128 || !Double.isFinite(travelDistance)
                    || (movement != null && (!allFinite(movement)
                            || Math.abs(movement[0]) > 128 || Math.abs(movement[1]) > 128
                            || Math.abs(movement[2]) > 128))
                    || particle.isEmpty() || particle.length() > 128) {
                throw new JsonParseException("projectile fields are outside the supported range");
            }
            if (!sound.isEmpty()) {
                ResourceLocation soundId = ResourceLocation.tryParse(sound);
                if (soundId == null || !BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                    throw new JsonParseException("Unknown projectile sound: " + sound);
                }
            }
            double fallDamageScale = GsonHelper.getAsDouble(projectile, "fall_damage_scale", 0);
            if (!Double.isFinite(fallDamageScale) || fallDamageScale < 0 || fallDamageScale > 16) {
                throw new JsonParseException("fall_damage_scale is outside the supported range");
            }
            DataSkillDefinition.PathSpec path = projectile.has("path")
                    ? parsePath(GsonHelper.getAsJsonObject(projectile, "path"), duration)
                    : null;
            String randomGroup = GsonHelper.getAsString(projectile, "random_group", "").trim();
            int randomIndex = GsonHelper.getAsInt(projectile, "random_index", 0);
            if (randomIndex < 0 || randomIndex > 32) {
                throw new JsonParseException("random_index is outside the supported range");
            }
            boolean singleTarget = GsonHelper.getAsBoolean(projectile, "single_target", false);
            boolean aimAtContact = GsonHelper.getAsBoolean(projectile, "aim_at_contact", false);
            if (aimAtContact && !trigger.equals("CONTACT")) {
                throw new JsonParseException("aim_at_contact is only valid for CONTACT projectiles");
            }
            projectiles.add(new DataSkillDefinition.ProjectileEvent(tick, offset, rotation,
                    playerPitchXMultiplier, playerPitchZMultiplier, renderPitch, horizontal, size,
                    damageMultiplier, damageType, knockback, knockbackMultiplier,
                    projectileDuration, particle, travelDistance,
                    movement, sound, trigger, fallDamageScale, path, randomGroup, randomIndex,
                    singleTarget, aimAtContact));
        }
        return projectiles;
    }

    private static DataSkillDefinition.PathSpec parsePath(JsonObject json, int duration) {
        int startTick = GsonHelper.getAsInt(json, "start_tick", 1);
        double position = GsonHelper.getAsDouble(json, "position", 0);
        boolean rotation = GsonHelper.getAsBoolean(json, "rotation", false);
        double sizeScale = GsonHelper.getAsDouble(json, "size_scale", 0);
        if (startTick < 1 || startTick > duration
                || !Double.isFinite(position) || position < 0 || position > 1
                || !Double.isFinite(sizeScale) || sizeScale < 0 || sizeScale > 32) {
            throw new JsonParseException("path fields are outside the supported range");
        }
        return new DataSkillDefinition.PathSpec(startTick, position, rotation, sizeScale);
    }

    private static List<DataSkillDefinition.AnimationEvent> parseAnimations(JsonArray json, int duration) {
        List<DataSkillDefinition.AnimationEvent> animations = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject animation = GsonHelper.convertToJsonObject(element, "animation");
            int tick = GsonHelper.getAsInt(animation, "tick");
            if (tick < 0 || tick > duration) {
                throw new JsonParseException("animation tick is outside the supported range");
            }
            String name = GsonHelper.getAsString(animation, "animation", "");
            String randomGroup = GsonHelper.getAsString(animation, "random_group", "").trim();
            int randomIndex = GsonHelper.getAsInt(animation, "random_index", 0);
            if (randomIndex < 0 || randomIndex > 32) {
                throw new JsonParseException("random_index is outside the supported range");
            }
            animations.add(new DataSkillDefinition.AnimationEvent(tick, name,
                    parseEventTrigger(animation), randomGroup, randomIndex));
        }
        return animations;
    }

    private static List<DataSkillDefinition.PlayerEffectEvent> parsePlayerEffects(JsonArray json, int duration) {
        List<DataSkillDefinition.PlayerEffectEvent> effects = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject effect = GsonHelper.convertToJsonObject(element, "player_effect");
            int tick = GsonHelper.getAsInt(effect, "tick");
            String effectId = GsonHelper.getAsString(effect, "effect", "").trim();
            ResourceLocation id = ResourceLocation.tryParse(effectId);
            if (tick < 0 || tick > duration || id == null
                    || BuiltInRegistries.MOB_EFFECT.getHolder(id).isEmpty()) {
                throw new JsonParseException("Unknown player effect: " + effectId);
            }
            int effectDuration = GsonHelper.getAsInt(effect, "duration_ticks", 1);
            int amplifier = GsonHelper.getAsInt(effect, "amplifier", 0);
            if (effectDuration < 1 || effectDuration > 100000 || amplifier < 0 || amplifier > 255) {
                throw new JsonParseException("player effect duration/amplifier is outside the supported range");
            }
            effects.add(new DataSkillDefinition.PlayerEffectEvent(tick, effectId, effectDuration, amplifier,
                    GsonHelper.getAsBoolean(effect, "show_particles", false), parseEventTrigger(effect)));
        }
        return effects;
    }

    private static String parseEventTrigger(JsonObject event) {
        String trigger = GsonHelper.getAsString(event, "trigger", "TICK").toUpperCase(java.util.Locale.ROOT);
        if (!trigger.equals("TICK") && !trigger.equals("CONTACT") && !trigger.equals("GROUND")) {
            throw new JsonParseException("event trigger must be TICK, CONTACT or GROUND");
        }
        return trigger;
    }

    private static List<DataSkillDefinition.SoundEvent> parseSounds(JsonArray json, int duration) {
        List<DataSkillDefinition.SoundEvent> sounds = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject soundEvent = GsonHelper.convertToJsonObject(element, "sound");
            int tick = GsonHelper.getAsInt(soundEvent, "tick");
            String sound = GsonHelper.getAsString(soundEvent, "sound").trim();
            float volume = GsonHelper.getAsFloat(soundEvent, "volume", 1.0F);
            float pitch = GsonHelper.getAsFloat(soundEvent, "pitch", 1.0F);
            ResourceLocation soundId = ResourceLocation.tryParse(sound);
            if (tick < 0 || tick > duration || soundId == null
                    || !BuiltInRegistries.SOUND_EVENT.containsKey(soundId)
                    || !Float.isFinite(volume) || volume < 0 || volume > 16
                    || !Float.isFinite(pitch) || pitch < 0.5F || pitch > 2.0F) {
                throw new JsonParseException("sound event fields are outside the supported range");
            }
            sounds.add(new DataSkillDefinition.SoundEvent(tick, sound, volume, pitch, parseEventTrigger(soundEvent)));
        }
        return sounds;
    }

    private static boolean allFinite(double[] values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static List<DataSkillDefinition.Effect> parseEffects(JsonArray json) {
        List<DataSkillDefinition.Effect> effects = new ArrayList<>();
        for (JsonElement element : json) {
            JsonObject effect = GsonHelper.convertToJsonObject(element, "effect");
            String type = GsonHelper.getAsString(effect, "type");
            if (!type.equals("undead_bonus") && !type.equals("stun") && !type.equals("lifesteal")
                    && !SkillHitEffectRegistry.isRegistered(type)) {
                throw new JsonParseException("Unknown hit effect '" + type
                        + "'. Register it in SkillHitEffectRegistry before using it.");
            }
            effects.add(new DataSkillDefinition.Effect(type,
                    GsonHelper.getAsDouble(effect, "multiplier", 1),
                    GsonHelper.getAsInt(effect, "duration_ticks", 0),
                    GsonHelper.getAsInt(effect, "amplifier", 0),
                    GsonHelper.getAsString(effect, "effect_id", "")));
        }
        return effects;
    }

    private static double[] readVector(JsonArray json, String name) {
        if (json.size() != 3) {
            throw new JsonParseException(name + " must contain three numbers");
        }
        return new double[] { json.get(0).getAsDouble(), json.get(1).getAsDouble(), json.get(2).getAsDouble() };
    }
}
