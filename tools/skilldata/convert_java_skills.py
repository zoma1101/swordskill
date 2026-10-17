#!/usr/bin/env python3
"""Generate skilldata migration drafts from SwordSkillRegistry Java registrations."""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


WEAPON_TYPES = {
    "ONE_HANDED_SWORD",
    "TWO_HANDED_SWORD",
    "KATANA",
    "AXE",
    "RAPIER",
    "CLAW",
    "SPEAR",
    "MACE",
    "SCYTHE",
    "DAGGER",
    "DUALSWORD",
}
SKILL_TYPES = {"SIMPLE", "TRANSFORM", "TRANSFORM_FINISH", "RUSH"}
SOUND_HELPERS = {
    "SimpleSkillSound": ("minecraft:entity.player.attack.sweep", 1.47),
    "StrongSkillSound": ("minecraft:entity.player.attack.strong", 0.6),
    "GodSkillSound": ("minecraft:item.totem.use", 0.6),
}
TEXTURE_HELPERS = {
    "NomalSkillTexture": "skill_particle_sword/blue",
    "RedSkillTexture": "skill_particle_sword/red",
    "YellowSkillTexture": "skill_particle_sword/holy",
    "BlackSkillTexture": "skill_particle_sword/black",
    "PurpleSkillTexture": "skill_particle_sword/purple",
    "GreenSkillTexture": "skill_particle_sword/green",
    "GoldSkillTexture": "skill_particle_sword/gold",
    "AxeBlueSkillTexture": "axe_particle/simple_blue",
    "AxeGreenSkillTexture": "axe_particle/simple_green",
    "AxeBloodSkillTexture": "axe_particle/simple_blackred",
    "AxeRedSkillTexture": "axe_particle/simple_red",
    "AxeKingSkillTexture": "axe_particle/simple_king",
    "AxePinkSkillTexture": "axe_particle/simple_pink",
    "AxePurpleSkillTexture": "axe_particle/simple_purple",
    "Spia_Particle": "spear_particle/simple_blue",
    "Spia_Particle_red": "spear_particle/simple_red",
    "Spia_Particle_AxeGreen": "spear_particle/simple_green",
    "Spia_Particle_SoftRed": "spear_particle/simple_softred",
    "Spia_Particle_Purple": "spear_particle/simple_purple",
    "FlashingPenetrator_Texture": "spear_particle/super_blue",
    "MaceBlue_Texture": "mace_particle/blue",
    "MaceGreen_Texture": "mace_particle/green",
    "MacePurple_Texture": "mace_particle/purple",
    "BlueRollTexture": "skill_particle_sword/roll_blue",
}

# Texture Metadata
TEXTURE_COLORS = {
    "skill_particle_sword/blue": "#54D8FF",
    "skill_particle_sword/red": "#FF6B6B",
    "skill_particle_sword/holy": "#FFF3A0",
    "skill_particle_sword/black": "#4A4A5A",
    "skill_particle_sword/purple": "#B06BFF",
    "skill_particle_sword/green": "#77DD77",
    "skill_particle_sword/gold": "#FFD700",
    "skill_particle_sword/roll_blue": "#7FA8FF",
    "axe_particle/simple_blue": "#54D8FF",
    "axe_particle/simple_green": "#77DD77",
    "axe_particle/simple_blackred": "#FF3355",
    "axe_particle/simple_red": "#FF6B6B",
    "axe_particle/simple_king": "#FFD700",
    "axe_particle/simple_pink": "#FF9FD0",
    "axe_particle/simple_purple": "#B06BFF",
    "spear_particle/simple_blue": "#54D8FF",
    "spear_particle/simple_red": "#FF6B6B",
    "spear_particle/simple_green": "#77DD77",
    "spear_particle/simple_softred": "#FF9E8E",
    "spear_particle/simple_purple": "#B06BFF",
    "spear_particle/super_blue": "#6EE7FF",
    "mace_particle/blue": "#54D8FF",
    "mace_particle/green": "#77DD77",
    "mace_particle/purple": "#B06BFF",
}

# パーティクル種類ごとの追加ダメージ効果(AttackEffectEntity.ApplyDamage相当)
# 一部(low_hp_bonus / magic_bonus)はDataDrivenSkill側のSkillHitEffectRegistry拡張が必要
TEXTURE_EFFECTS = {
    "skill_particle_sword/holy": [{"type": "undead_bonus", "multiplier": 3.0}],
    "skill_particle_sword/black": [{"type": "undead_bonus", "multiplier": 2.5}],
    "skill_particle_sword/red": [
        {"type": "undead_bonus", "multiplier": 1.25},
        {"type": "lifesteal", "multiplier": 0.25},
    ],
    "axe_particle/simple_blackred": [{"type": "low_hp_bonus", "multiplier": 2.5}],
    "axe_particle/simple_king": [{"type": "magic_bonus", "multiplier": 0.5}],
    "skill_particle_sword/gold": [{"type": "magic_bonus", "multiplier": 0.25}],
    "mace_particle/blue": [{"type": "stun", "duration_ticks": 100, "amplifier": 1}],
    "mace_particle/green": [{"type": "stun", "duration_ticks": 100, "amplifier": 1}],
    "mace_particle/purple": [{"type": "stun", "duration_ticks": 100, "amplifier": 1}],
}

SKILL_TEXTURE_CLASS_RE = re.compile(r"\bSkillTexture\.(?P<set>\w+_ParticleType)\s*\.")

def load_known_sets(texture_path: Path) -> dict[str, set[str]]:
    """SkillTexture.javaのstaticブロックからパーティクル集合を解析する。"""
    try:
        source = strip_java_comments(texture_path.read_text(encoding="utf-8"))
    except OSError:
        return {}
    sets: dict[str, set[str]] = {}
    for name in ["Spia_ParticleType", "Axe_ParticleType", "Simple_ParticleType", "Mace_ParticleType"]:
        value = TEXTURE_HELPERS  # sanity: keep attribute alive
        sets[name] = set()
    static_match = re.search(r"\bstatic\s*\{(.*)", source, re.DOTALL)
    if not static_match:
        return sets
    for statement in re.finditer(r"(\w+_ParticleType)\s*\.\s*(?:add|remove)\s*\(\s*(\w+)\s*\(\s*\)\s*\)", static_match.group(1)):
        set_name, helper = statement.groups()
        if set_name in sets and helper in TEXTURE_HELPERS:
            sets[set_name].add(TEXTURE_HELPERS[helper])
    return sets

DEFAULT_KNOWN_SETS = {
    "Spia_ParticleType": {"spear_particle/simple_blue", "spear_particle/simple_red",
                           "spear_particle/simple_green", "spear_particle/simple_softred",
                           "spear_particle/simple_purple"},
    "Axe_ParticleType": {"axe_particle/simple_blue", "axe_particle/simple_green",
                         "axe_particle/simple_blackred", "axe_particle/simple_red",
                         "axe_particle/simple_king", "axe_particle/simple_pink",
                         "axe_particle/simple_purple"},
    "Simple_ParticleType": {"skill_particle_sword/blue", "skill_particle_sword/red",
                            "skill_particle_sword/holy", "skill_particle_sword/black",
                            "skill_particle_sword/purple", "skill_particle_sword/green",
                            "skill_particle_sword/gold"},
    "Mace_ParticleType": {"mace_particle/blue", "mace_particle/green", "mace_particle/purple"},
}


@dataclass(frozen=True)
class SkillRegistration:
    legacy_id: int
    name: str
    cooldown: int
    sp_cost: float
    skill_type: str
    skill_class: str
    weapons: list[str]
    hidden: bool
    final_tick: int
    transform_limit_ticks: int


def strip_java_comments(source: str) -> str:
    result: list[str] = []
    index = 0
    in_string = False
    in_char = False
    escaped = False
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if in_string or in_char:
            result.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif (in_string and char == '"') or (in_char and char == "'"):
                in_string = False
                in_char = False
            index += 1
        elif char == '"':
            in_string = True
            result.append(char)
            index += 1
        elif char == "'":
            in_char = True
            result.append(char)
            index += 1
        elif char == "/" and next_char == "/":
            index += 2
            while index < len(source) and source[index] not in "\r\n":
                index += 1
        elif char == "/" and next_char == "*":
            index += 2
            while index + 1 < len(source) and source[index : index + 2] != "*/":
                if source[index] in "\r\n":
                    result.append(source[index])
                index += 1
            index = min(index + 2, len(source))
        else:
            result.append(char)
            index += 1
    return "".join(result)


def matching_parenthesis(source: str, opening: int) -> int:
    depth = 0
    in_string = False
    escaped = False
    for index in range(opening, len(source)):
        char = source[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("unclosed Java constructor call")


def split_java_arguments(source: str) -> list[str]:
    arguments: list[str] = []
    start = 0
    stack: list[str] = []
    pairs = {")": "(", "]": "[", "}": "{"}
    in_string = False
    escaped = False
    for index, char in enumerate(source):
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        elif char == '"':
            in_string = True
        elif char in "([{":
            stack.append(char)
        elif char in ")]}":
            if stack and stack[-1] == pairs[char]:
                stack.pop()
        elif char == "," and not stack:
            arguments.append(source[start:index].strip())
            start = index + 1
    tail = source[start:].strip()
    if tail:
        arguments.append(tail)
    return arguments


def integer_literal(expression: str, label: str) -> int:
    match = re.fullmatch(r"\(?\s*(-?\d+)\s*\)?", expression.strip())
    if not match:
        raise ValueError(f"{label} is not a supported integer literal: {expression}")
    return int(match.group(1))


def number_literal(expression: str, label: str) -> float:
    normalized = expression.strip().rstrip("fFdD")
    try:
        value = float(normalized)
    except ValueError as error:
        raise ValueError(f"{label} is not a supported number literal: {expression}") from error
    if value < 0:
        raise ValueError(f"{label} cannot be negative: {expression}")
    return value


def parse_weapon_list(expression: str) -> list[str]:
    weapon_match = re.fullmatch(r"List\.of\((.*)\)", expression.strip(), re.DOTALL)
    if not weapon_match:
        raise ValueError(f"unsupported weapon list: {expression}")
    weapons: list[str] = []
    for item in split_java_arguments(weapon_match.group(1)):
        weapon = item.rsplit(".", 1)[-1]
        if weapon not in WEAPON_TYPES:
            raise ValueError(f"unsupported weapon type: {item}")
        weapons.append(weapon)
    return weapons


def parse_registration(
    arguments: list[str], all_weapons: list[str], default_transform_limit: int, legacy_id: int
) -> SkillRegistration:
    if len(arguments) != 10:
        raise ValueError(f"expected 10 SkillData constructor arguments, found {len(arguments)}")
    if arguments[0].strip() != "getNextSkillId()":
        raise ValueError(f"unsupported legacy ID expression: {arguments[0]}")

    name_match = re.fullmatch(r'"([^"]+)"', arguments[1].strip())
    type_match = re.search(r"SkillType\.(\w+)", arguments[4])
    class_match = re.fullmatch(r"([\w.]+)\.class", arguments[5].strip())
    weapon_expression = arguments[6].strip()
    hidden_match = re.fullmatch(r"(true|false)", arguments[7].strip())
    if not name_match or not type_match or not class_match or not hidden_match:
        raise ValueError("constructor contains fields that cannot be statically converted")

    skill_type = type_match.group(1)
    if skill_type not in SKILL_TYPES:
        raise ValueError(f"unsupported skill type: {skill_type}")
    weapons = list(all_weapons) if weapon_expression == "AllWeapons" else parse_weapon_list(weapon_expression)
    if not weapons:
        raise ValueError("weapon list is empty")

    return SkillRegistration(
        legacy_id=legacy_id,
        name=name_match.group(1),
        cooldown=integer_literal(arguments[2], "cooldown"),
        sp_cost=number_literal(arguments[3], "sp cost"),
        skill_type=skill_type,
        skill_class=class_match.group(1).rsplit(".", 1)[-1],
        weapons=weapons,
        hidden=hidden_match.group(1) == "true",
        final_tick=integer_literal(arguments[8], "final tick"),
        transform_limit_ticks=(
            default_transform_limit
            if arguments[9].strip() == "DefaultTransformTick"
            else integer_literal(arguments[9], "transform limit")
        ),
    )


def load_registrations(registry_path: Path) -> list[SkillRegistration]:
    source = strip_java_comments(registry_path.read_text(encoding="utf-8"))
    all_weapons_match = re.search(
        r"\bAllWeapons\s*=\s*(List\.of\([^;]+\))\s*;",
        source,
        re.DOTALL,
    )
    if not all_weapons_match:
        raise ValueError("cannot find the AllWeapons list in the registry")
    all_weapons = parse_weapon_list(all_weapons_match.group(1))
    transform_limit_match = re.search(
        r"\bDefaultTransformTick\s*=\s*(\d+)\s*;",
        source,
    )
    if not transform_limit_match:
        raise ValueError("cannot find the DefaultTransformTick value in the registry")
    default_transform_limit = int(transform_limit_match.group(1))
    registrations: list[SkillRegistration] = []
    cursor = 0
    marker = "registerSkill(new SkillData("
    while True:
        start = source.find(marker, cursor)
        if start < 0:
            break
        opening = start + len("registerSkill(new SkillData")
        closing = matching_parenthesis(source, opening)
        try:
            registrations.append(
                parse_registration(
                    split_java_arguments(source[opening + 1 : closing]),
                    all_weapons,
                    default_transform_limit,
                    len(registrations),
                )
            )
        except ValueError as error:
            constructor = source[opening + 1 : closing]
            summary = re.sub(r"\s+", " ", constructor).strip()
            raise ValueError(f"cannot parse registration near {summary}: {error}") from error
        cursor = closing + 1
    if not registrations:
        raise ValueError(f"no legacy SkillData registrations found in {registry_path}")
    return registrations


def inspect_skill_source(skill_dir: Path, skill_class: str) -> tuple[Path | None, list[str], list[str]]:
    matches = list(skill_dir.rglob(f"{skill_class}.java"))
    if not matches:
        return None, [], ["Java実装クラスが見つからず、攻撃・移動処理を確認できません。"]
    if len(matches) > 1:
        return None, [], [f"クラス名 {skill_class} のJavaファイルが複数あり、対象を特定できません。"]

    path = matches[0]
    source = strip_java_comments(path.read_text(encoding="utf-8"))
    ticks = sorted({int(value) for value in re.findall(r"\bFinalTick\s*==\s*(\d+)", source)})
    warnings = [
        "単純なspawnAttackEffectと既知の効果音呼び出し以外のJava固有処理は自動変換されません。"
        "SimpleSkillSound/StrongSkillSound/GodSkillSound以外の音も手動移植が必要です。"
    ]
    if "spawnAttackEffect(" in source:
        warnings.append(
            "旧形式の攻撃エフェクト生成(spawnAttackEffect)を検出しました。自動抽出したprojectilesと"
            "legacy_fallbackの設定を確認してください。"
        )
    if "setDeltaMovement(" in source:
        warnings.append("プレイヤー移動処理(setDeltaMovement)を検出しました。motionsへの手動変換が必要です。")
    if "addEffect(" in source or "MobEffectInstance" in source:
        warnings.append("状態効果の処理を検出しました。skilldataのeffectsへ手動変換してください。")
    if "PlayAnimationPayload" in source:
        warnings.append("独自アニメーション切替を検出しました。skilldataのアニメーション設定へ手動移植してください。")
    return path, ticks, warnings


def extract_sound_events(source: str) -> list[dict[str, Any]]:
    events: dict[tuple[int, str], dict[str, Any]] = {}
    condition = re.compile(r"\bFinalTick\s*==\s*(\d+)")
    for match in condition.finditer(source):
        opening = source.find("{", match.end())
        if opening < 0:
            continue
        depth = 1
        cursor = opening + 1
        while cursor < len(source) and depth:
            if source[cursor] == "{":
                depth += 1
            elif source[cursor] == "}":
                depth -= 1
            cursor += 1
        if depth:
            continue
        body = source[opening + 1 : cursor - 1]
        tick = int(match.group(1))
        for helper, (sound, pitch) in SOUND_HELPERS.items():
            for _ in re.finditer(rf"\b{helper}\s*\(", body):
                events[(tick, sound)] = {
                    "tick": tick,
                    "sound": sound,
                    "volume": 1.0,
                    "pitch": pitch,
                }
    return sorted(events.values(), key=lambda event: (event["tick"], event["sound"]))


def merge_coincident_sounds(sounds: list[dict[str, Any]], hitboxes: list[dict[str, Any]],
                            projectiles: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Embed a same-tick sound in its attack event to avoid duplicate timeline entries."""
    attack_events = sorted([*hitboxes, *projectiles], key=lambda event: event["tick"])
    remaining = []
    for sound in sounds:
        attack = next((event for event in attack_events
                       if event["tick"] == sound["tick"] and not event.get("sound")), None)
        if attack is None:
            remaining.append(sound)
        else:
            attack["sound"] = sound["sound"]
    return remaining


def _block_after(source: str, start: int) -> tuple[str, int]:
    opening = source.find("{", start)
    if opening < 0:
        raise ValueError("Java block opening brace not found")
    depth = 1
    cursor = opening + 1
    while cursor < len(source) and depth:
        if source[cursor] == "{":
            depth += 1
        elif source[cursor] == "}":
            depth -= 1
        cursor += 1
    if depth:
        raise ValueError("unclosed Java block")
    return source[opening + 1 : cursor - 1], cursor


def _method_bodies(source: str) -> dict[str, tuple[list[str], str]]:
    methods: dict[str, tuple[list[str], str]] = {}
    pattern = re.compile(
        r"\b(?:public|protected|private)\s+(?:static\s+)?[\w.<>, ?]+\s+"
        r"(\w+)\s*\(([^)]*)\)\s*\{"
    )
    for match in pattern.finditer(source):
        if match.group(1) == "execute":
            continue
        try:
            body, _ = _block_after(source, match.end() - 1)
        except ValueError:
            continue
        parameters = []
        for parameter in split_java_arguments(match.group(2)):
            parts = parameter.rsplit(None, 1)
            if len(parts) == 2:
                parameters.append(parts[1].strip())
        methods[match.group(1)] = (parameters, body)
    return methods


def _substitute(expression: str, values: dict[str, str]) -> str:
    for name in sorted(values, key=len, reverse=True):
        val = values[name]
        # Expand Vec2 / Vec3 property access directly during substitution so
        # downstream numeric evaluators can see plain arithmetic expressions.
        vec2_match = re.fullmatch(r"new\s+Vec2\s*\((.*)\)", val.strip(), re.DOTALL)
        if vec2_match:
            args = split_java_arguments(vec2_match.group(1))
            if len(args) == 2:
                expression = re.sub(rf"\b{re.escape(name)}\.x\b", f"({args[0]})", expression)
                expression = re.sub(rf"\b{re.escape(name)}\.y\b", f"({args[1]})", expression)
        vec3_match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", val.strip(), re.DOTALL)
        if vec3_match:
            args = split_java_arguments(vec3_match.group(1))
            if len(args) == 3:
                expression = re.sub(rf"\b{re.escape(name)}\.x\b", f"({args[0]})", expression)
                expression = re.sub(rf"\b{re.escape(name)}\.y\b", f"({args[1]})", expression)
                expression = re.sub(rf"\b{re.escape(name)}\.z\b", f"({args[2]})", expression)
        expression = re.sub(rf"\b{re.escape(name)}\b", f"({val})", expression)
    return expression


def _unwrap(expression: str) -> str:
    expression = expression.strip()
    while expression.startswith("(") and expression.endswith(")"):
        try:
            if matching_parenthesis(expression, 0) != len(expression) - 1:
                break
        except ValueError:
            break
        expression = expression[1:-1].strip()
    return expression


def _number(expression: str) -> float:
    expression = re.sub(r"\(\s*(?:float|double|int|long)\s*\)", "", expression)
    value = _unwrap(expression).rstrip("fFdD")
    if not re.fullmatch(r"[-+0-9.eE ()*/]+", value):
        raise ValueError(f"unsupported numeric expression: {expression}")
    return float(eval(value, {"__builtins__": {}}, {}))


def _resolve_variables(body: str, initial: dict[str, str]) -> dict[str, str]:
    values = dict(initial)
    declaration = re.compile(
        r"(?:final\s+)?[\w.<>, ?]+\s+(\w+)\s*=\s*([^;]+);",
    )
    for match in declaration.finditer(body):
        name, expression = match.groups()
        if name in {"if", "return", "new"}:
            continue
        values[name] = _substitute(expression.strip(), values)
    # Also capture Vec2 constructor assignments
    assignment = re.compile(r"\b(\w+)\s*=\s*(new\s+(?:Vec3|Vec2)\s*\([^;]+\))\s*;")
    for match in assignment.finditer(body):
        values.setdefault(match.group(1), _substitute(match.group(2).strip(), values))
    switch_assignment = re.compile(r"(?:[\w.<>, ?]+\s+)?(\w+)\s*=\s*switch\s*\(")
    for match in switch_assignment.finditer(body):
        target = match.group(1)
        switch_start = body.find("switch", match.start(1) + len(target))
        brace = body.find("{", switch_start)
        if brace < 0:
            continue
        try:
            _, end = _block_after(body, brace)
        except ValueError:
            continue
        values[target] = body[switch_start:end].strip()
    return values


def _select_switch(expression: str, values: dict[str, str]) -> str | None:
    expression = expression.strip()
    if not expression.startswith("switch"):
        return None
    opening = expression.find("(")
    if opening < 0:
        return None
    closing = matching_parenthesis(expression, opening)
    selector = _number(_substitute(expression[opening + 1:closing], values))
    brace = expression.find("{", closing)
    if brace < 0:
        return None
    switch_body, _ = _block_after(expression, brace)
    selected = ""
    for labels, candidate in re.findall(r"case\s+([\d,\s-]+)\s*->\s*([^;]+);?", switch_body):
        if int(selector) in [int(label.strip()) for label in labels.split(",") if label.strip()]:
            selected = candidate
            break
    if not selected:
        default = re.search(r"default\s*->\s*([^;]+);?", switch_body)
        selected = default.group(1) if default else ""
    return _substitute(selected.strip(), values) if selected else None


def _resolve_expression(expression: str, values: dict[str, str], methods: dict[str, tuple[list[str], str]]) -> str:
    expression = _unwrap(_substitute(expression.strip(), values))
    for helper, particle in TEXTURE_HELPERS.items():
        if re.fullmatch(rf"{helper}\s*\(\s*\)", expression):
            return f'"{particle}"'
    selected_switch = _select_switch(expression, values)
    if selected_switch:
        return _resolve_expression(selected_switch, values, methods)
    ternary = re.fullmatch(r"(.+?)\?\s*(.+?)\s*:\s*(.+)", expression, re.DOTALL)
    if ternary:
        condition = _unwrap(ternary.group(1).strip())
        selected = ""
        if condition in {"true", "false"}:
            selected = ternary.group(2) if condition == "true" else ternary.group(3)
        elif "==" in condition:
            left, right = condition.split("==", 1)
            try:
                selected = ternary.group(2) if _number(left) == _number(right) else ternary.group(3)
            except ValueError:
                pass
        if selected:
            return _resolve_expression(selected.strip(), values, methods)
    switch_call = re.fullmatch(r"(\w+)\s*\((.*)\)", expression, re.DOTALL)
    if switch_call and switch_call.group(1) in methods:
        parameters, body = methods[switch_call.group(1)]
        arguments = split_java_arguments(switch_call.group(2))
        if len(arguments) == len(parameters):
            nested = dict(zip(parameters, arguments))
            # Resolve all simple switch assignments first. Helpers commonly
            # select a yaw, then a relative vector; treating the first switch
            # as the helper's return value silently loses the actual position.
            local_values = _resolve_variables(body, nested)
            switch_assignments = re.finditer(
                r"(?:[\w.<>, ?]+\s+)?(\w+)\s*=\s*switch\s*\(([^)]+)\)\s*\{(.*?)\}\s*;",
                body, re.DOTALL,
            )
            for selector in switch_assignments:
                target, selector_name, switch_body = selector.groups()
                selector_value = int(_number(_substitute(selector_name, local_values)))
                selected_expression = ""
                for labels, candidate in re.findall(
                    r"case\s+([\d,\s-]+)\s*->\s*([^;]+);?", switch_body
                ):
                    if selector_value in [int(label.strip()) for label in labels.split(",") if label.strip()]:
                        selected_expression = candidate
                        break
                if not selected_expression:
                    default = re.search(r"default\s*->\s*([^;]+);?", switch_body)
                    selected_expression = default.group(1) if default else ""
                if selected_expression:
                    local_values[target] = _substitute(selected_expression.strip(), local_values)

            returned_switch = re.search(r"\breturn\s+switch\s*\(", body)
            if returned_switch:
                switch_start = returned_switch.start() + len("return ")
                selected = _select_switch(body[switch_start:], local_values)
                if selected:
                    return _resolve_expression(selected, local_values, methods)
            returned = re.search(r"\breturn\s+(.+?)\s*;", body, re.DOTALL)
            if returned:
                return_expression = _substitute(returned.group(1).strip(), local_values)
                direct_switch = _select_switch(return_expression, local_values)
                if direct_switch:
                    return _resolve_expression(direct_switch, local_values, methods)
                return return_expression
    return expression


def _vector(expression: str, constructor: str, values: dict[str, str],
            methods: dict[str, tuple[list[str], str]]) -> list[float]:
    expression = _unwrap(_resolve_expression(expression, values, methods))
    direct_switch = _select_switch(expression, values)
    if direct_switch:
        expression = _unwrap(direct_switch)
    ternary = re.fullmatch(r"(.+?)\?\s*(new\s+\w+\s*\(.*?\)|(?:[\w.]+\.)?Vec3\.ZERO)\s*:\s*(new\s+\w+\s*\(.*?\)|(?:[\w.]+\.)?Vec3\.ZERO)",
                           expression, re.DOTALL)
    if ternary:
        condition = _unwrap(ternary.group(1).strip())
        equals = re.fullmatch(r"(.+?)\s*==\s*(-?\d+)", condition)
        if equals:
            actual = _number(equals.group(1))
            expression = ternary.group(2) if actual == int(equals.group(2)) else ternary.group(3)
        elif condition in {"true", "false"}:
            expression = ternary.group(2) if condition == "true" else ternary.group(3)
    if expression in {"Vec3.ZERO", "net.minecraft.world.phys.Vec3.ZERO"}:
        return [0.0, 0.0, 0.0]
    match = re.fullmatch(rf"new\s+{constructor}\s*\((.*)\)", expression.strip(), re.DOTALL)
    if not match:
        raise ValueError(f"unsupported {constructor} expression: {expression}")
    components = split_java_arguments(match.group(1))
    if len(components) != 3:
        raise ValueError(f"{constructor} requires three components")
    return [_number(_substitute(component, values)) for component in components]


def _rotation_vector(expression: str, values: dict[str, str],
                     methods: dict[str, tuple[list[str], str]]) -> tuple[list[float], float, float]:
    expression = _unwrap(_resolve_expression(expression, values, methods))
    match = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", expression, re.DOTALL)
    if not match:
        return _vector(expression, "Vec3", values, methods), 0.0, 0.0
    components = split_java_arguments(match.group(1))
    if len(components) != 3:
        raise ValueError("Vec3 rotation requires three components")
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
        rotation.append(_number(static_expression))
        pitch_multipliers.append(multiplier)
    if abs(pitch_multipliers[1]) > 1.0e-9:
        raise ValueError("player pitch adjustment on rotation Y is not supported")
    return rotation, pitch_multipliers[0], pitch_multipliers[2]


def _position_offset(expression: str, values: dict[str, str],
                     methods: dict[str, tuple[list[str], str]]) -> list[float]:
    expression = _resolve_expression(expression, values, methods)
    expression = _unwrap(_substitute(expression.strip(), values))
    if re.fullmatch(r"(?:[\w.]+\.)?Vec3\.ZERO", expression):
        return [0.0, 0.0, 0.0]
    literal_vector = re.fullmatch(r"new\s+Vec3\s*\((.*)\)", expression, re.DOTALL)
    if literal_vector:
        components = split_java_arguments(literal_vector.group(1))
        if len(components) == 3:
            return [_number(component) for component in components]
    if re.fullmatch(r"[-+\d.eE()*/ fFdD]+", expression):
        return [0.0, 0.0, _number(expression)]

    forward = 0.0
    right = 0.0
    vertical = 0.0
    for vector_name, axis in (("lookVec", "forward"), ("rightVec", "right"), ("upVec", "up")):
        for match in re.finditer(rf"\b{vector_name}\.scale\s*\(([^()]*)\)", expression):
            amount = _number(match.group(1))
            before = expression[max(0, match.start() - 20):match.start()]
            if re.search(r"\.subtract\s*\(\s*$", before):
                amount = -amount
            if axis == "forward":
                forward += amount
            elif axis == "right":
                right += amount
            else:
                vertical += amount
    for match in re.finditer(
        r"\.cross\s*\(\s*new\s+Vec3\s*\(\s*0\s*,\s*1\s*,\s*0\s*\)\s*\)\s*\.normalize\(\)\s*\.scale\s*\(([^()]*)\)",
        expression,
    ):
        amount = _number(match.group(1))
        before = expression[:match.start()]
        if before.rfind(".subtract(") > before.rfind(".add("):
            amount = -amount
        right += amount
    for match in re.finditer(
        r"\.cross\s*\(\s*(?:\(*[\w().]+getLookAngle\s*\(\)\)*|lookVec)\s*\)\s*\.normalize\(\)\s*\.scale\s*\(([^()]*)\)",
        expression,
    ):
        amount = _number(match.group(1))
        before = expression[:match.start()]
        if before.rfind(".subtract(") > before.rfind(".add("):
            amount = -amount
        vertical += amount
    look_scale = re.search(r"(?:lookVec|[\w().]+getLookAngle\s*\(\))\.scale\s*\(([^()]*)\)", expression)
    if look_scale and forward == 0:
        forward = _number(look_scale.group(1))
    rotated_look = re.search(r"rotateLookVec\s*\([^,]+,\s*([^,]+),\s*([^\)]+)\)\.scale\s*\(([^()]*)\)", expression)
    if rotated_look:
        pitch = math.radians(_number(rotated_look.group(1)))
        yaw = math.radians(_number(rotated_look.group(2)))
        distance = _number(rotated_look.group(3))
        forward += distance * math.cos(pitch) * math.cos(yaw)
        right += distance * math.cos(pitch) * math.sin(yaw)
        vertical += -distance * math.sin(pitch)
    eye_height = re.search(r"getEyeHeight\(\)\s*\*\s*([0-9.]+)[fFdD]?", expression)
    if eye_height:
        vertical = 1.62 * float(eye_height.group(1))
    else:
        if re.search(r"\.getEyeHeight\s*\(", expression):
            raise ValueError(f"unsupported eye-height position expression: {expression}")
        literal_height = re.search(r"\.add\s*\(\s*0\s*,\s*(-?[0-9.]+)[fFdD]?\s*,\s*0\s*\)", expression)
        if literal_height:
            vertical = float(literal_height.group(1))
    for match in re.finditer(r"\.add\s*\(\s*0\s*,\s*([^,]+),\s*0\s*\)", expression):
        height_expression = match.group(1).strip()
        if "getEyeHeight" not in height_expression:
            vertical += _number(height_expression)
    return [right, vertical, forward]


def _projectile_from_call(arguments: list[str], tick: int,
                          methods: dict[str, tuple[list[str], str]],
                          values: dict[str, str]) -> dict[str, Any]:
    if len(arguments) != 10:
        raise ValueError(f"spawnAttackEffect expected 10 arguments, found {len(arguments)}")
    position = _substitute(arguments[1], values)
    rotation, pitch_x_multiplier, pitch_z_multiplier = _rotation_vector(arguments[2], values, methods)
    size = _vector(arguments[3], "Vector3f", values, methods)
    damage_expression = _unwrap(_resolve_expression(arguments[5], values, methods))
    damage_match = re.search(r"(?:BaseDamage|RushDamage)\s*\(.*?\)(?:\s*\*\s*(.+))?$", damage_expression)
    if not damage_match:
        raise ValueError(f"unsupported damage expression: {damage_expression}")
    damage_multiplier = _number(damage_match.group(1)) if damage_match.group(1) else 1.0
    damage_type = "RUSH" if "RushDamage" in damage_expression else "BASE"
    knockback_expression = _unwrap(_resolve_expression(arguments[6], values, methods))
    knockback_match = re.search(r"BaseKnowBack\s*\(.*?\)\s*\*\s*(.+)$", knockback_expression)
    if knockback_match:
        knockback = _number(knockback_match.group(1))
    elif re.fullmatch(r"BaseKnowBack\s*\(.*?\)", knockback_expression):
        knockback = 1.0
    else:
        try:
            knockback = _number(knockback_expression)
        except ValueError as error:
            raise ValueError(f"unsupported knockback expression: {knockback_expression}") from error
    duration_expression = _substitute(arguments[7], values)
    duration = int(_number(duration_expression))
    texture = _unwrap(_resolve_expression(arguments[8], values, methods)).strip()
    texture_match = re.fullmatch(r'"([^"]+)"', texture)
    if not texture_match:
        raise ValueError(f"unsupported attack texture expression: {texture}")
    event = {
        "tick": tick,
        "offset": _position_offset(position, values, methods),
        "rotation": rotation,
        "size": size,
        "damage_multiplier": damage_multiplier,
        "damage_type": damage_type,
        "knockback": knockback,
        "knockback_multiplier": knockback if "BaseKnowBack" in knockback_expression else -1,
        "duration_ticks": duration,
        "travel_distance": 0,
        "particle": texture_match.group(1),
        "sound": "",
    }
    if abs(pitch_x_multiplier) > 1.0e-9:
        event["player_pitch_x_multiplier"] = pitch_x_multiplier
    if abs(pitch_z_multiplier) > 1.0e-9:
        event["player_pitch_z_multiplier"] = pitch_z_multiplier
    movement = _movement_vector(arguments[9], values, methods)
    if any(abs(component) >= 1.0e-9 for component in movement):
        event["movement"] = movement
    return event


def _is_zero_movement(expression: str, values: dict[str, str],
                      methods: dict[str, tuple[list[str], str]]) -> bool:
    try:
        return all(abs(component) < 1.0e-9
                   for component in _movement_vector(expression, values, methods))
    except ValueError:
        return False


def _hitbox_from_attack_effect(projectile: dict[str, Any]) -> dict[str, Any]:
    return {
        "tick": projectile["tick"],
        "hitbox_enabled": True,
        "offset": projectile["offset"],
        "size": projectile["size"],
        "damage_multiplier": projectile["damage_multiplier"],
        "damage_type": projectile["damage_type"],
        "knockback": projectile["knockback"],
        "knockback_multiplier": projectile["knockback_multiplier"],
        "effects": [],
        "sound": "",
    }


def extract_touch_rush_events(source: str, duration: int) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[str]]:
    """Translate the built-in one-burst, proximity-triggered rush pattern."""
    required_patterns = (
        r"getEntitiesOfClass\s*\(\s*LivingEntity\.class",
        r"\.distanceTo\s*\([^)]*\)\s*<\s*1\.5",
        r"setDeltaMovement\s*\(",
        r"getPersistentData\s*\(\)\.putBoolean",
    )
    if not all(re.search(pattern, source) for pattern in required_patterns):
        return [], [], []

    execute_match = re.search(r"\bvoid\s+execute\s*\([^)]*\)\s*\{", source)
    if not execute_match:
        return [], [], ["接触型RUSHのexecuteメソッドを解析できません。"]
    try:
        execute_body, _ = _block_after(source, execute_match.end() - 1)
    except ValueError:
        return [], [], ["接触型RUSHのexecuteブロックを解析できません。"]

    methods = _method_bodies(source)
    tick_one = re.search(r"\bFinalTick\s*==\s*1\s*\)", execute_body)
    if not tick_one:
        return [], [], ["接触型RUSHの開始tickを特定できません。"]
    try:
        movement_body, _ = _block_after(execute_body, tick_one.end())
    except ValueError:
        return [], [], ["接触型RUSHの開始tickブロックを解析できません。"]

    movement_call = re.search(r"player\.setDeltaMovement\s*\((.*?)\)\s*;", movement_body, re.DOTALL)
    if not movement_call:
        return [], [], ["接触型RUSHの初期移動を特定できません。"]
    movement_args = split_java_arguments(movement_call.group(1))
    if len(movement_args) != 3:
        return [], [], ["接触型RUSHのsetDeltaMovement形式は未対応です。"]

    vector_scale = re.search(
        r"(?:Vec3|var)\s+(\w+)\s*=\s*(?:lookVec|player\.getLookAngle\s*\(\))\s*\.scale\s*\(([^()]*)\)",
        movement_body,
    )
    if not vector_scale:
        return [], [], ["接触型RUSHの向きベクトル倍率を特定できません。"]
    vector_name, speed_expression = vector_scale.groups()
    movement_values = _resolve_variables(movement_body, {})
    try:
        speed = _number(_substitute(speed_expression, movement_values))
    except ValueError as error:
        return [], [], [f"接触型RUSHの移動速度を変換できません: {error}"]
    x_expression, y_expression, z_expression = [arg.strip() for arg in movement_args]
    if not (re.fullmatch(rf"{re.escape(vector_name)}\.x", x_expression)
            and re.fullmatch(rf"{re.escape(vector_name)}\.z", z_expression)):
        return [], [], ["接触型RUSHの水平移動が look-vector 以外の形式です。"]
    vertical = 0.0
    y_vector = re.match(rf"{re.escape(vector_name)}\.y\s*([+-])\s*(.+)", y_expression)
    if re.fullmatch(rf"{re.escape(vector_name)}\.y", y_expression):
        vertical = 0.0
    elif y_vector:
        try:
            amount = _number(y_vector.group(2))
        except ValueError as error:
            return [], [], [f"接触型RUSHの垂直移動を変換できません: {error}"]
        vertical = amount if y_vector.group(1) == "+" else -amount
    else:
        try:
            vertical = _number(y_expression)
        except ValueError as error:
            return [], [], [f"接触型RUSHの垂直移動を変換できません: {error}"]
    motions = [{"tick": 1, "forward": speed, "strafe": 0.0, "vertical": vertical}]

    attack_calls = list(re.finditer(r"\bspawnAttackEffect\s*\(", execute_body))
    # If execute_body has no direct spawnAttackEffect, look inside called helper methods
    resolved_attack_body = execute_body
    resolved_attack_values_source = execute_body
    if not attack_calls:
        for method_name, (parameters, method_body) in methods.items():
            if "spawnAttackEffect" not in method_body:
                continue
            helper_spawn_calls = list(re.finditer(r"\bspawnAttackEffect\s*\(", method_body))
            if len(helper_spawn_calls) != 1:
                continue
            # Find a call to this helper in execute_body
            helper_call = re.search(rf"\b{re.escape(method_name)}\s*\(", execute_body)
            if not helper_call:
                continue
            try:
                hend = matching_parenthesis(execute_body, helper_call.end() - 1)
                call_args = split_java_arguments(execute_body[helper_call.end():hend])
                if len(call_args) != len(parameters):
                    continue
                resolved_args = [_substitute(a, _resolve_variables(execute_body, {}))
                                 for a in call_args]
                attack_calls = helper_spawn_calls
                resolved_attack_body = method_body
                resolved_attack_values_source = _resolve_variables(
                    method_body, dict(zip(parameters, resolved_args)))
            except ValueError:
                continue
            break

    if len(attack_calls) != 1:
        return motions, [], ["接触型RUSHの攻撃エフェクトが1件に特定できず、攻撃判定を抽出できません。"]
    values = (resolved_attack_values_source if isinstance(resolved_attack_values_source, dict)
              else _resolve_variables(resolved_attack_body, {}))
    try:
        call = attack_calls[0]
        end = matching_parenthesis(resolved_attack_body, call.end() - 1)
        arguments = split_java_arguments(resolved_attack_body[call.end():end])
        projectile = _projectile_from_call(arguments, 1, methods, values)
        # Accept near-stationary movement (e.g. Vec3(0,0,0.1)) as a hitbox
        is_near_zero = False
        try:
            movement = _movement_vector(arguments[9], values, methods)
            is_near_zero = all(abs(c) < 1.0 for c in movement)
        except ValueError:
            pass
        if not is_near_zero and not _is_zero_movement(arguments[9], values, methods):
            return motions, [], ["接触型RUSHの攻撃エフェクトに移動があり、攻撃判定へ変換できません。"]
        base_hitbox = _hitbox_from_attack_effect(projectile)
    except ValueError as error:
        return motions, [], [f"接触型RUSHの攻撃範囲を変換できません: {error}"]

    # The original class checks contact every tick and stops on its first hit.
    # A repeat-while-rushing hitbox retries contact without duplicating timeline
    # events, and the data-driven RUSH ends as soon as one attack lands.
    base_hitbox["tick"] = 1
    base_hitbox["repeat_while_rushing"] = True
    hitboxes = [base_hitbox]
    warnings = [
        "接触型RUSHを移動1件と各tickの攻撃判定に変換しました。元Javaの接触距離条件とは当たり範囲が異なるため、Blockbenchで範囲を調整してください。",
        "skilldataの移動イベントは現在速度への加算です。元JavaのsetDeltaMovementによる速度置換とは結果が異なる場合があります。",
        "spawnAttackEffectの見た目はhitboxイベントでは再現されません。斬撃エフェクトは必要に応じてBlockbenchでFlying Slashを追加してください。",
        "元Javaの無敵時間・状態効果・接地時の早期終了など、skilldataで表現できない処理は変換されません。",
    ]
    return motions, hitboxes, warnings


def extract_contact_combo_rush(source: str, duration: int) -> tuple[dict[str, Any] | None, list[dict[str, Any]], list[str]]:
    """Extract a contact-triggered, multi-hit RUSH combo such as DoubleCircular."""
    required = ("ATTACK_FLAG", "NOW_TICK", "FINAL_TICK", "invulnerableTime")
    if not all(token in source for token in required) or "FinalTick - nowTick" not in source:
        return None, [], []
    execute_match = re.search(r"\bvoid\s+execute\s*\([^)]*\)\s*\{", source)
    if not execute_match:
        return None, [], ["連撃RUSHのexecuteメソッドを解析できませんでした。"]
    try:
        execute_body, _ = _block_after(source, execute_match.end() - 1)
    except ValueError:
        return None, [], ["連撃RUSHのexecuteブロックを解析できませんでした。"]

    methods = _method_bodies(source)
    move_match = re.search(
        r"(?:double|float)\s+moveSpeed\s*=\s*([^;]+);[\s\S]*?"
        r"Vec3\s+moveVec\s*=\s*limitLookVec\(player\)\.scale\(moveSpeed\);[\s\S]*?"
        r"player\.setDeltaMovement\(moveVec\.x,\s*moveVec\.y\s*\+\s*([^,]+),\s*moveVec\.z\)",
        execute_body,
    )
    search_radius = re.search(r"getBoundingBox\(\)\.inflate\(([^)]+)\)", execute_body)
    contact_radius = re.search(r"distanceTo\([^)]*\)\s*<\s*([0-9.]+)", execute_body)
    invulnerability = re.search(r"invulnerableTime\s*=\s*(\d+)", execute_body)
    reverse_speed = re.search(r"getLookAngle\(\)\.scale\(([^)]+)\)\.reverse\(\)", execute_body)
    start_animation_tick = re.search(r"FinalTick\s*==\s*(\d+)\s*\)\s*\{\s*PacketDistributor[\s\S]{0,180}?\"move\"", execute_body)
    if not all((move_match, search_radius, contact_radius, invulnerability, reverse_speed, start_animation_tick)):
        return None, [], ["連撃RUSHの移動・接触・phase設定が未対応の形式です。"]

    events: list[dict[str, Any]] = []
    warnings: list[str] = []
    trigger_blocks = list(re.finditer(r"FinalTick\s*-\s*nowTick\s*==\s*(\d+)\s*\)", execute_body))
    for condition in trigger_blocks:
        try:
            block, _ = _block_after(execute_body, condition.end())
        except ValueError:
            continue
        offset_tick = int(condition.group(1))
        calls = list(re.finditer(r"\b(performSlash|performThrust)\s*\(", block))
        for call in calls:
            method_name = call.group(1)
            method = methods.get(method_name)
            if not method:
                warnings.append(f"接触後{offset_tick}tickの{method_name}を解決できませんでした。")
                continue
            try:
                end = matching_parenthesis(block, call.end() - 1)
                call_args = split_java_arguments(block[call.end():end])
                parameters, method_body = method
                if len(call_args) != len(parameters):
                    raise ValueError("helper argument count mismatch")
                values = _resolve_variables(method_body, dict(zip(parameters, call_args)))
                spawn = list(re.finditer(r"\bspawnAttackEffect\s*\(", method_body))
                if len(spawn) != 1:
                    raise ValueError("expected one spawnAttackEffect in helper")
                spawn_end = matching_parenthesis(method_body, spawn[0].end() - 1)
                args = split_java_arguments(method_body[spawn[0].end():spawn_end])
                if not _is_zero_movement(args[9], values, methods):
                    raise ValueError("contact combo attack uses moving spawnAttackEffect")
                event = _hitbox_from_attack_effect(_projectile_from_call(args, offset_tick, methods, values))
                # The helper places the attack at player eye height plus its
                # rightVec/upVec/lookVec-relative offset; upVec is the schema's
                # local Y axis, while getEyeHeight is evaluated for a player.
                event["offset"][1] += 1.62 * 0.75
                event.update({"trigger": "CONTACT", "sound": "minecraft:entity.player.attack.sweep"})
                events.append(event)
            except (ValueError, IndexError) as error:
                warnings.append(f"接触後{offset_tick}tickの攻撃を変換できません: {error}")

    if not events or warnings:
        return None, events, warnings or ["接触後の連撃イベントを抽出できませんでした。"]
    finish_offset = None
    for condition in trigger_blocks:
        try:
            block, _ = _block_after(execute_body, condition.end())
        except ValueError:
            continue
        if "skillExecutions.remove" in block:
            finish_offset = int(condition.group(1))
            break
    if finish_offset is None:
        return None, events, ["連撃RUSHの終了tickを特定できませんでした。"]
    timeout = re.search(r"else\s+if\s*\(\s*FinalTick\s*>\s*(\d+)\s*\)", execute_body)
    try:
        start_forward = _number(move_match.group(1))
        start_vertical = _number(move_match.group(2))
        contact_forward = -_number(reverse_speed.group(1))
    except ValueError as error:
        return None, events, [f"連撃RUSHの移動値を変換できません: {error}"]
    combo = {
        "start_motion": {
            "forward": start_forward,
            "vertical": start_vertical,
            "pitch_limit_degrees": 15
        },
        "use_charge_multiplier": False,
        "invulnerability_ticks": int(invulnerability.group(1)),
        "contact_search_radius": _number(search_radius.group(1)),
        "contact_radius": _number(contact_radius.group(1)),
        "contact_motion": {"forward": contact_forward},
        "latest_contact_tick": int(timeout.group(1)) if timeout else duration,
        "no_contact_timeout_tick": int(timeout.group(1)) + 1 if timeout else duration + 1,
        "contact_animation_offset": 1,
        "finish_contact_offset": finish_offset
    }
    return {"rush_start_ticks": int(start_animation_tick.group(1)), "rush_combo": combo}, events, []


def _movement_vector(expression: str, values: dict[str, str],
                     methods: dict[str, tuple[list[str], str]]) -> list[float]:
    expression = _unwrap(_substitute(expression.strip(), values))
    if re.fullmatch(r"(?:[\w.]+\.)?Vec3\.ZERO", expression):
        return [0.0, 0.0, 0.0]
    return _vector(expression, "Vec3", values, methods)


def extract_projectile_events(source: str, final_tick: int,
                              stationary_hitboxes: list[dict[str, Any]] | None = None) -> tuple[list[dict[str, Any]], list[str]]:
    methods = _method_bodies(source)
    execute_match = re.search(r"\bvoid\s+execute\s*\([^)]*\)\s*\{", source)
    if not execute_match:
        return [], ["executeメソッドを解析できず、攻撃イベントを抽出できません。"]
    try:
        execute_body, _ = _block_after(source, execute_match.end() - 1)
    except ValueError:
        return [], ["executeメソッドのブロックを解析できず、攻撃イベントを抽出できません。"]

    tick_blocks: list[tuple[int, str]] = []
    condition = re.compile(r"\bFinalTick\s*==\s*(\d+)\s*\)")
    for match in condition.finditer(execute_body):
        try:
            body, _ = _block_after(execute_body, match.end())
        except ValueError:
            continue
        tick_blocks.append((int(match.group(1)), body))
    if not tick_blocks and final_tick == 0:
        tick_blocks.append((0, execute_body))
    # Fallback: if execute() has no FinalTick conditions but does call helpers
    # (e.g. Hant / Talon that delegate every-tick to performNeil), treat the
    # whole body as tick 1 so helper extraction can still run.
    if not tick_blocks:
        if re.search(r"\bspawnAttackEffect\b", execute_body):
            tick_blocks.append((1, execute_body))
        elif any("spawnAttackEffect" in mb for _, mb in methods.values()):
            tick_blocks.append((1, execute_body))
    if not tick_blocks:
        return [], ["攻撃イベントの実行tickを特定できません。"]

    events: list[dict[str, Any]] = []
    warnings: list[str] = []

    def _extract_from_body(body: str, tick: int, call_values: dict[str, str],
                           depth: int = 0, visited: frozenset[str] | None = None) -> None:
        """Recursively extract spawnAttackEffect calls from a code body."""
        if depth > 5:
            return
        if visited is None:
            visited = frozenset()
        body_values = _resolve_variables(body, call_values)

        # Direct spawnAttackEffect calls in this body
        for call in re.finditer(r"\bspawnAttackEffect\s*\(", body):
            try:
                end = matching_parenthesis(body, call.end() - 1)
                arguments = split_java_arguments(body[call.end():end])
                projectile = _projectile_from_call(arguments, tick, methods, body_values)
                if _is_zero_movement(arguments[9], body_values, methods):
                    if stationary_hitboxes is not None:
                        stationary_hitboxes.append(_hitbox_from_attack_effect(projectile))
                else:
                    events.append(projectile)
            except ValueError as error:
                if depth == 0:
                    warnings.append(
                        f"tick {tick}のspawnAttackEffectをskilldataへ変換できません: {error}"
                    )
                else:
                    warnings.append(
                        f"tick {tick}のspawnAttackEffect(depth={depth})を変換できません: {error}"
                    )

        # Helper method calls in this body (recurse into them)
        for method_name, (parameters, method_body) in methods.items():
            if method_name in visited:
                continue
            for call in re.finditer(rf"\b{re.escape(method_name)}\s*\(", body):
                try:
                    end = matching_parenthesis(body, call.end() - 1)
                    call_args_raw = split_java_arguments(body[call.end():end])
                    if len(call_args_raw) != len(parameters):
                        continue
                    # Substitute outer values into call arguments
                    resolved_args = [_substitute(a, body_values) for a in call_args_raw]
                    inner_values = dict(zip(parameters, resolved_args))
                    _extract_from_body(method_body, tick, inner_values,
                                       depth + 1, visited | {method_name})
                except ValueError:
                    pass

    for tick, body in tick_blocks:
        if re.search(r"\bRandom\b|\.nextBoolean\s*\(|\.nextInt\s*\(", body):
            warnings.append(
                f"tick {tick}にランダム分岐があります。抽出イベントは1つの分岐値を使用するため、"
                "legacy_fallback=trueで元のJava挙動を維持します。"
            )
        _extract_from_body(body, tick, {})

    return events, warnings


def make_definition(
    registration: SkillRegistration, duration: int, rush_start_ticks: int = 5,
    sounds: list[dict[str, Any]] | None = None,
    projectiles: list[dict[str, Any]] | None = None,
    legacy_fallback: bool = True,
    hitboxes: list[dict[str, Any]] | None = None,
    motions: list[dict[str, Any]] | None = None,
    rush_combo: dict[str, Any] | None = None,
) -> dict[str, Any]:
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
        "color": "#54D8FF",
        "motions": motions or [],
        "hitboxes": hitboxes or [],
        "projectiles": projectiles or [],
        "legacy_fallback": legacy_fallback,
        "sounds": sounds or [],
    }
    if registration.skill_type == "RUSH":
        definition.update(
            {
                "rush_start_ticks": rush_start_ticks,
                "rush_start_animation": "start",
                "rush_move_animation": "move",
                "rush_finish_animation": "finish",
            }
        )
        if rush_combo is not None:
            definition["rush_combo"] = rush_combo
    return definition


def read_rush_start_ticks(animations_dir: Path, animation_name: str) -> int | None:
    animation_path = animations_dir / f"{animation_name}.json"
    if not animation_path.is_file():
        return None
    try:
        animation_file = json.loads(animation_path.read_text(encoding="utf-8"))
        animations = animation_file["animations"]
    except (OSError, ValueError, KeyError, TypeError) as error:
        raise ValueError(f"cannot read startup animation length from {animation_path}: {error}") from error
    if not isinstance(animations, dict) or not animations:
        return None

    startup_animation = animations.get(f"{animation_name}.start")
    if startup_animation is not None:
        try:
            length = float(startup_animation["animation_length"])
        except (KeyError, TypeError, ValueError) as error:
            raise ValueError(f"cannot read startup animation length from {animation_path}: {error}") from error
        if not math.isfinite(length) or length < 0:
            raise ValueError(f"invalid startup animation length in {animation_path}")
        return min(1200, math.ceil(length * 20))

    lengths = []
    for name, animation in animations.items():
        try:
            length = float(animation["animation_length"])
        except (KeyError, TypeError, ValueError):
            continue
        if math.isfinite(length) and length >= 0:
            lengths.append(length)
    if not lengths:
        return None
    return min(1200, math.ceil(max(lengths) * 20))


def convert(registry: Path, skill_dir: Path, animations_dir: Path, output: Path, force: bool,
            skill_names: set[str] | None = None) -> int:
    try:
        registrations = load_registrations(registry)
    except (OSError, ValueError) as error:
        print(f"変換元の解析に失敗しました: {error}", file=sys.stderr)
        return 2

    output.mkdir(parents=True, exist_ok=True)
    report: list[dict[str, Any]] = []
    converted = 0
    skipped = 0
    for registration in registrations:
        if skill_names and registration.name not in skill_names:
            continue
        path, event_ticks, warnings = inspect_skill_source(skill_dir, registration.skill_class)
        source = strip_java_comments(path.read_text(encoding="utf-8")) if path else ""
        sounds = extract_sound_events(source) if path else []
        hitboxes: list[dict[str, Any]] = []
        motions: list[dict[str, Any]] = []
        rush_combo: dict[str, Any] | None = None
        combo_metadata: dict[str, Any] | None = None
        projectiles, attack_warnings = extract_projectile_events(
            source, registration.final_tick, hitboxes) if path else ([], [])
        rush_converted = False
        if path and registration.skill_type == "RUSH":
            rush_motions, rush_hitboxes, rush_warnings = extract_touch_rush_events(
                source, registration.final_tick)
            if rush_motions and rush_hitboxes:
                motions = rush_motions
                hitboxes = rush_hitboxes
                projectiles = []
                attack_warnings = []
                rush_converted = True
                warnings = [warning for warning in warnings if not warning.startswith((
                    "旧形式の攻撃エフェクト生成", "プレイヤー移動処理", "独自アニメーション切替"
                ))]
                warnings.extend([
                    "接触型RUSHの移動と攻撃をskilldataイベントへ移植しました。",
                    *rush_warnings,
                ])
                if "SimpleSkillSound" in source:
                    warnings.append("命中時のSimpleSkillSoundは自動移植されません。Blockbenchで攻撃音を設定してください。")
            else:
                warnings.extend(rush_warnings)
            combo_metadata, combo_hitboxes, combo_warnings = extract_contact_combo_rush(
                source, registration.final_tick)
            if combo_metadata and combo_hitboxes:
                warnings = []
                rush_combo = combo_metadata["rush_combo"]
                hitboxes = combo_hitboxes
                projectiles = []
                motions = []
                attack_warnings = []
                rush_converted = True
                warnings.extend([
                    "接触後の複数段RUSHをrush_comboとCONTACT hitboxイベントに変換しました。",
                    "Vec3.ZEROの攻撃は斬撃を生成せずhitbox判定として移行しました。",
                    "ゲーム内で接触タイミングと当たり範囲を確認してください。",
                ])
            elif combo_warnings:
                warnings.extend(combo_warnings)
        sounds = merge_coincident_sounds(sounds, hitboxes, projectiles)
        warnings.extend(attack_warnings)
        legacy_fallback = (not rush_converted and (not bool(projectiles or hitboxes)
                           or bool(attack_warnings) or any(
            warning.startswith(("プレイヤー移動処理", "状態効果の処理", "独自アニメーション切替"))
            for warning in warnings
        )))
        if (projectiles or hitboxes) and not attack_warnings:
            warnings.append(
                f"JavaのspawnAttackEffect呼び出しを{len(hitboxes)}件hitboxes、"
                f"{len(projectiles)}件projectilesへ変換しました。"
                "生成後に見た目と当たり範囲を確認してください。"
            )
        if sounds:
            warnings.append(
                f"単純なJava効果音呼び出しを{len(sounds)}件 soundsイベントへ変換しました。"
                "その他の効果音再生方法は手動で移植してください。"
            )
        rush_start_ticks = int(combo_metadata["rush_start_ticks"]) if combo_metadata else 5
        if registration.skill_type == "RUSH" and combo_metadata is None:
            try:
                detected_start_ticks = read_rush_start_ticks(animations_dir, registration.name)
            except ValueError as error:
                print(f"突進開始アニメーションの長さを取得できません: {error}", file=sys.stderr)
                return 2
            if detected_start_ticks is None:
                warnings.append("突進開始アニメーション長を検出できません。rush_start_ticksは既定の5tickです。")
            else:
                rush_start_ticks = detected_start_ticks
                animation_path = animations_dir / f"{registration.name}.json"
                animation_data = json.loads(animation_path.read_text(encoding="utf-8"))
                if f"{registration.name}.start" not in animation_data.get("animations", {}):
                    warnings.append(
                        "開始用 .start アニメーションがないため、既存アニメーションの最長時間から"
                        "rush_start_ticksを推定しました。必要に応じて手動調整してください。"
                    )
        duration_candidate = max([registration.final_tick, *event_ticks, rush_start_ticks, 1])
        duration = min(duration_candidate, 1200)
        if duration_candidate > 1200:
            warnings.append(f"推定duration_ticks {duration_candidate}は上限1200を超えるため1200に制限しました。")

        target = output / f"{registration.name}.json"
        if target.exists() and not force:
            skipped += 1
            status = "skipped_existing_file"
            warnings.append("既存ファイルを上書きしませんでした。--forceを指定すると上書きできます。")
        else:
            target.write_text(
                json.dumps(
                    make_definition(registration, duration, rush_start_ticks, sounds,
                                    projectiles, legacy_fallback, hitboxes, motions, rush_combo),
                    ensure_ascii=False,
                    indent=2,
                ) + "\n",
                encoding="utf-8",
            )
            converted += 1
            status = "draft_created"

        report.append(
            {
                "name": registration.name,
                "legacy_id": registration.legacy_id,
                "class": registration.skill_class,
                "source": str(path) if path else None,
                "output": str(target),
                "status": status,
                "duration_ticks": duration,
                "rush_start_ticks": rush_start_ticks if registration.skill_type == "RUSH" else None,
                "detected_final_ticks": event_ticks,
                "warnings": warnings,
            }
        )

    report_path = output / "conversion-report.json"
    if skill_names and report_path.is_file():
        try:
            previous_report = json.loads(report_path.read_text(encoding="utf-8"))
            if isinstance(previous_report, list):
                replaced_names = {entry["name"] for entry in report if "name" in entry}
                report = [entry for entry in previous_report
                          if not isinstance(entry, dict) or entry.get("name") not in replaced_names] + report
        except (OSError, ValueError):
            pass
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"登録スキル数: {len(registrations)}")
    print(f"JSONひな形作成: {converted}")
    print(f"既存ファイルのためスキップ: {skipped}")
    print(f"変換レポート: {report_path}")
    print("注意: legacy_fallback=trueのJSONは、攻撃イベント未移植中も元のJavaスキル処理を維持します。")
    return 0


def main() -> int:
    project_root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description="Convert Java SwordSkill registrations into skilldata drafts.")
    parser.add_argument(
        "--registry",
        type=Path,
        default=project_root / "src/main/java/com/zoma1101/swordskill/swordskills/SwordSkillRegistry.java",
        help="SwordSkillRegistry.java path",
    )
    parser.add_argument(
        "--skills-dir",
        type=Path,
        default=project_root / "src/main/java/com/zoma1101/swordskill/swordskills/skill",
        help="directory containing legacy skill implementation classes",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=project_root / "build/skilldata_migration",
        help="output directory (defaults to build/skilldata_migration)",
    )
    parser.add_argument(
        "--animations-dir",
        type=Path,
        default=project_root / "src/main/resources/assets/swordskill/player_animation",
        help="directory containing PlayerAnimator animation JSON files",
    )
    parser.add_argument("--force", action="store_true", help="overwrite generated JSON files")
    parser.add_argument(
        "--skills",
        nargs="+",
        help="convert only the named skills (for example: rage_spike sonic_reap)",
    )
    args = parser.parse_args()
    return convert(args.registry, args.skills_dir, args.animations_dir, args.output, args.force,
                   set(args.skills) if args.skills else None)


if __name__ == "__main__":
    raise SystemExit(main())
