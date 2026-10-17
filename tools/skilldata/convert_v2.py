#!/usr/bin/env python3
"""v2変換エンジン: Javaスキルクラスの型パターンからskilldata JSONを生成する。

convert_java_skills.py (v1) を置き換える改良版。
- SkillEngine (skill_interp.py) による静的評価:
  if/else-if/else チェーン・switch文・三項演算・ヘルパーメソッド (Setsize等) の解決
- 冗語な括弧の除去 ( (((x))).scale(k) 形式の位置式の修正 )
- 非RUSHスキルの setDeltaMovement → motions への変換
- FinalTick 分岐がないスキルは実行モデルに従い各tickへ複製 (Javaの毎tick生成を再現)
- テクスチャ → 色 / 特殊効果 (undead_bonus 等) の自動付与
"""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from pathlib import Path
from typing import Any

import convert_java_skills as v1
import skill_interp as si
from skill_interp import (  # noqa: F401  (再エクスポート)
    SkillEngine,
    UnresolvedExpression,
    block_after,
    eval_number,
    match_assignment,
    strip_redundant_parens,
    try_number,
    unwrap,
)


# ------------------------------------------------------------------
# Java式 → スキーマ値 変換
# ------------------------------------------------------------------

def rotation_parts(engine: SkillEngine, expression: str, values: dict[str, str],
                   warnings: list[str]) -> tuple[list[float], float, float]:
    """回転 Vec3 を解析し (rotation, pitch_x_multiplier, pitch_z_multiplier) を返す。"""
    resolved = unwrap(engine.canon(expression, values, warnings))
    match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", resolved, re.DOTALL)
    if not match:
        raise UnresolvedExpression(f"unsupported rotation expression: {resolved}")
    components = v1.split_java_arguments(match.group(1))
    if len(components) != 3:
        raise UnresolvedExpression("rotation Vec3 requires three components")
    rotation: list[float] = []
    pitch_multipliers: list[float] = []
    pitch_pattern = re.compile(r"[\w().]+\.getXRot\(\)\s*(?:\*\s*(-?\d+(?:\.\d+)?))?")
    for component in components:
        multiplier = 0.0
        static_expression = component
        for pitch in list(pitch_pattern.finditer(component)):
            before = component[:pitch.start()].rstrip()
            sign = -1.0 if before.endswith("-") else 1.0
            factor = float(pitch.group(1)) if pitch.group(1) else 1.0
            multiplier += sign * factor
            static_expression = static_expression.replace(pitch.group(0), "0", 1)
        numeric = si.try_number(static_expression)
        if numeric is None:
            raise UnresolvedExpression(f"unsupported rotation component: {component}")
        rotation.append(numeric)
        pitch_multipliers.append(multiplier)
    if abs(pitch_multipliers[1]) > 1.0e-9:
        warnings.append("回転Yに視点依存の係数があり静的近似しました。")
    return rotation, pitch_multipliers[0], pitch_multipliers[2]


def position_offset(engine: SkillEngine, expression: str, values: dict[str, str],
                    warnings: list[str]) -> list[float]:
    """スポーン位置式 → スキーマのoffset [右(+X)/上(+Y)/前(+Z)]。"""
    resolved = unwrap(engine.canon(expression, values, warnings))
    if re.fullmatch(r"(?:[\w.]+\.)?Vec3\.ZERO", resolved):
        return [0.0, 0.0, 0.0]
    literal_vector = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", resolved, re.DOTALL)
    if literal_vector:
        components = v1.split_java_arguments(literal_vector.group(1))
        if len(components) == 3:
            numbers = [si.try_number(component) for component in components]
            if all(number is not None for number in numbers):
                return [float(number) for number in numbers]
    if re.fullmatch(r"[-+0-9.eE()*/ fdF]+", resolved):
        forward = si.try_number(resolved)
        if forward is not None:
            return [0.0, 0.0, forward]

    forward = 0.0
    right = 0.0
    vertical = 0.0
    for vector_name, axis in (("lookVec", "forward"), ("rightVec", "right"), ("upVec", "up")):
        for match in re.finditer(rf"\b{vector_name}\.scale\s*\(([^()]*)\)", resolved):
            amount = si.try_number(match.group(1))
            if amount is None:
                raise UnresolvedExpression(
                    f"unsupported scale factor for {vector_name}: {match.group(1)}")
            before = resolved[max(0, match.start() - 24):match.start()]
            if re.search(r"\.subtract\s*\(\s*$", before):
                amount = -amount
            if axis == "forward":
                forward += amount
            elif axis == "right":
                right += amount
            else:
                vertical += amount
    for match in re.finditer(
        r"\.cross\s*\(\s*new\s+Vec3\s*\(\s*0\s*,\s*1\s*,\s*0\s*\)\s*\)\s*\.normalize\(\)"
        r"\s*\.scale\s*\(([^()]*)\)",
        resolved,
    ):
        amount = si.try_number(match.group(1))
        if amount is None:
            raise UnresolvedExpression(f"unsupported cross-product scale: {match.group(1)}")
        before = resolved[:match.start()]
        if before.rfind(".subtract(") > before.rfind(".add("):
            amount = -amount
        right += amount
    for match in re.finditer(
        r"\.cross\s*\(\s*(?:\(*[\w().]+getLookAngle\s*\(\)\)*|lookVec)\s*\)\s*\.normalize\(\)"
        r"\s*\.scale\s*\(([^()]*)\)",
        resolved,
    ):
        amount = si.try_number(match.group(1))
        if amount is None:
            raise UnresolvedExpression(f"unsupported cross-product scale: {match.group(1)}")
        before = resolved[:match.start()]
        if before.rfind(".subtract(") > before.rfind(".add("):
            amount = -amount
        vertical += amount
    look_scale = re.search(r"(?:lookVec|[\w().]+getLookAngle\s*\(\))\.scale\s*\(([^()]*)\)",
                           resolved)
    if look_scale and forward == 0:
        amount = si.try_number(look_scale.group(1))
        if amount is not None:
            forward = amount
    rotated_look = re.search(
        r"rotateLookVec\s*\([^,]+,\s*([^,]+),\s*([^\)]+)\)\s*\)?\s*\.scale\s*\(([^()]*)\)",
        resolved)
    if rotated_look:
        pitch_deg = si.try_number(rotated_look.group(1))
        yaw_deg = si.try_number(rotated_look.group(2))
        distance = si.try_number(rotated_look.group(3))
        if None in (pitch_deg, yaw_deg, distance):
            raise UnresolvedExpression(
                f"unsupported rotateLookVec factors: {rotated_look.group(0)}")
        pitch = math.radians(pitch_deg)
        yaw = math.radians(yaw_deg)
        forward += distance * math.cos(pitch) * math.cos(yaw)
        right += distance * math.cos(pitch) * math.sin(yaw)
        vertical += -distance * math.sin(pitch)
    eye_height = re.search(r"getEyeHeight\(\)\s*\*\s*([0-9.]+)[fFdD]?", resolved)
    if eye_height:
        vertical = 1.62 * float(eye_height.group(1))
    else:
        if re.search(r"\.getEyeHeight\s*\(", resolved):
            raise UnresolvedExpression(
                f"unsupported eye-height position expression: {resolved}")
        literal_height = re.search(r"\.add\s*\(\s*0\s*,\s*(-?[0-9.]+)[fFdD]?\s*,\s*0\s*\)",
                                   resolved)
        if literal_height:
            vertical = float(literal_height.group(1))
    for match in re.finditer(r"\.add\s*\(\s*0\s*,\s*([^,]+),\s*0\s*\)", resolved):
        height_expression = match.group(1).strip()
        if "getEyeHeight" not in height_expression:
            additional = si.try_number(height_expression)
            if additional is None:
                raise UnresolvedExpression(
                    f"unsupported vertical offset expression: {height_expression}")
            vertical += additional
    return [right, vertical, forward]


def size_vector(engine: SkillEngine, expression: str, values: dict[str, str],
                warnings: list[str]) -> list[float]:
    resolved = unwrap(engine.canon(expression, values, warnings))
    match = re.fullmatch(r"new\s+Vector3f\s*\((.*)\)", resolved, re.DOTALL)
    if not match:
        raise UnresolvedExpression(f"unsupported Vector3f expression: {resolved}")
    components = v1.split_java_arguments(match.group(1))
    if len(components) != 3:
        raise UnresolvedExpression("Vector3f requires three components")
    numbers = [si.try_number(component) for component in components]
    if None in numbers:
        missing = [component for component, number in zip(components, numbers)
                   if number is None]
        raise UnresolvedExpression(f"unsupported size components: {missing}")
    return [float(number) for number in numbers]


def damage_info(engine: SkillEngine, expression: str, values: dict[str, str],
                warnings: list[str]) -> tuple[str, float]:
    """damage式 → (damage_type, multiplier)。"""
    resolved = unwrap(engine.canon(expression, values, warnings))
    match = re.search(r"(?:BaseDamage|RushDamage)\s*\(.*?\)(?:\s*\*\s*(.+))?$", resolved)
    if match:
        multiplier_text = match.group(1)
        multiplier = 1.0
        if multiplier_text:
            number = si.try_number(multiplier_text)
            if number is None:
                raise UnresolvedExpression(
                    f"unsupported damage multiplier: {multiplier_text}")
            multiplier = number
        return ("RUSH" if "RushDamage" in resolved else "BASE"), multiplier
    try:
        number = eval_number(resolved)
        return "BASE", float(number)
    except ValueError as error:
        raise UnresolvedExpression(f"unsupported damage expression: {resolved}") from error


def knockback_info(engine: SkillEngine, expression: str, values: dict[str, str],
                   warnings: list[str]) -> tuple[float, float]:
    """knockback式 → (knockback値, knockback_multiplier)。"""
    resolved = unwrap(engine.canon(expression, values, warnings))
    match = re.search(r"BaseKnowBack\s*\(.*?\)\s*\*\s*(.+)$", resolved)
    if match:
        number = si.try_number(match.group(1))
        if number is None:
            raise UnresolvedExpression(f"unsupported knockback multiplier: {match.group(1)}")
        return number, number
    if re.fullmatch(r"BaseKnowBack\s*\(.*?\)", resolved):
        return 1.0, 1.0
    try:
        number = eval_number(resolved)
        return float(number), -1.0
    except ValueError as error:
        raise UnresolvedExpression(
            f"unsupported knockback expression: {resolved}") from error


def texture_of(engine: SkillEngine, expression: str, values: dict[str, str],
               warnings: list[str]) -> str:
    resolved = unwrap(engine.canon(expression, values, warnings))
    match = re.fullmatch(r'"([^"]+)"', resolved)
    if not match:
        raise UnresolvedExpression(f"unsupported attack texture expression: {resolved}")
    return match.group(1)


def movement_vector(engine: SkillEngine, expression: str, values: dict[str, str],
                    warnings: list[str]) -> list[float]:
    resolved = unwrap(engine.canon(expression, values, warnings))
    if re.fullmatch(r"(?:[\w.]+\.)?Vec3\.ZERO", resolved):
        return [0.0, 0.0, 0.0]
    match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", resolved, re.DOTALL)
    if not match:
        raise UnresolvedExpression(f"unsupported movement expression: {resolved}")
    components = v1.split_java_arguments(match.group(1))
    if len(components) != 3:
        raise UnresolvedExpression("movement Vec3 requires three components")
    numbers = [si.try_number(component) for component in components]
    if None in numbers:
        raise UnresolvedExpression(f"unsupported movement components: {components}")
    return [float(number) for number in numbers]


# ------------------------------------------------------------------
# setDeltaMovement → motions
# ------------------------------------------------------------------

_LOOK_BASE = (r"(?:\(*\s*)?(?:(?:(?:[A-Za-z_][\w]*)\.)?getLookAngle\s*\(\s*\)"
              r"|\blookVec\b)")
_NUMBER = r"[-+]?(?:[0-9]+\.?[0-9]*|\.[0-9]+)[fFdD]?"


def _strip_reverse(text: str) -> tuple[str, bool]:
    """式から `.reverse()` を取り除き、反転の有無を返す。"""
    reverse = False
    while True:
        match = re.search(r"\.\s*reverse\s*\(\s*\)", text)
        if not match:
            break
        reverse = not reverse
        text = (text[:match.start()] + text[match.end():]).strip()
    return text, reverse


def _split_numeric_offset(text: str) -> tuple[str, float]:
    """`expr + 0.25` のような末尾の数値オフセットを分離する。"""
    match = re.fullmatch(r"(?P<head>.+?)(?P<sign>[+\-])\s*(?P<tail>" + _NUMBER + r")",
                         text)
    if match:
        tail = si.try_number(match.group("tail"))
        head = match.group("head").rstrip()
        if tail is not None and not head.endswith(("*", "/", "+", "-", "(")):
            offset = tail if match.group("sign") == "+" else -tail
            return head, offset
    return text, 0.0


def _look_component(text: str) -> tuple[float, str | None, float] | None:
    """look由来成分を (係数, 軸, 数値オフセット) に分解する。

    軸が None の場合は look ベクトル全体のスケール。`.reverse()` は符号反転する。
    """
    text = si.strip_redundant_parens(text).strip()
    text, reverse = _strip_reverse(text)
    text, offset = _split_numeric_offset(text)
    factor: float | None = None
    axis: str | None = None
    match = re.fullmatch(
        rf"{_LOOK_BASE}\s*\.\s*scale\s*\(\s*(?P<factor>[^()]+?)\s*\)"
        r"\s*(?:\.\s*(?P<axis>[xyz]))?", text)
    if match:
        factor = si.try_number(match.group("factor"))
        axis = match.group("axis")
    else:
        for pattern in (
                rf"{_LOOK_BASE}\s*\.\s*(?P<axis>[xyz])\s*\*\s*(?P<factor>{_NUMBER})",
                rf"(?P<factor>{_NUMBER})\s*\*\s*{_LOOK_BASE}\s*\.\s*(?P<axis>[xyz])"):
            match = re.fullmatch(pattern, text)
            if match:
                factor = si.try_number(match.group("factor"))
                axis = match.group("axis")
                break
        else:
            match = re.fullmatch(rf"{_LOOK_BASE}(?:\s*\.\s*(?P<axis>[xyz]))?", text)
            if match:
                factor = 1.0
                axis = match.group("axis")
    if factor is None:
        return None
    if reverse:
        factor = -factor
    return factor, axis, offset


def _is_zero_vec3(text: str) -> bool:
    match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", text, re.DOTALL)
    if not match:
        return False
    components = [si.try_number(si.strip_redundant_parens(part))
                  for part in v1.split_java_arguments(match.group(1))]
    return len(components) == 3 and all(value is not None and abs(value) < 1.0e-9
                                        for value in components)


def _motion_from_components(components: list[str], warnings: list[str]
                            ) -> tuple[float, float, float] | None:
    if len(components) != 3:
        warnings.append(f"予期しないsetDeltaMovement引数: {components}")
        return None
    parts = [si.strip_redundant_parens(component).strip() for component in components]
    axes = "xyz"
    look_factors: list[float] = []
    vertical = 0.0
    has_look = False
    for index, part in enumerate(parts):
        parsed = _look_component(part)
        if parsed is not None:
            has_look = True
            factor, axis, offset = parsed
            if axis == axes[index]:
                look_factors.append(factor)
            vertical += offset
        elif index == 1:
            number = si.try_number(part)
            if number is not None:
                vertical = number
    if has_look:
        if not look_factors:
            warnings.append(f"setDeltaMovementのlook成分を解釈できません: {parts}")
            return None
        forward = look_factors[0]
        if any(abs(factor - forward) > 1.0e-9 for factor in look_factors):
            warnings.append(f"setDeltaMovementのlook係数が一致しないため先頭を使用: {parts}")
        return forward, 0.0, vertical
    numbers = [si.try_number(part) for part in parts]
    if None not in numbers:
        if abs(numbers[0]) < 1.0e-9 and abs(numbers[2]) < 1.0e-9:
            if abs(numbers[1]) < 1.0e-9:
                return 0.0, 0.0, 0.0
            warnings.append(f"setDeltaMovementを垂直移動として近似: vertical={numbers[1]}")
            return 0.0, 0.0, numbers[1]
        warnings.append(f"setDeltaMovementを水平・垂直の固定移動として近似: {parts}")
        return 0.0, numbers[0] + numbers[2], numbers[1]
    warnings.append(f"setDeltaMovementを解析できません: {parts}")
    return None


def motion_from_args(args: list[str], warnings: list[str]) -> tuple[float, float, float] | None:
    """setDeltaMovement(...) → (forward, strafe, vertical)。"""
    if len(args) == 3:
        return _motion_from_components(args, warnings)
    if len(args) != 1:
        warnings.append(f"予期しないsetDeltaMovement引数: {args}")
        return None
    raw = args[0]
    arg = si.strip_redundant_parens(raw).strip()
    if _is_zero_vec3(arg):
        # Vec3.ZERO / new Vec3(0,0,0) はその場停止を表す。
        return 0.0, 0.0, 0.0
    add_match = re.fullmatch(
        r"(?:[\w.]+\.)?getDeltaMovement\s*\(\s*\)\s*\.\s*add\s*\((.*)\)", arg, re.DOTALL)
    if add_match:
        inner = v1.split_java_arguments(add_match.group(1))
        if len(inner) == 3:
            numbers = [si.try_number(si.strip_redundant_parens(part)) for part in inner]
            if None not in numbers:
                warnings.append(
                    f"現在速度への加算を垂直移動として近似: vertical={numbers[1]}")
                return 0.0, 0.0, numbers[1]
        # add(...) の中身を通常の移動ベクトルとして再解釈する。
        return motion_from_args(inner, warnings)
    vec_match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", arg, re.DOTALL)
    if vec_match:
        return _motion_from_components(v1.split_java_arguments(vec_match.group(1)), warnings)
    whole = _look_component(arg)
    if whole is not None and whole[1] is None:
        return whole[0], 0.0, whole[2]
    warnings.append(f"setDeltaMovementを解析できません: {raw}")
    return None


# ------------------------------------------------------------------
# 呼び出し → イベント化
# ------------------------------------------------------------------

def convert_spawn_call(engine: SkillEngine, call: si.RawCall, tick: int,
                       values: dict[str, str], warnings: list[str]) -> dict[str, Any]:
    args = call.args
    if len(args) != 10:
        raise UnresolvedExpression(
            f"spawnAttackEffect expected 10 arguments, found {len(args)}")
    offset = position_offset(engine, args[1], values, warnings)
    rotation, pitch_x, pitch_z = rotation_parts(engine, args[2], values, warnings)
    size = size_vector(engine, args[3], values, warnings)
    damage_type, damage_multiplier = damage_info(engine, args[5], values, warnings)
    knockback, knockback_multiplier = knockback_info(engine, args[6], values, warnings)
    duration_value = engine.canon(args[7], values, warnings)
    duration = si.try_int(duration_value)
    if duration is None:
        raise UnresolvedExpression(f"unsupported duration expression: {args[7]}")
    particle = texture_of(engine, args[8], values, warnings)
    movement = movement_vector(engine, args[9], values, warnings)
    event: dict[str, Any] = {
        "tick": tick,
        "offset": offset,
        "rotation": rotation,
        "size": size,
        "damage_multiplier": damage_multiplier,
        "damage_type": damage_type,
        "knockback": knockback,
        "knockback_multiplier": knockback_multiplier,
        "duration_ticks": duration,
        "travel_distance": 0,
        "particle": particle,
        "sound": "",
    }
    effects = list(v1.TEXTURE_EFFECTS.get(particle, []))
    if effects:
        event["effects"] = effects
    if abs(pitch_x) > 1.0e-9:
        event["player_pitch_x_multiplier"] = pitch_x
    if abs(pitch_z) > 1.0e-9:
        event["player_pitch_z_multiplier"] = pitch_z
    if any(abs(component) >= 1.0e-9 for component in movement):
        event["movement"] = movement
    return event


def build_events_for_segment(engine: SkillEngine, segment_body: str, tick: int | None,
                             values: dict[str, str], warnings: list[str],
                             duration: int) -> tuple[list, list, list, list]:
    """1セグメントの呼び出しを hitboxes / projectiles / motions / sounds に変換する。"""
    calls = engine.analyze_segment(segment_body, values, warnings)
    hitboxes: list[dict[str, Any]] = []
    projectiles: list[dict[str, Any]] = []
    motions: list[dict[str, Any]] = []
    sounds: list[dict[str, Any]] = []

    def ticks_for() -> list[int]:
        if tick is not None:
            return [tick]
        return list(range(1, max(duration, 1) + 1))

    for call in calls:
        if call.kind == "spawn":
            try:
                prototype = convert_spawn_call(engine, call, 1, values, warnings)
            except UnresolvedExpression as error:
                warnings.append(
                    f"tick {tick}のspawnAttackEffectを変換できません: {error}")
                continue
            is_moving = "movement" in prototype
            movements = prototype.pop("movement", None)
            for event_tick in ticks_for():
                event = dict(prototype)
                event["tick"] = event_tick
                if movements is not None:
                    event["movement"] = movements
                if is_moving:
                    projectiles.append(event)
                else:
                    hitboxes.append(event)
        elif call.kind == "sound":
            sound, pitch = v1.SOUND_HELPERS[call.name]
            sound_tick = 1 if tick is None else tick
            sounds.append(dict(tick=sound_tick, sound=sound, volume=1.0, pitch=pitch))
        elif call.kind == "motion":
            parsed = motion_from_args(call.args, warnings)
            if parsed is None:
                continue
            forward, strafe, vertical = parsed
            motions.append(dict(tick=1 if tick is None else tick,
                                forward=forward, strafe=strafe, vertical=vertical))
    return hitboxes, projectiles, motions, sounds


# ------------------------------------------------------------------
# 1スキル変換
# ------------------------------------------------------------------

def convert_skill(source: str, default_duration: int
                  ) -> dict[str, Any] | None:
    """1スキルのイベント群を生成する。execute未検出なら None。"""
    warnings: list[str] = []
    engine = SkillEngine(source)
    execute_match = re.search(r"\bvoid\s+execute\s*\([^)]*\)\s*\{", source)
    if not execute_match:
        warnings.append("executeメソッドを検出できませんでした。")
        return None
    try:
        execute_body, _ = block_after(source, execute_match.end() - 1)
    except ValueError:
        warnings.append("executeメソッドのブロックを解析できませんでした。")
        return None

    has_tick_dispatch = bool(re.search(r"\bFinalTick\s*==", execute_body))

    segments, _ = engine.split_tick_segments(execute_body, max_tick=max(default_duration, 64))
    guarded_ticks = sorted({tick for tick, _, _ in segments if tick is not None})
    duration = max(default_duration, max(guarded_ticks, default=1))

    hitboxes: list[dict[str, Any]] = []
    projectiles: list[dict[str, Any]] = []
    motions: list[dict[str, Any]] = []
    sounds: list[dict[str, Any]] = []

    for tick, segment_body, seed in segments:
        segment_warnings: list[str] = []
        hb, pr, mo, sn = build_events_for_segment(
            engine, segment_body, tick, dict(seed), segment_warnings, duration)
        hitboxes.extend(hb)
        projectiles.extend(pr)
        motions.extend(mo)
        sounds.extend(sn)
        warnings.extend(segment_warnings)

    colors = [v1.TEXTURE_COLORS.get(event["particle"])
              for event in (*hitboxes, *projectiles)]
    color = next((value for value in colors if value), "#54D8FF")
    return {
        "hitboxes": hitboxes,
        "projectiles": projectiles,
        "motions": motions,
        "sounds": sounds,
        "color": color,
        "duration": duration,
        "warnings": warnings,
    }


# ------------------------------------------------------------------
# 定義JSON生成
# ------------------------------------------------------------------

def make_definition(registration: v1.SkillRegistration, duration: int,
                    rush_start_ticks: int = 5,
                    sounds: list[dict[str, Any]] | None = None,
                    projectiles: list[dict[str, Any]] | None = None,
                    legacy_fallback: bool = True,
                    hitboxes: list[dict[str, Any]] | None = None,
                    motions: list[dict[str, Any]] | None = None,
                    rush_combo: dict[str, Any] | None = None,
                    color: str = "#54D8FF") -> dict[str, Any]:
    definition = {
        "schema_version": 1,
        "id": registration.legacy_id,
        "name": registration.name,
        "replace_builtin": True,
        "animation": registration.name,
        "type": registration.skill_type,
        "weapons": registration.weapons,
        "cooldown": registration.cooldown,
        "sp_cost": registration.sp_cost,
        "duration_ticks": duration,
        "transform_limit_ticks": registration.transform_limit_ticks,
        "hidden": registration.hidden,
        "color": color,
        "motions": motions or [],
        "hitboxes": [dict(box, effects=box.get("effects", [])) for box in (hitboxes or [])],
        "projectiles": [dict(projectile, effects=projectile.get("effects", []))
                        for projectile in (projectiles or [])],
        "legacy_fallback": legacy_fallback,
        "sounds": sounds or [],
    }
    if registration.skill_type == "RUSH":
        definition.update({
            "rush_start_ticks": rush_start_ticks,
            "rush_start_animation": "start",
            "rush_move_animation": "move",
            "rush_finish_animation": "finish",
        })
        if rush_combo is not None:
            definition["rush_combo"] = rush_combo
    return definition


def write_definition(target: Path, definition: dict[str, Any]) -> None:
    # 同tickの重複イベントを統一 (else節内の同一イベントなど)
    seen: set[tuple] = set()
    boxes = []
    for box in definition["hitboxes"]:
        key = (box["tick"], tuple(box["offset"]), tuple(box["size"]),
               box["damage_multiplier"], box.get("damage_type"), box["knockback"],
               box["knockback_multiplier"])
        if key in seen:
            continue
        seen.add(key)
        boxes.append(box)
    boxes.sort(key=lambda box: box["tick"])
    definition["hitboxes"] = boxes

    seen = set()
    shots = []
    for projectile in definition["projectiles"]:
        key = (projectile["tick"], tuple(projectile["offset"]), tuple(projectile["size"]),
               projectile["damage_multiplier"], projectile["knockback"],
               tuple(projectile.get("movement", [])))
        if key in seen:
            continue
        seen.add(key)
        shots.append(projectile)
    shots.sort(key=lambda projectile: projectile["tick"])
    definition["projectiles"] = shots

    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(
        json.dumps(definition, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ------------------------------------------------------------------
# 全スキル変換
# ------------------------------------------------------------------

def convert_all(registry_path: Path, skill_dir: Path, animations_dir: Path,
                output: Path, force: bool, skill_names: set[str] | None) -> int:
    try:
        registrations = v1.load_registrations(registry_path)
    except (OSError, ValueError) as error:
        print(f"変換元の解析に失敗しました: {error}", file=sys.stderr)
        return 2

    output.mkdir(parents=True, exist_ok=True)
    report: list[dict[str, Any]] = []
    converted = 0

    for registration in registrations:
        if skill_names and registration.name not in skill_names:
            continue
        path, event_ticks, static_warnings = v1.inspect_skill_source(
            skill_dir, registration.skill_class)
        target = output / f"{registration.name}.json"
        name = registration.name

        if path is None:
            definition = make_definition(registration, registration.final_tick)
            write_definition(target, definition)
            report.append(dict(name=name, legacy_id=registration.legacy_id,
                               source=None, status="missing_java",
                               duration_ticks=registration.final_tick,
                               warnings=static_warnings))
            continue

        source = v1.strip_java_comments(path.read_text(encoding="utf-8"))
        warnings: list[str] = []
        rush_start_ticks = 5
        rush_combo = None
        rush_hitboxes: list[dict[str, Any]] = []
        rush_motions: list[dict[str, Any]] = []
        rush_handle = False
        if registration.skill_type == "RUSH":
            combo_metadata, combo_hitboxes, combo_warnings = v1.extract_contact_combo_rush(
                source, registration.final_tick)
            if combo_metadata and combo_hitboxes:
                rush_handle = True
                rush_combo = combo_metadata["rush_combo"]
                rush_start_ticks = int(combo_metadata["rush_start_ticks"])
                rush_hitboxes = combo_hitboxes
                rush_motions = []
                warnings = [
                    "接触後の複数段RUSHをrush_comboとCONTACT hitboxイベントに変換しました。",
                    *combo_warnings,
                ]
            else:
                combo_failures = list(combo_warnings)
                rush_motions, rush_hitboxes, rush_warnings = v1.extract_touch_rush_events(
                    source, registration.final_tick)
                if rush_motions and rush_hitboxes:
                    rush_handle = True
                    warnings = [
                        "接触型RUSHの移動と攻撃をskilldataイベントへ移植しました。",
                        *rush_warnings,
                    ]
                else:
                    warnings.extend(combo_failures)
                    warnings.extend(rush_warnings)

        legacy_fallback = True
        if rush_handle:
            definition = make_definition(
                registration, registration.final_tick, rush_start_ticks, [],
                [], False, rush_hitboxes, rush_motions, rush_combo)
            write_definition(target, definition)
            report.append({
                "name": name, "legacy_id": registration.legacy_id,
                "class": registration.skill_class, "source": str(path), "output": str(target),
                "status": "rush_converted", "duration_ticks": registration.final_tick,
                "warnings": warnings})
            converted += 1
            continue

        result = convert_skill(source, registration.final_tick)
        if result is None:
            definition = make_definition(registration, registration.final_tick)
            write_definition(target, definition)
            report.append(dict(name=name, legacy_id=registration.legacy_id,
                               source=str(path), output=str(target),
                               status="execute_unparsed",
                               duration_ticks=registration.final_tick,
                               warnings=static_warnings + warnings))
            continue

        hitboxes = result["hitboxes"]
        projectiles = result["projectiles"]
        motions = result["motions"]
        sounds = v1.merge_coincident_sounds(result["sounds"], hitboxes, projectiles)
        event_warnings = result["warnings"]
        duration = result["duration"]

        if not hitboxes and not projectiles:
            event_warnings.append("攻撃イベントを抽出できませんでした。")
        blocked_prefixes = (
            "if条件を静的に評価できません", "静的評価を中断", "spawnAttackEffectを変換できません")
        legacy_fallback = not (hitboxes or projectiles) or \
            any(w.startswith(blocked_prefixes) for w in event_warnings)
        warnings = [w for w in (static_warnings + warnings + event_warnings)]
        if len(hitboxes) > 1 and not _has_tick_dispatch(source):
            warnings.append(
                f"FinalTick分岐がないため、Javaの毎tick生成({len(hitboxes)}回)をhitboxesに展開しました。"
                "ゲーム内で挙動を確認してください。")

        definition = make_definition(
            registration, duration, rush_start_ticks, sounds, projectiles,
            legacy_fallback, hitboxes, motions, None, result["color"])
        write_definition(target, definition)
        report.append({
            "name": name, "legacy_id": registration.legacy_id,
            "class": registration.skill_class, "source": str(path), "output": str(target),
            "status": "draft_created", "duration_ticks": duration,
            "detected_final_ticks": sorted({box["tick"] for box in hitboxes} |
                                           {projectile["tick"] for projectile in projectiles}),
            "warnings": warnings})
        converted += 1

    report_path = output / "conversion-report-v2.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                           encoding="utf-8")
    print(f"登録スキル数: {len(registrations)}")
    print(f"変換: {converted}")
    print(f"レポート: {report_path}")
    return 0


def _has_tick_dispatch(source: str) -> bool:
    return bool(re.search(r"\bFinalTick\s*==", source))


def main() -> int:
    project_root = Path(__file__).resolve().parents[2]
    registry = project_root / ("src/main/java/com/zoma1101/swordskill/swordskills/"
                               "SwordSkillRegistry.java")
    skill_dir = project_root / "src/main/java/com/zoma1101/swordskill/swordskills/skill"
    animations = project_root / "src/main/resources/assets/swordskill/player_animation"
    output = project_root / "build/skilldata_migration_v2"

    parser = argparse.ArgumentParser(description="v2型ベースのskilldata変換エンジン")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--skills", nargs="*", default=None)
    parser.add_argument("--registry", type=Path, default=registry)
    parser.add_argument("--skill-dir", type=Path, default=skill_dir)
    parser.add_argument("--animations", type=Path, default=animations)
    parser.add_argument("--output", type=Path, default=output)
    args = parser.parse_args()

    names = set(args.skills) if args.skills else None
    return convert_all(args.registry, args.skill_dir, args.animations, args.output,
                       args.force, names)


if __name__ == "__main__":
    sys.exit(main())
