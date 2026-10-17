#!/usr/bin/env python3
"""Java式の静的評価エンジン (スキルクラス → 正規化式)。

SwordSkillのJavaスキル実装を「型から基本的な変換」するための式評価器。
- 変数の逐次代入解決
- if / else-if / else チェーンの静的分岐評価 (SkillTexture.*ParticleType.contains() を含む)
- switch 式・switch 文 (アロー形式) の分岐選択
- 純粋算術式の畳み込み
- 中間変数・冗語な括弧の除去
- spawnAttackEffect / 効果音 / setDeltaMovement 呼び出しの抽出 (ヘルパーメソッド再帰込み)
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

import convert_java_skills as v1
from convert_java_skills import (  # noqa: F401  (再エクスポート)
    matching_parenthesis,
    split_java_arguments,
    strip_java_comments,
)


# ------------------------------------------------------------------
# 字句ユーティリティ
# ------------------------------------------------------------------

_OPERATOR_CHARS = set("+-*/%!<>=&|^?")
_BRACE_PAIR = {"{": "}"}


def has_top_level_operator(text: str) -> bool:
    """トップレベル (括弧の外) に二項演算子・比較演算子などがあるか。"""
    depth = 0
    in_string = False
    escaped = False
    for char in text:
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif depth == 0 and char in _OPERATOR_CHARS:
            return True
    return False


def matching_brace(text: str, brace_index: int) -> int:
    """`{` の位置に対応する `}` の位置を返す (文字列リテラル対応)。"""
    depth = 0
    in_string = False
    escaped = False
    for index in range(brace_index, len(text)):
        char = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return index
    raise ValueError("unclosed brace")


def block_after(text: str, brace_index: int) -> tuple[str, int]:
    """`{` の位置から中身と終了位置 (次の文字のインデックス) を返す。"""
    closing = matching_brace(text, brace_index)
    return text[brace_index + 1:closing], closing + 1


def unwrap(expression: str) -> str:
    """式全体を囲む冗語な括弧を剥がす (連続)。"""
    result = expression.strip()
    while result.startswith("(") and result.endswith(")"):
        try:
            if v1.matching_parenthesis(result, 0) != len(result) - 1:
                break
        except ValueError:
            break
        result = result[1:-1].strip()
    return result


_SIMPLE_WRAP_RE = re.compile(
    r"(?<![\w.$])\(\s*(?!(?:float|double|int|long|short|byte|boolean|char|void)\b)"
    r"((?P<num>-?[\w.]+)|(?P<str>\"[^\"]*\"))\s*\)")


def wash_simple_parens(expression: str) -> str:
    """(player) / (0) / (\"text\") のような単一トークンを包む冗語な括弧を剥がす。"""
    current = expression
    while True:
        washed = _SIMPLE_WRAP_RE.sub(lambda m: m.group("num") or m.group("str"), current)
        if washed == current:
            return current
        current = washed




def strip_redundant_parens(expression: str) -> str:
    """余分な括弧を剥がす。中に二項演算子を含む括弧は安全のため残す。"""
    expr = expression.strip()
    while True:
        stripped = unwrap(expr)
        if stripped != expr:
            expr = stripped
            continue
        if expr.startswith("("):
            try:
                end = v1.matching_parenthesis(expr, 0)
            except ValueError:
                break
            if end < len(expr) - 1 and expr[end + 1] in ".(":
                inner = expr[1:end]
                if inner.strip() and not has_top_level_operator(inner):
                    # ((call(...)).scale(k)) のような連鎖前置の冗語括弧
                    expr = (inner + expr[end + 1:]).strip()
                    continue
        break
    return expr


# ------------------------------------------------------------------
# 数値評価
# ------------------------------------------------------------------

def eval_number(expression: str) -> float:
    """純粋な数値式を評価する。getXRot 等の動的項があれば ValueError。"""
    expr = strip_redundant_parens(expression.strip())
    expr = re.sub(r"\(\s*(?:float|double|int|long)\s*\)", "", expr)
    expr = re.sub(r"(?<=\d)\s*[fFdD]\b", "", expr)
    if re.search(r"[A-Za-z_]", expr):
        raise ValueError(f"not a numeric expression: {expression!r}")
    if not re.fullmatch(r"[-+0-9.e ()*/]+", expr):
        raise ValueError(f"not a numeric expression: {expression!r}")
    try:
        value = eval(expr, {"__builtins__": {}}, {})  # noqa: S307 (静的な数値のみ)
    except Exception as error:  # noqa: BLE001
        raise ValueError(f"numeric evaluation failed for {expression!r}: {error}") from error
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"non numeric result for {expression!r}")
    return float(value)


def format_number(value: float) -> str:
    if float(value).is_integer():
        return str(int(value))
    return repr(round(float(value), 6))


def try_number(expression: str) -> float | None:
    try:
        return eval_number(expression)
    except ValueError:
        return None


def try_int(expression: str) -> int | None:
    number = try_number(expression)
    return int(number) if number is not None else None


# ------------------------------------------------------------------
# 変数代入のパターン
# ------------------------------------------------------------------

_ASSIGNMENT_RE = re.compile(
    r"^\s*(?:(?:final|public|private|protected|static)\s+)*"
    r"(?:[\w.<>\[\] ,?]+\s+)?"
    r"(?P<name>\w+)\s*=\s*(?P<expr>.+)$", re.DOTALL)


def match_assignment(statement: str) -> tuple[str, str] | None:
    stripped = statement.rstrip().rstrip(";").strip()
    match = _ASSIGNMENT_RE.match(stripped)
    if not match:
        return None
    name = match.group("name")
    if name in ("return", "break", "continue", "new", "case", "default"):
        return None
    lhs_head = stripped[:stripped.find("=")]
    # LHS の「型宣言」部分 (name 直前のトークン) が `==` 等を含む場合は代入でない
    if re.search(r"(?:==|!=|<=|>=|&&|\|\|)", lhs_head):
        return None
    return name, match.group("expr").strip()


# ------------------------------------------------------------------
# ステートメント分割
# ------------------------------------------------------------------

def iter_statements(body: str) -> list[str]:
    """本体をステートメント (セミコロン / 複合ブロック) に分割する。"""
    statements: list[str] = []
    start = 0
    cursor = 0
    in_string = False
    escaped = False
    length = len(body)
    while cursor < length:
        char = body[cursor]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            cursor += 1
            continue
        if char == '"':
            in_string = True
            cursor += 1
            continue
        if char == "{":
            try:
                end = matching_brace(body, cursor)
            except ValueError:
                break
            # if(...) {...} else if(...){...} else {...} の連鎖ごと取り込む
            if _statement_head_is_conditional(body, start, cursor):
                while True:
                    tail = body[end + 1:]
                    tail_match = re.match(r"\s*else\b", tail)
                    if not tail_match:
                        break
                    brace_probe = None
                    if_match = re.match(r"\s*else\s+if\s*\(", tail)
                    if if_match:
                        open_paren = end + 1 + tail.find("(", if_match.start())
                        try:
                            close_paren = v1.matching_parenthesis(body, open_paren)
                        except ValueError:
                            break
                        brace_probe = body.find("{", close_paren)
                    else:
                        brace_probe = end + 1 + tail.find("{", tail_match.end())
                    if brace_probe is None or brace_probe < 0:
                        break
                    try:
                        end = matching_brace(body, brace_probe)
                    except ValueError:
                        break
            statements.append(body[start:end + 1].strip())
            cursor = end + 1
            start = cursor
            continue
        if char == ";":
            statements.append(body[start:cursor + 1].strip())
            cursor += 1
            start = cursor
            continue
        if char == "}":
            stray = body[start:cursor].strip()
            if stray:
                statements.append(stray)
            cursor += 1
            start = cursor
            continue
        cursor += 1
    tail = body[start:].strip()
    if tail:
        statements.append(tail)
    return statements


def _statement_head_is_conditional(body: str, start: int, brace_index: int) -> bool:
    head = body[start:brace_index]
    return bool(re.match(r"\s*(?:if\s*\(|else\b)", head))


# ------------------------------------------------------------------
# 抽出イベント構造
# ------------------------------------------------------------------

@dataclass
class RawCall:
    kind: str          # "spawn" | "sound" | "motion"
    name: str
    args: list[str] = field(default_factory=list)

    def describe(self) -> str:
        return f"{self.name}({', '.join(self.args)})"


_SPAWN_CALL_RE = re.compile(r"\bspawnAttackEffect\s*\(")
_SOUND_CALL_RE = re.compile(
    r"\b(?:" + "|".join(re.escape(name) for name in v1.SOUND_HELPERS) + r")\s*\(")
_MOTION_CALL_RE = re.compile(r"\bsetDeltaMovement\s*\(")


def _parse_final_tick_condition(condition: str, max_tick: int = 64) -> list[int]:
    """FinalTick条件を具体tickリストへ展開する。

    `FinalTick == N` と `FinalTick % k == r` をサポートする。
    `&&` で結ばれた残りの部分はランタイム条件 (onGround等) として静的に近似される。
    """
    cond = condition.strip()
    conjunction = _split_logical(cond)
    if conjunction is not None and conjunction[0] == "&&":
        left_ticks = _parse_final_tick_condition(conjunction[1], max_tick)
        right_ticks = _parse_final_tick_condition(conjunction[2], max_tick)
        if left_ticks and right_ticks:
            return [tick for tick in left_ticks if tick in right_ticks]
        # どちらか一方がFinalTick条件で、もう一方が近似可能なランタイム条件の場合
        if left_ticks and _is_approximable_runtime_condition(conjunction[2]):
            return left_ticks
        if right_ticks and _is_approximable_runtime_condition(conjunction[1]):
            return right_ticks
        return []
    equality = re.fullmatch(r"FinalTick\s*==\s*(-?\d+)", cond)
    if equality:
        value = int(equality.group(1))
        return [value] if value >= 1 else [1]
    modulo = re.fullmatch(r"FinalTick\s*%\s*(\d+)\s*==\s*(\d+)", cond)
    if modulo:
        divisor = int(modulo.group(1))
        remainder = int(modulo.group(2))
        if divisor <= 0 or divisor > 40:
            return []
        start = remainder if remainder >= 1 else divisor
        return list(range(start, max_tick + 1, divisor))
    # `FinalTick <= N` / `FinalTick >= N` の簡易サポート
    range_match = re.fullmatch(r"FinalTick\s*(<=|>=|<|>)\s*(-?\d+)", cond)
    if range_match:
        bound = int(range_match.group(2))
        operator = range_match.group(1)
        if operator == "<=":
            return list(range(1, min(max_tick, bound) + 1))
        if operator == "<":
            return list(range(1, min(max_tick, bound)))
        if operator == ">=":
            return list(range(max(bound, 1), max_tick + 1))
        return list(range(max(bound, 1) + 1, max_tick + 1))
    return []


def _is_approximable_runtime_condition(condition: str) -> bool:
    cond = condition.strip()
    probe = cond[1:] if cond.startswith("!") else cond
    probe = strip_redundant_parens(probe)
    return bool(re.fullmatch(r"(?:[\w.]+\s*\.\s*)?(onGround|isOnGround)\s*\(\s*\)", probe))


def _normalize_pitch(expression: str) -> str:
    expr = unwrap(expression.strip())
    expr = wash_simple_parens(expr)
    expr = re.sub(r"\(\s*(?:float|double|int|long)\s*\)", "", expr)
    expr = re.sub(r"(?<=\d)\s*[fFdD]\b", "", expr)
    return expr


def _texture_literal(expr: str) -> str | None:
    for helper, particle in v1.TEXTURE_HELPERS.items():
        if re.fullmatch(rf"(?:[\w.]+\.)?{helper}\s*\(\s*\)", expr):
            return particle
    return None


def select_switch_case(switch_body: str, selector: str, warnings: list[str]) -> str | None:
    """switch本体から selector (正規化済み) に該当するケース本文を取り出す。"""
    if re.search(r"\bcase\s+[^->:\n]+:", switch_body):
        warnings.append("コロン形式のswitch文は未対応です。")
        return None
    numeric_selector = try_int(selector)
    if numeric_selector is not None:
        for case in re.finditer(r"case\s+(-?\d+(?:\s*,\s*-?\d+)*)\s*->\s*", switch_body):
            labels = [int(label.strip()) for label in case.group(1).split(",")]
            if numeric_selector in labels:
                arrow = switch_body.find("->", case.start())
                return member_after_arrow(switch_body, arrow + 2)
    else:
        quoted = re.fullmatch(r'"([^"]*)"', selector.strip())
        if quoted:
            for case in re.finditer(r'case\s+"([^"]*)"\s*->\s*', switch_body):
                if case.group(1) == quoted.group(1):
                    arrow = switch_body.find("->", case.start())
                    return member_after_arrow(switch_body, arrow + 2)
    default_match = re.search(r"default\s*->\s*", switch_body)
    if default_match:
        return member_after_arrow(switch_body, default_match.end())
    return None


def member_after_arrow(text: str, start: int) -> str:
    """case -> 以降の本体 (ブロック {} または式) を取り出す。"""
    rest = text[start:]
    stripped = rest.lstrip()
    if stripped.startswith("{"):
        brace = start + rest.find("{")
        closing = matching_brace(text, brace)
        return text[brace:closing + 1]
    # 式のcase: 次の case / default / } までを切り出す
    terminator = len(rest)
    boundary = re.search(r"\n\s*(?=case\b|default\b|\}\s*(?:;|$))", rest)
    if boundary:
        terminator = boundary.start()
    candidate = rest[:terminator].strip().rstrip(";").strip()
    if re.search(r"(?:case|default)\b", candidate):
        # 改行が存在しない極端な形式への保険
        candidate = re.split(r"\bcase\s|\bdefault\s", candidate)[0].strip().rstrip(";").strip()
    return candidate


# ------------------------------------------------------------------
# エンジン本体
# ------------------------------------------------------------------

class UnresolvedExpression(ValueError):
    """静的評価できなかった式。"""


class SkillEngine:
    def __init__(self, source: str, known_sets: dict[str, set[str]] | None = None,
                 max_depth: int = 10) -> None:
        self.methods: dict[str, tuple[list[str], str]] = v1._method_bodies(source)
        self.known_sets = dict(known_sets) if known_sets is not None \
            else dict(v1.DEFAULT_KNOWN_SETS)
        self.max_depth = max_depth

    # --------------------------------------------------------------
    # 公開API: FinalTick ディスパッチの分割
    # --------------------------------------------------------------
    def split_tick_segments(self, body: str, max_tick: int = 64
                            ) -> tuple[list[tuple[int | None, str, dict[str, str]]],
                                       dict[str, str]]:
        """FinalTick 分岐チェーンで本体を (tick|None, セグメント本文, 先行宣言値) に分割する。

        2番目の返り値はチェーン前に宣言された変数の正規化値 (各セグメントの seed) 。
        分割できない場合は ([(None, body, {})], {}) を返す。
        max_tick は FinalTick % 演算の展開に用いる。
        並列する複数チェーン (例: if(FinalTick%3==0){...} と if(FinalTick==N){...}) も解析する。
        """
        empty: dict[str, str] = {}
        # 複数チェーン対応: 上位レベルの分割は外側のループで行う
        dispatch = re.compile(r"\bif\s*\(\s*FinalTick\b")
        match = dispatch.search(body)
        if not match:
            return [(None, body, empty)], empty
        prelude_text = body[:match.start()].strip()
        if prelude_text and not _is_prelude_only_declarations(prelude_text):
            return [(None, body, empty)], empty

        base_values: dict[str, str] = {}
        if prelude_text:
            warnings: list[str] = []
            try:
                self._walk_statements(prelude_text, base_values, warnings, 0, [])
            except UnresolvedExpression:
                return [(None, body, empty)], empty

        segments: list[tuple[int | None, str, dict[str, str]]] = []
        cursor = match.start()
        while cursor < len(body):
            remaining = body[cursor:]
            next_dispatch = dispatch.search(remaining)
            if next_dispatch is None:
                break
            chain_segments, chain_end = self._parse_dispatch_chain(
                remaining, next_dispatch, max_tick)
            if chain_segments is None:
                return [(None, body, empty)], empty
            segments.extend(chain_segments)
            if chain_end >= len(remaining):
                break
            cursor += chain_end
        seeded = [(tick, text, dict(base_values)) for tick, text, _ in segments]
        return seeded, base_values

    def _parse_dispatch_chain(self, remaining: str, match: re.Match[str], max_tick: int
                              ) -> tuple[list[tuple[int | None, str, dict[str, str]]],
                                         int] | None:
        """1つの if...else if...else チェーンを解析し、チェーンの終了位置を返す。"""
        segments: list[tuple[int | None, str, dict[str, str]]] = []
        index = remaining.find("(", match.start())
        while True:
            if index < 0:
                return None, 0
            try:
                close_paren = v1.matching_parenthesis(remaining, index)
            except ValueError:
                return None, 0
            condition = remaining[index + 1:close_paren].strip()
            brace = remaining.find("{", close_paren)
            if brace < 0:
                return None, 0
            try:
                block, after = block_after(remaining, brace)
            except ValueError:
                return None, 0
            ticks = _parse_final_tick_condition(condition, max_tick)
            if not ticks:
                return None, 0
            for tick in ticks:
                segments.append((tick, block, {}))
            tail = remaining[after:]
            else_if = re.match(r"\s*else\s+if\s*\(", tail)
            if else_if:
                index = after + else_if.end() - 1
                continue
            plain_else = re.match(r"\s*else\s*\{", tail)
            if plain_else:
                brace = after + tail.find("{", plain_else.start())
                try:
                    block, chain_end = block_after(remaining, brace)
                except ValueError:
                    return None, 0
                segments.append((None, block, {}))
            else:
                chain_end = after
            return segments, chain_end

    # --------------------------------------------------------------
    # 公開API: セグメント解析
    # --------------------------------------------------------------
    def analyze_segment(self, segment_body: str, seed_values: dict[str, str] | None = None,
                        warnings: list[str] | None = None) -> list[RawCall]:
        """セグメント本文から攻撃/効果音/移動呼び出しを抽出する。"""
        warnings = warnings if warnings is not None else []
        values = dict(seed_values or {})
        calls: list[RawCall] = []
        try:
            self._walk_statements(segment_body, values, warnings, 0, calls)
        except UnresolvedExpression as error:
            warnings.append(f"静的評価を中断: {error}")
        return calls

    def resolve_values(self, body: str, seed_values: dict[str, str] | None = None,
                       warnings: list[str] | None = None) -> dict[str, str]:
        """呼び出しを無視して宣言のみ解決した変数辞書を返す。"""
        warnings = warnings if warnings is not None else []
        values = dict(seed_values or {})
        try:
            self._walk_statements(body, values, warnings, 0, [])
        except UnresolvedExpression as error:
            warnings.append(f"静的評価を中断: {error}")
        return values

    # --------------------------------------------------------------
    # ステートメント走査
    # --------------------------------------------------------------
    def _walk_statements(self, body: str, values: dict[str, str], warnings: list[str],
                         depth: int, calls: list[RawCall]) -> str | None:
        """本体を走査して呼び出しを `calls` に集め、return 式を返す。"""
        if depth > self.max_depth:
            raise UnresolvedExpression("statement recursion limit")
        for statement in iter_statements(body):
            stripped = statement.strip()
            if not stripped:
                continue
            return_match = re.match(r"\breturn\b(.+)$", stripped, re.DOTALL)
            if return_match:
                return self.canon(return_match.group(1), values, warnings, depth)
            if stripped.startswith("if"):
                verdict_or_value = self._walk_if_chain(stripped, values, warnings,
                                                       depth, calls)
                continue
            if stripped.startswith("switch") and not has_top_level_operator(
                    stripped.split("(")[0]):
                self._walk_switch_statement(stripped, values, warnings, depth, calls)
                continue
            assignment = match_assignment(stripped)
            if assignment is not None:
                name, expression = assignment
                try:
                    values[name] = self.canon(expression, values, warnings, depth)
                except UnresolvedExpression as error:
                    warnings.append(f"変数 {name} を評価できませんでした({error})")
                continue
            self._emit_call_statement(stripped, values, warnings, depth, calls)
        return None

    def _emit_call_statement(self, statement: str, values: dict[str, str],
                             warnings: list[str], depth: int, calls: list[RawCall]) -> None:
        text = statement.rstrip().rstrip(";").strip()
        match = re.fullmatch(r"(?:[\w.]+\.)?(?P<name>[\w$]+)\s*\((?P<args>.*)\)",
                             text, re.DOTALL)
        if not match:
            return
        name = match.group("name")
        raw_args = split_java_arguments(match.group("args"))
        if name == "spawnAttackEffect":
            calls.append(RawCall("spawn", name,
                                 [self.canon(arg, values, warnings, depth)
                                  for arg in raw_args]))
            return
        if name in v1.SOUND_HELPERS:
            calls.append(RawCall("sound", name,
                                 [self.canon(arg, values, warnings, depth)
                                  for arg in raw_args]))
            return
        if name == "setDeltaMovement":
            calls.append(RawCall("motion", name,
                                 [self.canon(arg, values, warnings, depth)
                                  for arg in raw_args]))
            return
        if name in self.methods:
            self._run_method(name, raw_args, values, warnings, depth, calls)

    def _run_method(self, name: str, raw_args: list[str], values: dict[str, str],
                    warnings: list[str], depth: int, calls: list[RawCall]) -> str | None:
        if depth > self.max_depth:
            raise UnresolvedExpression(f"method inlining limit: {name}")
        if name not in self.methods:
            raise UnresolvedExpression(f"unknown helper method: {name}")
        parameters, method_body = self.methods[name]
        if len(raw_args) != len(parameters):
            raise UnresolvedExpression(
                f"helper {name}: expected {len(parameters)} args, got {len(raw_args)}")
        binding = {param: self.canon(arg, values, warnings, depth + 1)
                   for param, arg in zip(parameters, raw_args)}
        inner_values = dict(values)
        inner_values.update(binding)
        return self._walk_statements(method_body, inner_values, warnings, depth + 1, calls)

    # --------------------------------------------------------------
    # if / switch チェーン
    # --------------------------------------------------------------
    def _walk_if_chain(self, statement: str, values: dict[str, str], warnings: list[str],
                       depth: int, calls: list[RawCall]) -> None:
        segments = self._parse_if_chain(statement)
        if segments is None:
            warnings.append("if/else連鎖を解析できませんでした。")
            return
        for condition, block in segments:
            if condition is None:
                self._walk_statements(block, values, warnings, depth + 1, calls)
                return
            verdict = self.canon_bool(condition, values, warnings, depth)
            if verdict is None:
                warnings.append(f"if条件を静的に評価できません: {condition}")
                return
            if verdict:
                self._walk_statements(block, values, warnings, depth + 1, calls)
                return

    @staticmethod
    def _parse_if_chain(statement: str) -> list[tuple[str | None, str]] | None:
        segments: list[tuple[str | None, str]] = []
        index = statement.find("(")
        while True:
            if index < 0:
                return None
            try:
                close = v1.matching_parenthesis(statement, index)
            except ValueError:
                return None
            condition = statement[index + 1:close].strip()
            brace = statement.find("{", close)
            if brace < 0:
                return None
            try:
                block, after = block_after(statement, brace)
            except ValueError:
                return None
            segments.append((condition, block))
            tail = statement[after:]
            else_if = re.match(r"\s*else\s+if\s*\(", tail)
            if else_if:
                index = after + else_if.end() - 1
                continue
            plain_else = re.match(r"\s*else\s*\{", tail)
            if plain_else:
                brace = after + tail.find("{", plain_else.start())
                try:
                    block, _ = block_after(statement, brace)
                except ValueError:
                    return None
                segments.append((None, block))
            return segments

    def _walk_switch_statement(self, statement: str, values: dict[str, str],
                               warnings: list[str], depth: int, calls: list[RawCall]) -> None:
        open_paren = statement.find("(")
        if open_paren < 0:
            return
        try:
            close_paren = v1.matching_parenthesis(statement, open_paren)
        except ValueError:
            return
        brace = statement.find("{", close_paren)
        if brace < 0:
            return
        try:
            switch_body, _ = block_after(statement, brace)
        except ValueError:
            return
        try:
            selector = self.canon(statement[open_paren + 1:close_paren],
                                  values, warnings, depth)
        except UnresolvedExpression as error:
            warnings.append(f"switch文の選択値を評価できません: {error}")
            return
        chosen = select_switch_case(switch_body, selector, warnings)
        if chosen is None:
            return
        piece = chosen.strip()
        if piece.startswith("{"):
            try:
                block, _ = block_after(piece, 0)
            except ValueError:
                warnings.append("switchケースのブロック解析に失敗しました。")
                return
            self._walk_statements(block, values, warnings, depth + 1, calls)
            return
        self._walk_statements(piece + ";", values, warnings, depth + 1, calls)

    # --------------------------------------------------------------
    # 式の正規化
    # --------------------------------------------------------------
    def canon(self, expression: str, values: dict[str, str], warnings: list[str],
              depth: int = 0) -> str:
        if depth > self.max_depth:
            raise UnresolvedExpression(f"expression depth limit: {expression[:60]}")
        expr = expression.strip().rstrip(";").strip()
        for _ in range(24):
            expr = self._substitute(expr, values)
            expr = strip_redundant_parens(wash_simple_parens(expr))
            texture = _texture_literal(expr)
            if texture:
                return f'"{texture}"'
            if expr == "Vec3.ZERO":
                return "new Vec3(0,0,0)"
            if expr == "Vec2.ZERO":
                return "new Vec2(0,0)"
            if expr.startswith("switch"):
                brace = expr.find("{")
                if brace < 0:
                    raise UnresolvedExpression(f"switch body not found: {expr[:80]}")
                selector_raw = unwrap(expr[len("switch"):brace].strip())
                if not selector_raw:
                    raise UnresolvedExpression(f"switch selector not found: {expr[:60]}")
                selector = self.canon(selector_raw, values, warnings, depth + 1)
                switch_body, _ = block_after(expr, brace)
                selected = select_switch_case(switch_body, selector, warnings)
                if selected is None:
                    raise UnresolvedExpression(f"switch case not found: {selector}")
                expr = selected
                continue
            call_match = re.fullmatch(r"([A-Za-z_$][\w$]*)\s*\((.*)\)", expr, re.DOTALL)
            if call_match and call_match.group(1) in self.methods:
                name, arg_text = call_match.group(1), call_match.group(2)
                parameters, method_body = self.methods[name]
                args = split_java_arguments(arg_text)
                if len(args) == len(parameters):
                    binding = {param: self.canon(arg, values, warnings, depth + 1)
                               for param, arg in zip(parameters, args)}
                    inner_values = dict(values)
                    inner_values.update(binding)
                    returned = self._walk_statements(method_body, inner_values,
                                                     warnings, depth + 1, [])
                    if returned is not None:
                        expr = returned
                        continue
                    raise UnresolvedExpression(f"helper {name} has no return")
            ternary = self.split_ternary(expr)
            if ternary is not None:
                condition, if_true, if_false = ternary
                verdict = self.canon_bool(condition, values, warnings, depth)
                if verdict is None:
                    # FinalTick 等まだタイミング由来の条件は呼び出し元で解決される
                    raise UnresolvedExpression(f"ternary condition unresolved: {condition}")
                expr = if_true if verdict else if_false
                continue
            if "getXRot" in expr:
                return _normalize_pitch(expr)
            number = try_number(expr)
            if number is not None:
                return format_number(number)
            return wash_simple_parens(expr)
        raise UnresolvedExpression(f"expression did not stabilize: {expr[:80]}")

    def _substitute(self, expr: str, values: dict[str, str]) -> str:
        for name in sorted(values, key=len, reverse=True):
            value = values[name]
            vec_match = re.fullmatch(r"new\s+(?:Vec2|Vec3|Vector3f)\s*\((.*)\)",
                                     value.strip(), re.DOTALL)
            if vec_match:
                args = split_java_arguments(vec_match.group(1))
                for axis, order in (("x", 0), ("y", 1), ("z", 2)):
                    if order < len(args):
                        expr = re.sub(rf"\b{re.escape(name)}\s*\.\s*{axis}\b",
                                      f"({args[order].strip()})", expr)
            else:
                look_scale = re.fullmatch(
                    r"(?:[\w.]+\.)?getLookAngle\s*\(\s*\)\s*(?:\.scale\s*\(([^()]*)\))?",
                    value.strip())
                if look_scale:
                    factor = look_scale.group(1)
                    for axis in ("x", "y", "z"):
                        if factor:
                            replacement = rf"(lookVec.scale({factor}).{axis})"
                        else:
                            replacement = rf"(lookVec.{axis})"
                        expr = re.sub(rf"\b{re.escape(name)}\s*\.\s*{axis}\b",
                                      replacement, expr)
            expr = re.sub(rf"\b{re.escape(name)}\b", f"({value})", expr)
        return expr

    # --------------------------------------------------------------
    # 条件評価
    # --------------------------------------------------------------
    def canon_bool(self, condition: str, values: dict[str, str], warnings: list[str],
                   depth: int = 0) -> bool | None:
        """条件式を評価する。解決不能なら None。"""
        cond = strip_redundant_parens(condition.strip())
        if cond in ("true", "false"):
            return cond == "true"
        logical = _split_logical(cond)
        if logical is not None:
            operator, left, right = logical
            left_side = self.canon_bool(left, values, warnings, depth)
            right_side = self.canon_bool(right, values, warnings, depth)
            if left_side is None or right_side is None:
                return None
            if operator == "&&":
                return left_side and right_side
            return left_side or right_side
        if cond.startswith("!") and not has_top_level_operator(unwrap(cond[1:])):
            inner = self.canon_bool(cond[1:].strip(), values, warnings, depth)
            return None if inner is None else not inner
        if "FinalTick" in cond:
            # FinalTick条件はタイミング条件なのでここでは評価しない
            return None
        if "Math.random" in cond or "random" in cond:
            warnings.append(f"ランダム条件は静的評価できません: {cond}")
            return None
        ground_predicate = re.fullmatch(
            r"!?\s*(?:[\w.]+\s*\.\s*)?onGround\s*\(\s*\)", cond)
        if ground_predicate:
            warnings.append("onGround()条件を恒真として近似しました。")
            return not cond.startswith("!")
        objects_equals = re.fullmatch(r"Objects\s*\.\s*equals\s*\((.*),(.*)\)",
                                      cond, re.DOTALL)
        if objects_equals:
            return self._compare_equality(objects_equals.group(1), objects_equals.group(2),
                                          values, warnings, depth)
        method_equals = re.fullmatch(r"(.+?)\s*\.\s*(?:equals|equalsIgnoreCase)\s*\((.*)\)",
                                     cond, re.DOTALL)
        if method_equals:
            return self._compare_equality(method_equals.group(1), method_equals.group(2),
                                          values, warnings, depth)
        contains = re.fullmatch(
            r"(?:[\w.]+\.)?(?P<set>\w+_ParticleType)\s*\.\s*contains\s*\((?P<arg>[^()]+)\)",
            cond)
        if contains:
            set_name = contains.group("set")
            arg_text = self.canon(contains.group("arg").strip(), values, warnings, depth)
            quoted = re.fullmatch(r'"([^"]*)"', arg_text)
            if not quoted:
                return None
            if set_name not in self.known_sets:
                warnings.append(f"未登録のパーティクル集合: {set_name}")
                return None
            return quoted.group(1) in self.known_sets[set_name]
        equality = re.fullmatch(r"(.+?)\s*==\s*(.+)", cond, re.DOTALL)
        if equality:
            return self._compare_equality(equality.group(1), equality.group(2),
                                          values, warnings, depth)
        inequality = re.fullmatch(r"(.+?)\s*!=\s*(.+)", cond, re.DOTALL)
        if inequality:
            result = self._compare_equality(inequality.group(1), inequality.group(2),
                                            values, warnings, depth)
            return None if result is None else not result
        order = re.fullmatch(r"(.+?)\s*(<=|>=|<|>)\s*(.+)", cond, re.DOTALL)
        if order:
            left = _as_comparable(self.canon(order.group(1), values, warnings, depth))
            right = _as_comparable(self.canon(order.group(3), values, warnings, depth))
            if not isinstance(left, float) or not isinstance(right, float):
                return None
            operator = order.group(2)
            return {"<": left < right, ">": left > right,
                    "<=": left <= right, ">=": left >= right}[operator]
        return None

    def _compare_equality(self, left_text: str, right_text: str, values: dict[str, str],
                          warnings: list[str], depth: int) -> bool | None:
        # Objects.equals(a, b) / a.equals(b) 形式の展開
        objects_equals = re.fullmatch(r"Objects\s*\.\s*equals\s*\((.*),(.*)\)",
                                      strip_redundant_parens(left_text), re.DOTALL)
        if objects_equals:
            left_text, right_text = objects_equals.group(1), objects_equals.group(2)
        left_str = None
        right_str = None
        left = _as_comparable(self.canon(left_text, values, warnings, depth))
        right = _as_comparable(self.canon(right_text, values, warnings, depth))
        if left is None or right is None:
            return None
        return left == right


    # --------------------------------------------------------------
    # 三項演算
    # --------------------------------------------------------------
    @staticmethod
    def split_ternary(expression: str) -> tuple[str, str, str] | None:
        expr = expression.strip()
        question = -1
        depth = 0
        in_string = False
        for index, char in enumerate(expr):
            if char == '"':
                in_string = not in_string
            elif in_string:
                continue
            elif char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            elif char == "?" and depth == 0:
                question = index
                break
        if question < 0:
            return None
        depth = 0
        colon = -1
        in_string = False
        for index in range(question + 1, len(expr)):
            char = expr[index]
            if char == '"':
                in_string = not in_string
            if in_string:
                continue
            if char in "([{":
                depth += 1
            elif char in ")]}":
                depth -= 1
            elif char == ":" and depth == 0:
                colon = index
                break
        if colon < 0:
            return None
        condition = expr[:question].strip()
        if_true = expr[question + 1:colon].strip()
        if_false = expr[colon + 1:].strip()
        if not condition:
            return None
        return condition, if_true, if_false


# ------------------------------------------------------------------
# 下位ユーティリティ
# ------------------------------------------------------------------

def _split_logical(condition: str) -> tuple[str, str, str] | None:
    """トップレベルの && / || を最浅位置で分離する。"""
    candidates: list[tuple[str, int]] = []
    depth = 0
    in_string = False
    index = 0
    while index < len(condition):
        char = condition[index]
        if in_string:
            if char == '"':
                in_string = False
            index += 1
            continue
        if char == '"':
            in_string = True
            index += 1
            continue
        if char in "([{":
            depth += 1
            index += 1
            continue
        if char in ")]}":
            depth -= 1
            index += 1
            continue
        if depth == 0 and condition.startswith("&&", index):
            candidates.append(("&&", index))
            index += 2
            continue
        if depth == 0 and condition.startswith("||", index):
            candidates.append(("||", index))
            index += 2
            continue
        index += 1
    if not candidates:
        return None
    operator, position = candidates[0]
    return operator, condition[:position].strip(), condition[position + 2:].strip()


def _as_comparable(text: str) -> str | float | None:
    if text is None:
        return None
    try:
        return eval_number(text)
    except ValueError:
        pass
    quoted = re.fullmatch(r'"([^"]*)"', text.strip())
    if quoted:
        return quoted.group(1)
    return None


def _is_prelude_only_declarations(text: str) -> bool:
    """宣言文 (or 空) のみから成るか判定する。呼び出し文が含まれたら False。"""
    for statement in iter_statements(text):
        stripped = statement.strip()
        if not stripped:
            continue
        if match_assignment(stripped) is None:
            # if/switch等も許容しない (前奏は単純宣言のみ)
            return False
    return True


def _before_switch_body(expr: str) -> int:
    """switch(セレクタ) の閉じ括弧位置を返す。"""
    open_paren = expr.find("(")
    try:
        return v1.matching_parenthesis(expr, open_paren)
    except ValueError:
        return -1


def _switch_expression_body(expr: str) -> str | None:
    closing = _before_switch_body(expr)
    if closing < 0:
        return None
    brace = expr.find("{", closing)
    if brace < 0:
        return None
    try:
        body, _ = block_after(expr, brace)
    except ValueError:
        return None
    return body
