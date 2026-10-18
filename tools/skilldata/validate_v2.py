#!/usr/bin/env python3
"""skilldata JSONのローダー制約検証 (SkillDataLoader.parse と同じ規則)。"""

from __future__ import annotations

import json
import math
import re
import sys
from pathlib import Path

VALID_TYPES = {"SIMPLE", "RUSH", "TRANSFORM", "TRANSFORM_FINISH"}
VALID_WEAPONS = {
    "ONE_HANDED_SWORD", "TWO_HANDED_SWORD", "KATANA", "AXE", "RAPIER", "CLAW",
    "SPEAR", "MACE", "SCYTHE", "DAGGER", "DUALSWORD",
}
VALID_EFFECTS = {"undead_bonus", "stun", "lifesteal", "low_hp_bonus", "magic_bonus"}
SOUND_ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def fail(name: str, path: str, message: str, problems: list[str]) -> None:
    problems.append(f"{name}: {path}: {message}")


def vec3(value: list, path: str, name: str, problems: list[str]) -> None:
    if not isinstance(value, list) or len(value) != 3:
        fail(name, path, "3要素の配列が必要", problems)
        return
    for element in value:
        if not isinstance(element, (int, float)) or isinstance(element, bool) \
                or not math.isfinite(element):
            fail(name, path, f"非有限/非数値: {element}", problems)


def validate(path: Path, problems: list[str]) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    name = data.get("name", path.stem)

    if data.get("schema_version") != 1:
        fail(name, "schema_version", "1固定であるべき", problems)
    if not re.fullmatch(r"[a-z0-9_]+", str(data.get("name", ""))):
        fail(name, "name", "小文字/数字/アンダースコアのみ", problems)
    if not isinstance(data.get("id"), int) or data["id"] < 0:
        fail(name, "id", "0 (auto-assign) または 1以上の整数", problems)

    duration = data.get("duration_ticks")
    cooldown = data.get("cooldown")
    sp_cost = data.get("sp_cost")
    if not isinstance(duration, int) or not (1 <= duration <= 1200):
        fail(name, "duration_ticks", f"範囲外 (1..1200): {duration}", problems)
    if not isinstance(cooldown, (int, float)) or cooldown < 0:
        fail(name, "cooldown", "負値", problems)
    if not isinstance(sp_cost, (int, float)) or sp_cost < 0:
        fail(name, "sp_cost", "負値", problems)

    weapons = data.get("weapons")
    if not isinstance(weapons, list) or not weapons:
        fail(name, "weapons", "1つ以上必要", problems)
    for weapon in weapons or []:
        if weapon not in VALID_WEAPONS:
            fail(name, "weapons", f"未知の武器種: {weapon}", problems)

    skill_type = data.get("type")
    if skill_type not in VALID_TYPES:
        fail(name, "type", f"未知: {skill_type}", problems)

    color = data.get("color", "")
    if not re.fullmatch(r"#[0-9a-fA-F]{6}", str(color)):
        fail(name, "color", f"#RRGGBB形式でない: {color}", problems)

    def check_tick(value: int, label: str, upper: int | None = None) -> bool:
        top = duration if upper is None else upper
        if not isinstance(value, int) or not (0 <= value <= top):
            fail(name, label, f"tick範囲外: {value} (duration={duration})", problems)
            return False
        return True

    for index, motion in enumerate(data.get("motions", []) or []):
        prefix = f"motions[{index}]"
        check_tick(motion.get("tick", 0), prefix + ".tick")
        for key in ("forward", "strafe", "vertical"):
            value = motion.get(key, 0)
            if not isinstance(value, (int, float)) or not math.isfinite(value):
                fail(name, f"{prefix}.{key}", f"非有限: {value}", problems)

    for index, hitbox in enumerate(data.get("hitboxes", []) or []):
        prefix = f"hitboxes[{index}]"
        check_tick(hitbox.get("tick", 0), prefix + ".tick")
        vec3(hitbox.get("offset", [0, 0, 0]), prefix + ".offset", name, problems)
        size = hitbox.get("size", [0, 0, 0])
        vec3(size, prefix + ".size", name, problems)
        for element in size:
            if not (0 < element <= 32):
                fail(name, prefix + ".size", f"範囲外 (0<x<=32): {element}", problems)
        multiplier = hitbox.get("damage_multiplier", 1)
        if not isinstance(multiplier, (int, float)) or not math.isfinite(multiplier) \
                or multiplier < 0:
            fail(name, prefix + ".damage_multiplier", f"不正: {multiplier}", problems)
        kb = hitbox.get("knockback", 0)
        if abs(kb) > 16:
            fail(name, prefix + ".knockback", f"|knockback|>16: {kb}", problems)
        kb_mult = hitbox.get("knockback_multiplier", -1)
        if kb_mult != -1 and abs(kb_mult) > 16:
            fail(name, prefix + ".knockback_multiplier", f"|mult|>16: {kb_mult}", problems)
        if hitbox.get("damage_type", "BASE") not in ("BASE", "RUSH"):
            fail(name, prefix + ".damage_type", f"不正: {hitbox.get('damage_type')}", problems)
        sound = hitbox.get("sound", "")
        if sound and not SOUND_ID_RE.fullmatch(sound):
            fail(name, prefix + ".sound", f"sound id形式不正: {sound}", problems)
        trigger = str(hitbox.get("trigger", "TICK")).upper()
        if trigger not in ("TICK", "CONTACT"):
            fail(name, prefix + ".trigger", f"不正: {trigger}", problems)
        for effect in hitbox.get("effects", []) or []:
            if effect.get("type") not in VALID_EFFECTS:
                fail(name, prefix + ".effects", f"未登録effect: {effect.get('type')}", problems)

    for index, projectile in enumerate(data.get("projectiles", []) or []):
        prefix = f"projectiles[{index}]"
        check_tick(projectile.get("tick", 0), prefix + ".tick")
        vec3(projectile.get("offset", [0, 0, 0]), prefix + ".offset", name, problems)
        vec3(projectile.get("rotation", [0, 0, 0]), prefix + ".rotation", name, problems)
        for key in ("player_pitch_x_multiplier", "player_pitch_z_multiplier"):
            value = projectile.get(key, 0)
            if abs(value) > 4:
                fail(name, f"{prefix}.{key}", f"|mult|>4: {value}", problems)
        render_pitch = projectile.get("render_pitch", 0)
        if not (-180 <= render_pitch <= 180):
            fail(name, prefix + ".render_pitch", f"範囲外: {render_pitch}", problems)
        size = projectile.get("size", [0, 0, 0])
        vec3(size, prefix + ".size", name, problems)
        for element in size:
            if not (0 < element <= 32):
                fail(name, prefix + ".size", f"範囲外 (0<x<=32): {element}", problems)
        multiplier = projectile.get("damage_multiplier", 1)
        if not isinstance(multiplier, (int, float)) or multiplier < 0 \
                or not math.isfinite(multiplier):
            fail(name, prefix + ".damage_multiplier", f"不正: {multiplier}", problems)
        kb = projectile.get("knockback", 0)
        if abs(kb) > 16:
            fail(name, prefix + ".knockback", f"|knockback|>16: {kb}", problems)
        kb_mult = projectile.get("knockback_multiplier", -1)
        if kb_mult != -1 and abs(kb_mult) > 16:
            fail(name, prefix + ".knockback_multiplier", f"|mult|>16: {kb_mult}", problems)
        if projectile.get("damage_type", "BASE") not in ("BASE", "RUSH"):
            fail(name, prefix + ".damage_type", f"不正: {projectile.get('damage_type')}", problems)
        projectile_duration = projectile.get("duration_ticks", 12)
        if not isinstance(projectile_duration, int) or not (1 <= projectile_duration <= 1200):
            fail(name, prefix + ".duration_ticks", f"範囲外: {projectile_duration}", problems)
        travel = projectile.get("travel_distance", 16)
        if not (0 <= travel <= 128):
            fail(name, prefix + ".travel_distance", f"範囲外: {travel}", problems)
        if "movement" in projectile:
            movement = projectile["movement"]
            vec3(movement, prefix + ".movement", name, problems)
            for element in movement:
                if abs(element) > 128:
                    fail(name, prefix + ".movement", f"|movement|>128: {element}", problems)
        particle = projectile.get("particle", "")
        if not particle or len(particle) > 128:
            fail(name, prefix + ".particle", f"不正: {particle}", problems)
        sound = projectile.get("sound", "")
        if sound and not SOUND_ID_RE.fullmatch(sound):
            fail(name, prefix + ".sound", f"sound id形式不正: {sound}", problems)
        trigger = str(projectile.get("trigger", "TICK")).upper()
        if trigger not in ("TICK", "CONTACT"):
            fail(name, prefix + ".trigger", f"不正: {trigger}", problems)
        for effect in projectile.get("effects", []) or []:
            if effect.get("type") not in VALID_EFFECTS:
                fail(name, prefix + ".effects", f"未登録effect: {effect.get('type')}", problems)

    for index, sound_event in enumerate(data.get("sounds", []) or []):
        prefix = f"sounds[{index}]"
        check_tick(sound_event.get("tick", 0), prefix + ".tick")
        sound = sound_event.get("sound", "")
        if not SOUND_ID_RE.fullmatch(sound):
            fail(name, prefix + ".sound", f"sound id形式不正: {sound}", problems)
        volume = sound_event.get("volume", 1)
        if not (0 <= volume <= 16):
            fail(name, prefix + ".volume", f"範囲外: {volume}", problems)
        pitch = sound_event.get("pitch", 1)
        if not (0.5 <= pitch <= 2.0):
            fail(name, prefix + ".pitch", f"範囲外: {pitch}", problems)

    if skill_type == "RUSH":
        rush_start = data.get("rush_start_ticks")
        if not isinstance(rush_start, int) or not (0 <= rush_start <= duration):
            fail(name, "rush_start_ticks", f"範囲外: {rush_start}", problems)
        combo = data.get("rush_combo")
        if combo is not None:
            latest = combo.get("latest_contact_tick", duration)
            timeout = combo.get("no_contact_timeout_tick", duration + 1)
            offset = combo.get("contact_animation_offset", 0)
            finish = combo.get("finish_contact_offset", duration)
            contact_motion = combo.get("contact_motion", {}) or {}
            forward = contact_motion.get("forward", -0.5)
            if abs(forward) > 16:
                fail(name, "rush_combo.contact_motion.forward", f"|forward|>16: {forward}", problems)
            if not (1 <= latest <= duration):
                fail(name, "rush_combo.latest_contact_tick", f"範囲外: {latest}", problems)
            if not (1 <= timeout <= duration + 1):
                fail(name, "rush_combo.no_contact_timeout_tick", f"範囲外: {timeout}", problems)
            if not (0 <= offset <= duration):
                fail(name, "rush_combo.contact_animation_offset", f"範囲外: {offset}", problems)
            if not (offset <= finish <= duration):
                fail(name, "rush_combo.finish_contact_offset", f"範囲外: {finish}", problems)
            if latest + finish > duration:
                fail(name, "rush_combo", f"latest({latest})+finish({finish})>duration({duration})",
                     problems)

    trail_length = data.get("trail_length", 1.5)
    if not isinstance(trail_length, (int, float)) or isinstance(trail_length, bool) \
            or not math.isfinite(trail_length) or not (0 < trail_length <= 32):
        fail(name, "trail_length", f"範囲外 (0<..<=32): {trail_length}", problems)
    shockwave = data.get("shockwave_ticks", 0)
    if not isinstance(shockwave, int) or not (0 <= shockwave <= 1200):
        fail(name, "shockwave_ticks", f"範囲外 (0..1200): {shockwave}", problems)
    hitstop = data.get("hitstop_ticks", -1)
    if not isinstance(hitstop, int) or not (-1 <= hitstop <= 40):
        fail(name, "hitstop_ticks", f"範囲外 (-1..40): {hitstop}", problems)
    proficiency_category = data.get("proficiency_category")
    if proficiency_category is not None and proficiency_category not in VALID_WEAPONS:
        fail(name, "proficiency_category", f"不明な武器種: {proficiency_category}", problems)
    unlock_proficiency = data.get("unlock_proficiency", 0)
    if not isinstance(unlock_proficiency, int) or isinstance(unlock_proficiency, bool) \
            or not (0 <= unlock_proficiency <= 1000000):
        fail(name, "unlock_proficiency", f"範囲外 (0..1000000): {unlock_proficiency}", problems)
    assignable = data.get("assignable", True)
    if not isinstance(assignable, bool):
        fail(name, "assignable", f"真偽値が必要: {assignable}", problems)
    return data


def main() -> int:
    directory = Path(sys.argv[1]) if len(sys.argv) > 1 else \
        Path("build/skilldata_migration_v2")
    problems: list[str] = []
    files = sorted(p for p in directory.glob("*.json") if p.name != "conversion-report-v2.json")
    for path in files:
        try:
            validate(path, problems)
        except (json.JSONDecodeError, KeyError) as error:
            problems.append(f"{path.stem}: PARSE ERROR: {error}")
    print(f"検証対象: {len(files)}ファイル")
    if problems:
        print(f"問題: {len(problems)}件")
        for problem in problems:
            print("  -", problem)
        return 1
    print("すべての制約を満たしています。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
