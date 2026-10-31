# Swordskill 引き継ぎナレッジ

最終更新: 2026-10-26
対象リポジトリ: `D:\MOD\Mods\NeoForge[1.21.1]\Swordskill`

会話をまたぐためのメモ。新しいセッションでは最初にこれを読む。
ファイル単位の対応表は [code-map.md](code-map.md) を参照。

---

## 1. プロジェクト概要

| 項目 | 値 |
| --- | --- |
| Mod ID | `swordskill` |
| Mod 名 | swordskill (SwordSkill / SAO のソードスキル) |
| バージョン | `1.3.3` |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.179` |
| Java | 21 |
| ビルド | ModDevGradle (`net.neoforged.moddev` 2.0.93) |
| 作者 | Zoma1101 |
| ライセンス | MIT |

主要ディレクトリ:

```
src/main/java/com/zoma1101/swordskill/   Java 本体
src/main/resources/assets/swordskill/    アニメーション・モデル・テクスチャ・lang
src/main/resources/data/swordskill/      スキル定義・熟練度・武器種・鍛冶データ
tools/skilldata/                         検証・移行用 Python スクリプト
tools/blockbench/                        Blockbench プラグイン
tools/talkdata/                          この引き継ぎメモ
run/                                     開発用クライアント実行ディレクトリ
```

---

## 2. 現在のリリース方針（重要）

`src/main/java/com/zoma1101/swordskill/SwordSkillFeatures.java`

| フラグ | 値 | 意味 |
| --- | --- | --- |
| `SMITHING` | `false` | 鍛冶台・溶解炉・鍛造武器・インゴット・メニュー・レシピ・コマンドを登録しない |
| `STATUS_SCREEN` | `false` | ステータス画面（プレイヤーレベル／STR・AGI）を無効化 |

- `STATUS_SCREEN=false` のとき、`K`（`key.swordskill.status_key`）は **熟練度一覧画面** を直接開く。
- `levelSystemEnabled` は既定 `false`。プレイヤーレベルの kill XP / SP ボーナス / STR・AGI 補正は停止。
- **熟練度（proficiency）はプレイヤーレベルとは独立**して動作する。

再有効化する場合:

- `SwordSkillFeatures.SMITHING = true`
- `SwordSkillFeatures.STATUS_SCREEN = true`
- `config/swordskill-server.toml` の `levelSystemEnabled = true`
- 鍛冶レシピ `recipe/smithing_forge.json` / `recipe/melting_forge.json` を復元

---

## 3. スキルの設計

### 3.1 全スキルがデータ駆動

- スキル本体は `src/main/resources/data/swordskill/skilldata/*.json`。
- 旧・スキルごとの Java クラスは削除済み。`SwordSkillRegistry` が JSON を読み込んで `SKILLS` に登録する。
- スキル実行は `swordskills/DataDrivenSkill.java` が担当。
- **`legacy_fallback` は禁止**。`replace_builtin: true` が基本。
- 新規スキルは skilldata JSON + `assets/swordskill/player_animation/<animation>.json` の 2 つ。

### 3.2 skilldata の主なフィールド

基本:

- `schema_version`, `id`, `replace_builtin`, `name`, `animation`
- `type` … `SIMPLE` / `RUSH` / `TRANSFORM` / `TRANSFORM_FINISH` など
- `weapons` … `RAPIER`, `DAGGER`, `ONE_HANDED_SWORD`, `TWO_HANDED_SWORD`, `KATANA`, `AXE`, `CLAW`, `SPEAR`, `MACE`, `SCYTHE`
- `cooldown`, `sp_cost`, `duration_ticks`, `transform_limit_ticks`
- `hidden`, `assignable`, `default_unlocked`
- `proficiency_category`, `unlock_proficiency`
- `invulnerability_ticks`, `end_skill_on_ground`
- `color`, `trail_length`, `shockwave_ticks`, `hitstop_ticks`

イベント:

- `motions[]` … `tick`, `forward`, `strafe`, `vertical`, `set`, `repeat`, `trigger` (`TICK` / `CONTACT` / `GROUND`)
- `hitboxes[]` … `tick`, `hitbox_enabled`, `offset`, `size`, `damage_multiplier`, `damage_type` (`BASE` / `RUSH`), `knockback`, `knockback_multiplier`, `repeat_while_rushing`, `effects`, `sound`, `trigger`
  - Blockbench 用の未使用フィールド `rotation`, `duration_ticks`, `travel_distance`, `particle` も保持する
- `projectiles[]` … 飛翔斬撃。`tick`, `offset`, `rotation`, `size`, `damage_multiplier`, `damage_type`, `knockback`, `knockback_multiplier`, `duration_ticks`, `particle`, `travel_distance`, `movement`, `render_pitch`, `single_target`, `sound`, `trigger`, `fall_damage_scale`, `path`, `random_group`, `random_index`, `aim_at_contact`
- `sounds[]` … `tick`, `sound`, `volume`, `pitch`, `trigger`
- `animations[]` … 途中で PlayerAnimator アニメーションを切り替えるイベント
- `player_effects[]` … 落下ダメージ無効など
- RUSH 系: `rush_start_ticks`, `rush_start_animation`, `rush_move_animation`, `rush_finish_animation`, `rush_combo`（`start_motion`, `contact_motion_forward`, `latest_contact_tick`, `no_contact_timeout_tick`, `contact_animation_offset`, `finish_contact_offset`, `invulnerability_ticks`, `pitch_limit_degrees`, `search_radius`, `contact_radius`）

> `set` を省略すると **加算**（現在の速度に足す）。`repeat` は毎 tick 加算。
> この挙動が `critical_pain` の終了時バウンドの原因（後述）。

### 3.3 派生（コンボ）

- 派生関係は **連続する `TRANSFORM` → `TRANSFORM_FINISH` の ID** で決まる（`SkillChains.java`）。
- `TRANSFORM_FINISH` が出たらその連鎖は終了。
- 派生先はスロットに割り当て不可。
- 単独使用は `SkillChainTracker` が拒否（`chargeTicks > 0` のチャージ派生のみ許可）。
- コンボ受付は `lastExecutedSkillId` + `transform_limit_ticks` のウィンドウ。
- 派生成立時は `SoundEvents.AMETHYST_BLOCK_CHIME`（音量 0.7、pitch 1.2 + 0.1 × depth）。
- チャージ中にコンボを始めた場合、長押ししてもウィンドウが持続する。
- `horizontal_square` を `vertical` の派生元として表示しない。

### 3.4 ダメージ・レベル

- プレイヤーレベルは **ダメージに影響しない**。
- レベル差補正は **`EnemyLevel` を持つ敵だけ**に作用（`CombatStats.levelDiffAttackFactor`）。
- ステータスは STR / AGI のみ。防御は防具（バニラの防御力）のみ。
- スキルダメージは `SkillUtils.BaseDamage` 系 + スキルレベル倍率 + STR 補正 + 武器攻撃力。
- スキルレベル Lv1 = ×1.00 ～ Lv10 = ×3.00 の線形。

### 3.5 衝突・鍔迫り合い（2026-10-26 追加）

- サーバー側: `server/handler/SkillClashManager.java`
- クライアント側: `client/handler/SkillClashHandler.java`（火花・アニメ）、`Hud_Registry` の `skill_clash` レイヤー（ゲージ）
- 通知: `payload/SkillClashPayload.java`（新規。`phase` = LOCK_START / LOCK_END / SPARK）

仕様:

- **プレイヤー同士（または訓練用マネキン）のスキル判定がぶつかったとき**に衝突判定が走る。双方が
  `SkillExecutionManager` の実行中（マネキンは `isClashReady()`）で、`hitboxes` を持つスキルであることが条件。
  距離 `clashRange`、向き `clashFacingDegrees`、再発動 `clashCooldownTicks` も満たす必要がある。
  成立後にロックになるか一方的決着になるかは、押し込み力の差で決まる（下記）。
- 成立すると双方のスキルを中断し、`clashLockTicks`（既定 2 tick ≈ 0.1 秒）のあいだ移動・スキル使用・スロット変更を固定。
  移動は速度 0 だけでなく、開始位置（アンカー）から 0.02 ブロック以上ずれたら
  `ServerPlayer#connection.teleport`（プレイヤー以外は `teleportTo`）でサーバーから送り返す
  （クライアントの移動パケットでは抜けられない）。
  両者間のダメージは `LivingIncomingDamageEvent` で無効化（第三者からのダメージは通る）。
- 押し込み力（`SkillClashManager#clashPower`）は **実ダメージ相当値**:
  `(BaseDamage または RushDamage) × スキルの最大 damage_multiplier × チャージ倍率`。
  RUSH ヒットボックスを持つスキルは移動速度込みの `SkillUtils.RushDamage` を使う。
- **ダメージ差が `clashLockThreshold`（既定 0.15 = 15%）未満のときだけ鍔迫り合いになる。**
  差がそれ以上ならロックせず、**金属の火花だけ散らして強い側の一方的な勝ち**:
  弱い側のスキルを即座に中断し、強い側のスキルはそのままダメージを通す
  （`SkillClashManager#oneSidedWin`。弱い側には 20 tick の再判定クールダウン）。
- ロック中の最終判定は **自動**。`clashDrawThreshold`（既定 0.05 = 5%）未満の差は引き分け、
  それ以上なら少し強い側の勝ち。`clashRandomness` で揺らぎを追加できる（既定 0）。
- 勝者は無傷で短時間の無敵、敗者はノックバック＋移動鈍化／弱体。引き分けは双方ノックバック。
- **訓練用マネキン以外の Mob は鍔迫り合いしない**（通常どおりダメージ）。
- **訓練用マネキン** `swordskill:skill_mannequin` はプレイヤーと同じ体格・スキンで描画され、
  1 クライアントでも鍔迫り合いを実装テストできる。
  - `/swordskill mannequin [倍率] [スキル名]` または `/summon swordskill:skill_mannequin` で出現（2 ブロック前）。
  - `/swordskill mannequin skill <スキル名>` で 16 ブロック以内の最も近いマネキンに
    **常時使用させるスキル**を設定（`none` で解除）。
  - スキル未設定時の押し込み力は「相手プレイヤーの押し込み力 × 倍率」のミラー（チャージ倍率も含む）。
    倍率 ×1.00 なら常に同等＝鍔迫り合い、×0.50 なら一方的に負け、×1.50 なら一方的に勝ち。
    右クリックで ×1.00 → ×0.50 → ×1.50 → 無効（通常ダメージ）の順に切り替える。
  - スキルを設定すると「相手プレイヤーの基礎ダメージ × そのスキルの damage_multiplier × 倍率」。
    同じスキル同士なら ×1.00 で同等になり、別スキルなら倍率差で一方的決着になる。
    チャージ倍率はプレイヤー側にだけ掛かるので、同じスキルならチャージで ×1.50 のマネキンにも勝てる。
  - スキル設定中はスキル定義の `duration_ticks` に応じた間隔（最低 20 tick）で
    **スキルアニメーションと武器トレイルを再生**し、近くのスキル使用中プレイヤーへ
    こちらから鍔迫り合いを仕掛ける。スキル自体のヒットボックス・ダメージは出さず、
    押し込み力・ゲージ色・仕掛け判定にだけ使う。
  - アニメーションは `client/handler/MannequinSkillAnimations.java` が
    `player_animation` の GeckoLib 定義を `KeyframeAnimationPlayer` で自前評価して
    `PlayerModel` の腕・胴・頭へ適用する（`playerAnimator` はモブに効かないため）。
    対応するアニメーションが見つからないスキルでは簡易斬撃モーションを代わりに再生する。
  - トレイルは `WeaponTrailHandler` が実際に描画された剣の向きから生成する。
    色と長さはスキル定義の `color` / `trail_length`。
  - 通常時は剣を構えたポーズ、ロック中は両腕を突き出すポーズ。
    同期データ `DATA_CLASH_POSE` / `DATA_SKILL_ID` / `DATA_SWING_START` を使う。
  - 移動・攻撃はせず、`isPushable=false`・自己修復あり（200 HP）。
- 鍔迫り合いの火花は `spark_streak.png` の光の筋（`SparkStreakParticleOptions`）を使う。
  ロック中は中央から小さく、開始・終了時は放射状に多めに飛ばす。
- **飛翔斬撃同士**は `AttackEffectEntity` の AABB 交差で相殺（両方消滅＋火花）。
- **飛翔斬撃 vs スキル実行中プレイヤー（またはマネキン）**も鍔迫り合いに移行する。
- アニメーションは `ClientConfig` の `clashAnimation`（既定 `clash_lock`）。
  `AnimationUtils.playClashAnimation` が先頭フレームで固定再生（`FrozenKeyframeAnimationPlayer`）するため、
  ロック時間がアニメーション長を超えても姿勢は戻らない。
  リソース `player_animation/clash_lock.json` は暫定ポーズ。Blockbench で差し替え可能。
  このファイルは GeckoLib 形式の「tick 0 のみ `vector` 直書き」を使っている
  （`GeckoLibSerializer` が対応済み。時間キーに書き換えても動くが、固定再生なので不要）。
- `PROTOCOL_VERSION` を `12` に更新（payload 追加のため）。

未対応:

- 連打入力による押し込み（自動判定のみ）。
- 鍔迫り合い専用の効果音・カメラシェイク（既存の `SoundEvents` と火花のみ）。

---

## 4. 熟練度（Proficiency）

- 武器カテゴリごとに個別の熟練度。
- **スキルで撃破したときだけ**上昇（`ProficiencyManager.awardForKill`）。
- **100 熟練度 = 1 ポイント**。
- HUD には表示しない。
- ポイントでスキルを解放。必要ポイントはカテゴリ別に datapack で変更可能。
- 上書きファイル: `src/main/resources/data/<ns>/proficiency/<category>.json`

```json
{
  "skills": {
    "skill_name": 3
  }
}
```

- 記載のないスキルは skilldata の `unlock_proficiency` にフォールバック。
- 初期解放:
  - 実効コスト `<= 0` のスキル
  - `default_unlocked: true` の 11 スキル（`slant`, `cascade`, `sakura_mai`, `wall_wind`, `spark_thrust`, `end_revolver`, `thrusts`, `canine`, `barrick`, `talon`, `grand`）
- `how_to_use` は説明用エントリ（id 0）。スロット割り当て不可。
- 熟練度の主カテゴリと `weapons` の先頭が一致しないと無効（例: `wall_wind` の `axe.json` エントリは削除済み）。

---

## 5. クライアント UI

- 選択画面: `client/gui/SwordSkillSelectionScreen.java`
  - 下部スロットは常時表示
  - ステータス画面へのボタンは置かない
  - テーマ切替ボタンは 32×32 または 64×64
  - `SaoButton` はバニラ風
  - 派生関係を見やすく表示
- 熟練度一覧: `client/gui/ProficiencyOverviewScreen.java`（`K` で開く）
- ステータス: `client/gui/StatusScreen.java`（現在は無効）
- テーマ: `client/gui/SaoTheme.java`
  - ライト: 白／黒＋オレンジアクセント、文字は読みやすく
  - ダーク: 既存の SAO 風
  - 状態は `guiDarkTheme` に保存
- HUD: `client/gui/Hud_Registry.java`, `HudPositionSettingScreen.java`
- クイック選択キー既定:
  - `SwordSkill_Use_Key` = `R`
  - `_0` ～ `_4` = `1` ～ `5`

---

## 6. アニメーション・Blockbench

### 6.1 リソース

- プレイヤーアニメーション: `src/main/resources/assets/swordskill/player_animation/*.json`
- skilldata の `animation` 名とファイル名／アニメーション名を一致させる。
- PlayerAnimator / GeckoLib 形式（`format_version: 1.8.0`）。

### 6.2 Blockbench プラグイン

- `tools/blockbench/swordskill_skill_creator.js`（バージョン `2.19.4`）
- `tools/blockbench/README.md` に操作説明。
- 主な仕様:
  - skilldata JSON のインポート／エクスポート
  - 攻撃・飛翔斬撃・移動・サウンド・アニメーション切替・PlayerEffect を `Effects > Timeline` のカスタムキーフレームとして編集
  - インポート時に最初の攻撃／飛翔斬撃キーフレームを自動選択し、ヒットボックスにカメラをフィット
  - プレビューのボックスは `DoubleSide`
  - 旧マーカーの末尾セミコロン／空白を許容し、アニメーション選択時に正規化
  - **`update_selection` を購読しない**（`Canvas.updateAll()` が再発火して無限再帰するため）
  - 開いているプロジェクトを上書きしない。既存アニメーションとユーザーのアイテム編集を保持
  - 未使用ヒットボックスフィールドを import/export で保持

### 6.3 モデルファイル（リポジトリ外）

`D:\MOD\Mods\` 配下:

| ファイル | 状態 |
| --- | --- |
| `ss_player.bbmodel` | ユーザーが Blockbench で開いている作業ファイル。`snake_bite` 含む |
| `ss用player.bbmodel` | 元モデル。古い／stale。リポジトリ直下にもコピーあり（未追跡） |
| `ss用player_snake_bite_fixed.bbmodel` | 右腕修正版の別名保存 |
| `ss用player_snake_bite_itemfix.bbmodel` | `rightItemFix` 親グループ版。検証中 |

### 6.4 GeckoLib コーデックの注意

- GeckoLib の Blockbench コーデックは import/export で **X/Y 回転と X 位置を反転**する。
- モデル内部の回転は JSON の X/Y の Negation、Z はそのまま。
- `Format.euler_order` は既定 `ZYX` で Minecraft の `ModelPart` と同じ。回転順は原因ではない。
- `snake_bite` の右腕不一致は **古いキーフレーム**が原因だった（肩ピボットではない）。
- ミラーリング（`*_mirrored.bbmodel`, `player_rig_fix.js`）は却下・削除済み。

### 6.5 現時点の未解決（rightItem）

- `right_arm` はプレビューとゲームで一致。
- `rightItem`（手持ち武器）だけが一致しない。
- 原因はプレビューの軸問題で、右腕の問題ではない。
- `rightItemFix` 親（Z=180°, pivot `[6,13,1]`）＋ `dia_sw`/`spear` の Z=180° カウンター回転で、静止時の剣先を保ったまま X アニメの向きを反転させる実装を検証中。
- ユーザー報告: 「どのように動かしても変わらない」。
- 切り分けが必要:
  1. プレビューで動かない → `t=0.2083` / `0.375` の `rightItem` キーフレームを編集しているか（rest pose はアニメに上書きされる）
  2. プレビューは動くがゲームで変わらない → アニメーション JSON を再エクスポートしたか（ゲームは `.bbmodel` を読まない）
- フォールバック案: JSON の符号を直接 `rightItem` / `leftItem` に使い、コーデックに例外を入れて GeckoLib がアイテムボーンを反転しないようにする。

---

## 7. 現在の作業状態

### 7.1 Git

- 最新コミット: `3d43677 Blockbenchの旧マーカー読込とヒットボックス表示を修正`
- `master` は未 push。
- 作業ツリーに **未コミットの変更が多数**ある。破棄しないこと。
  - `player_animation/*.json` 21 ファイル変更
  - `skilldata/*.json` 18 ファイル変更
  - `proficiency/axe.json` 変更
  - 新規 `player_animation/*.json` 13 ファイル（`critical_pain`, `snake_bite`, `test` など）
  - 新規 `skilldata/*.json` 7 ファイル
  - 未追跡 `ss用player.bbmodel`（リポジトリ直下）

### 7.2 検証済み

- `tools/skilldata/validate_v2.py` … 93 ファイル通過
- `tools/skilldata/validate_proficiency.py` … 11 カテゴリ / 83 エントリ通過
- Blockbench 5.2.2 で `ss用player.bbmodel` の旧マーカー攻撃表示と `horizontal_arc` インポートを実機確認
- QuickJS ハーネスで import → re-import → export、追加ヒットボックスフィールド保持を確認
- `snake_bite.json` と `ss_player` / 修正版の比較で `body`, `head`, `right_arm`, `rightItem`, `left_arm` の不一致 0

### 7.3 未完了・既知の問題

1. **`critical_pain` の終了時バウンド**
   - 原因判明: `skilldata/critical_pain.json` の `motions`
     - tick 16: `vertical: 0.3`, `trigger: "GROUND"`
     - tick 17: `vertical: 0.9`（TICK、加算）
     - tick 21: `vertical: -1`（TICK、加算）
   - tick 17 で打ち上げ → tick 21 で落下 → 着地時に tick 16 の GROUND モーションが `+0.3` を加算 → 終盤に小さく跳ねる。
   - 修正案: GROUND モーションを削除 / `vertical` を 0.1 程度に下げる / `trigger` を `CONTACT` に変更。
2. **`rightItem` プレビュー不一致**（上記 6.5）
3. ~~`gradlew build` が最新変更後に未実行~~ → 2026-10-26 に再実行し `BUILD SUCCESSFUL` を確認済み。
4. リリース残作業:
   - tier curve と datapack 敵 `level` / `floor`
   - スキルスロットのレベル解放
   - 防具（`forged_armor`）
   - 素材アイテムのエンティティ
   - クリエイティブタブ
5. **鍔迫り合い・衝突は未実機確認**
   - コンパイルとデータ検証のみ。
   - 1 クライアントなら `/swordskill mannequin`（または `/summon swordskill:skill_mannequin`）で
     訓練用マネキンを出し、ロック開始・ゲージ・ポーズ・位置固定・自動判定・ノックバックまで確認できる
     （`pvp` は不要。マネキンは `SkillTargetEntity` の対象になる）。
   - マネキンを右クリックすると押し込み力 ×1.00 / ×0.50 / ×1.50 / 無効 を切り替えられる。
     ×1.00 はミラーで常に同等（ロック）、×0.50 / ×1.50 はダメージ差が大きく一方的決着（火花のみ）。
   - `/swordskill mannequin skill <スキル名>` で常時使用スキルを設定でき、
     同じスキル同士なら ×1.00 でロック、別スキルなら倍率差で一方的決着になる。
     設定したスキルのアニメーションと武器トレイルが定期的に再生されることも確認する。
   - 鍔迫り合いの火花が `spark_streak.png` 風の光の筋になっていることを確認する。
   - プレイヤー同士は `ServerConfig` の `pvp=true` にして 2 クライアントで確認する。
   - ロック中の見た目は `ClientConfig.clashAnimation`（既定 `clash_lock`）に依存。
   - 飛翔斬撃同士の相殺は `clashCancelProjectiles` で無効化できる。

---

## 8. よく使うコマンド

```powershell
# ビルド（角括弧パスはリテラルで渡す）
cmd /c '"D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\gradlew.bat" -p "D:\MOD\Mods\NeoForge[1.21.1]\Swordskill" build --console=plain'

# データ検証
python tools/skilldata/validate_v2.py src/main/resources/data/swordskill/skilldata
python tools/skilldata/validate_proficiency.py src/main/resources

# クライアント実行前の準備
cmd /c '"D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\gradlew.bat" -p "D:\MOD\Mods\NeoForge[1.21.1]\Swordskill" prepareClientRun'
```

ゲーム内で鍔迫り合いだけを確認する場合（1 クライアントで可、`pvp` 不要）:

```text
/swordskill mannequin                 # 押し込み力 ×1.00（相手と同等＝ロック）
/swordskill mannequin 0.5             # ダメージ差でプレイヤーの一方的勝ち
/swordskill mannequin 1.5             # ダメージ差でマネキンの一方的勝ち
/swordskill mannequin 1.0 cascade     # マネキンに cascade を常時使用させる
/swordskill mannequin skill cascade   # 16 ブロック以内のマネキンにスキルを設定
/swordskill mannequin skill none      # スキル設定を解除
/swordskill mannequin 1.1             # ロック後にわずかにマネキンが勝つ（draw 閾値 5% 超）
/summon swordskill:skill_mannequin    # コマンド版（倍率は既定 1.00）
```

マネキンを右クリックすると ×1.00 → ×0.50 → ×1.50 → 無効 を切り替えられる
（無効時は鍔迫り合いせず通常どおりダメージが通る）。
スキル未設定の ×1.00 は相手の押し込み力をミラーするため、相手のスキル倍率やチャージに関係なくロックになる。

注意:

- `gradlew clean` は `build/moddev/*/` を消す。実行後は `prepareClientRun` を再実行。
- プラグインを変更したら Blockbench で **Load Plugin from File** から再読み込み。
- ユーザーの Blockbench プラグインフォルダには `swordskill_skill_creator.js` は置かれていない（手動ロード）。

---

## 9. 触るときの注意

- 作業ツリーの未コミット変更を破棄しない。
- `src/main/resources/data/swordskill/skilldata/*.json` は **`replace_builtin: true`** を維持。`legacy_fallback` を書かない。
- スキルのリーチ延長は JSON に焼き込み済み:
  - `offset[2] += 0.375`
  - `size[2] += 0.75`
- スパーク:
  - 色はトレイル色（`saturateColor`）を薄めて使う
  - `spark_streak` は先端だけ白熱
- チャージ:
  - クールダウン中の発動はアニメーションだけ再生する事故を起こさない
  - 無効なチャージは pose / glint / sound より前にブロック
  - クライアント側で SP を事前チェックし、足りなければ選択中のチャージ段を下げる
  - SP はログイン／リスポーン時に同期
- 鍛冶コードは残っているが `SMITHING=false` で登録されない。読むときは前提を忘れない。
- `StatusScreen` は残っているが `STATUS_SCREEN=false`。

---

## 10. 通信の効率化メモ（2026-10-26 調査・未実施）

別タブで payload を触っているため、既存 codec の変更は見送った。新規 payload は
`SkillClashPayload` の 1 つだけ。以下は効果の大きい順の候補。

| 対象 | 現状 | 削減案 | 備考 |
| --- | --- | --- | --- |
| `SkillHitEffectPayload` | 約 62 B/命中（`Vec3` double×2 = 48 B） | `position` を送らず target の AABB と attacker 位置から再計算。方向は float×3 | 最多送信。約 40 B/命中の削減 |
| 命中系の統合 | `SkillHitEffectPayload` + `HitstopPayload` を別送信 | `SkillImpactPayload` に統合し、対象 viewer 集合も 1 回だけ構築 | ヘッダと走査が半減 |
| `SkillShockwavePayload` | 色を `"#RRGGBB"` 文字列 | RGB を varint（`0xRRGGBB`） | 約 4 B/発動 |
| `SyncSPPayload` | `double`×2 = 16 B | `float`×2 = 8 B、または 0.01 固定小数点 varint | 発動ごと |
| `PlayAnimationPayload` | `skillId` が INT(4 B)、`animationType` が UTF | `skillId` を VAR_INT、RUSH phase を byte 化 | RUSH は phase 切替ごと |
| ログイン要求 | `SkillRequest` / `CheckSkillUnlocked` / `RequestSkillLevels` の 3 連発 | 1 つの `RequestAll` に集約 | ログイン時のみ |
| `SyncDataSkillsPayload` | 全定義 JSON を 16k 文字で分割送信 | `Deflater` 圧縮、またはハッシュ一致でスキップ | reload 時のみ。転送量が最大 |

注意:

- どれも protocol 変更なので `PROTOCOL_VERSION` の更新が必要。
- `SkillHitEffectPayload` の `position` 削除はクライアント側の描画位置が微妙に変わるため、
  見た目の確認が必要。
- 採用時は `tools/talkdata/code-map.md` の payload 表も更新する。

