#!/usr/bin/env python3
"""ゲーム内確認用チェックリストを生成する。"""

from __future__ import annotations

import json
from pathlib import Path

WEAPON_JA = {
    "ONE_HANDED_SWORD": "片手剣", "TWO_HANDED_SWORD": "両手剣", "KATANA": "刀",
    "AXE": "斧", "RAPIER": "レイピア", "CLAW": "爪", "SPEAR": "槍",
    "MACE": "メイス", "SCYTHE": "鎌", "DAGGER": "短剣", "DUALSWORD": "二刀流",
}

lines = [
    "# スキルデータ変換 v2 ゲーム内確認チェックリスト",
    "",
    "生成: build/skilldata_migration_v2 / 適用: src/main/resources/data/swordskill/skilldata (2026-10-17)",
    "",
    "## 動作確認ポイント (全スキル共通)",
    "- 斬撃エフェクトの見た目 (アニメ+トレイル/パーティクル色) と当たり判定の範囲・位置",
    "- ヒット時のダメージ・ノックバック感 (BaseDamage(player) 基準 × damage_multiplier)",
    "- 効果音のタイミング (tick番号は hitboxのsound または sounds配列)",
    "- 特殊効果: undead系=不死種ダメージ増 / red=lifesteal / blood系=半血以下×2.5 /"
    " king・gold=魔法追加ダメージ / mace=移動低下100tick amp1",
    "",
    "## スキル別一覧",
    "",
    "| id | name | type | 武器 | dur | hb/pr | fx | 補足 |",
    "|---|---|---|---|---|---|---|---|",
]

for path in sorted(Path("src/main/resources/data/swordskill/skilldata").glob("*.json")):
    data = json.loads(path.read_text(encoding="utf-8"))
    hb = len(data.get("hitboxes", []))
    pr = len(data.get("projectiles", []))
    fx = sorted({effect["type"] for box in data.get("hitboxes", []) + data.get("projectiles", [])
                 for effect in box.get("effects", [])})
    weapons = ",".join(WEAPON_JA.get(w, w) for w in data["weapons"])
    notes = []
    if data.get("legacy_fallback"):
        notes.append("**legacy_fallback (Java準拠のまま)**")
    if pr:
        notes.append(f"移動攻撃{pr}件")
    if data.get("motions"):
        notes.append(f"motions{len(data['motions'])}件")
    if "rush_combo" in data:
        notes.append("rush_combo/CONTACT")
    if data.get("type") == "RUSH":
        notes.append(f"rush_start={data.get('rush_start_ticks')}")
    lines.append("| {id} | {name} | {t} | {w} | {dur} | {hb}/{pr} | {fx} | {note} |".format(
        id=data["id"], name=data["name"], t=data["type"], w=weapons,
        dur=data["duration_ticks"], hb=hb, pr=pr,
        fx=",".join(fx) if fx else "-",
        note="; ".join(notes) if notes else "-"))

lines += [
    "",
    "## legacy_fallback 残存6スキル (参考)",
    "",
    "- how_to_use: チャット表示のみ (データ表現不可のため Java 実行のまま)",
    "- gengetu: ランダムで上/下斬撃の切替 (rotation差・アニメバリアント)",
    "- flashing_penetrator: 対象への動的照準 (回転が対象位置依存)",
    "- rapid_bite: 接触時に 対象位置起点スポーン+動的回転+クーディング短縮",
    "- fast: 接触RUSH 3連撃",
    "- mace_impact: 落下距離でダメージ変動 (接触finish)",
    "",
]

out = Path("build/skilldata_migration_v2/verification-checklist.md")
out.write_text("\n".join(lines), encoding="utf-8")
print("wrote", out)
