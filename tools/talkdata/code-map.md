# コード配置マップ

`src/main/java/com/zoma1101/swordskill/` 配下のファイルと役割。
「どのファイルを触ればよいか」を探すための索引。最終更新 2026-10-24。

> 役割は現時点の理解。詳細な実装は各ファイルを読むこと。

---

## 1. ルートパッケージ

| ファイル | 役割 |
| --- | --- |
| `SwordSkill.java` | Mod エントリ。Config / Entity / Item / Attribute / Effect / Particle 登録、reload listener、サーバー tick で `SkillExecutionManager` を駆動、スキル定義の同期 |
| `SwordSkillFeatures.java` | リリース用機能フラグ（`SMITHING=false`, `STATUS_SCREEN=false`） |
| `AnimationUtils.java` | アニメーション関連のユーティリティ |
| `IsAnimation.java` | アニメーション対象を示すマーカー interface |

---

## 2. `capability`

| ファイル | 役割 |
| --- | --- |
| `PlayerSkills.java` | プレイヤーのスキル状態（解放・選択スロットなど）の Capability |

---

## 3. `swordskills`（スキル本体・実行）

| ファイル | 役割 |
| --- | --- |
| `SwordSkillRegistry.java` | skilldata JSON を読み込み `SKILLS` に登録。`replaceDataDrivenSkills` / `getDataDrivenDefinitions` |
| `DataDrivenSkill.java` | スキル 1 tick 分の実行。モーション、ヒットボックス、飛翔斬撃、サウンド、アニメ切替、プレイヤー効果、チャージ、RUSH |
| `DataSkillDefinition.java` | skilldata の record 定義（Motion / MotionEvent / Hitbox / ProjectileEvent / SoundEvent / RushCombo など） |
| `SkillData.java` | スキルの基本データ（ID、名前、武器種、type、SP、CT、アニメ名など）と `SkillType` enum |
| `SkillChains.java` | 派生チェーンの解決。`isDerived` / `next` / `parent` / `depthOf` / `startOf` |
| `SkillUtils.java` | ダメージ計算、飛翔斬撃スポーン、ターゲット判定などの共通処理 |
| `SkillSound.java` | スキル用サウンド定義／再生補助 |
| `SkillTexture.java` | 飛翔斬撃などのテクスチャ定義 |
| `SkillHitEffectRegistry.java` | ヒット時エフェクトの登録・解決 |
| `ISkill.java` | スキル実行 interface（データ駆動スキルもこれを実装） |

### 実行の流れ

```
クライアント: キー入力
  → ClientSkillSlotHandler / ClientTickHandler / ClientForgeHandler
  → UseSkillPayload(skillId, finalTick, chargeTicks)
  → サーバー: UseSkillPayload.handle
       - チャージ値検証
       - スキル存在確認
       - 派生スキルの単独使用チェック (SkillChainTracker.canAdvance)
       - 実行中スキルとのコンボ判定
       - 解放チェック / クリエイティブ / UnlockedSkill 設定
       - SP チェックと消費
       - SkillChainTracker.record + 派生チャイム
  → SkillExecutionManager.startSkillExecution
  → 毎サーバー tick: SwordSkill.onServerTick
  → SkillExecutionManager.handleSkillExecution
  → DataDrivenSkill.tick
       - motions / hitboxes / projectiles / sounds / animations / player_effects
       - 命中 → SkillUtils.BaseDamage → ダメージ
  → クライアントへ同期 payload
       - PlayAnimationPayload, SkillHitEffectPayload, SkillShockwavePayload, SyncSPPayload など
```

---

## 4. `server/handler`

| ファイル | 役割 |
| --- | --- |
| `SkillExecutionManager.java` | スキル実行セッションの開始・tick 駆動・終了。`skillExecutions`、RUSH フェーズ、重量ペナルティ |
| `SkillChainTracker.java` | コンボ受付ウィンドウ。`record` / `canAdvance`。派生スキルの単独使用を拒否 |
| `ServerModHandler.java` | Mod バス側サーバーイベント |
| `ServerEventHandler.java` | NeoForge サーバーイベント（kill XP、SP 同期など） |
| `HitstopManager.java` | ヒットストップのサーバー側管理 |
| `SkillClashManager.java` | 衝突・鍔迫り合いの判定、固定、自動勝敗、飛翔斬撃の相殺。ダメージ差が `clashLockThreshold` 以上ならロックせず火花＋強い側の一方的勝ち（`oneSidedWin`） |

---

## 5. `payload`（ネットワーク）

| ファイル | 役割 |
| --- | --- |
| `PayloadRegistryHandler.java` | 全 payload の登録 |
| `UseSkillPayload.java` | クライアント → サーバー。スキル発動要求。検証・SP 消費・コンボ記録 |
| `SkillRequestPayload.java` | スキル情報要求 |
| `SkillSelectionPayload.java` | スキル選択の同期 |
| `SkillSlotSelectionPayload.java` | スロット選択 |
| `SkillSlotInfoPayload.java` | スロット情報 |
| `SkillLoadSlotPayload.java` | ロード済みスロット |
| `SkillShockwavePayload.java` | 衝撃波 VFX |
| `SkillHitEffectPayload.java` | ヒットエフェクト VFX |
| `PlayAnimationPayload.java` | PlayerAnimator アニメーション再生指示 |
| `HitstopPayload.java` | ヒットストップ |
| `SyncDataSkillsPayload.java` | skilldata 定義の同期（バッチ） |
| `SyncSPPayload.java` | 現在 SP / 最大 SP |
| `SyncUnlockedSkillsPayload.java` | 解放済みスキル |
| `SyncProficiencyPayload.java` | 熟練度 |
| `SyncSkillLevelsPayload.java` | スキルレベル |
| `SyncSkillIndexPayload.java` | スキル index |
| `SyncPlayerLevelPayload.java` | プレイヤーレベル |
| `SyncSmithingDefsPayload.java` | 鍛冶定義（`SMITHING=false` では送らない） |
| `CheckSkillUnlockedPayload.java` | 解放確認 |
| `RequestUnlockSkillPayload.java` | 解放要求 |
| `RequestSkillLevelUpPayload.java` | スキルレベルアップ要求 |
| `RequestSkillLevelsPayload.java` | スキルレベル要求 |
| `RequestPlayerLevelPayload.java` | プレイヤーレベル要求 |
| `AllocateStatPayload.java` | STR / AGI 割り振り |
| `SkillClashPayload.java` | 鍔迫り合いの開始／終了、飛翔斬撃の相殺通知（S2C） |
---

## 6. `proficiency`（熟練度）

| ファイル | 役割 |
| --- | --- |
| `ProficiencyManager.java` | カテゴリ別熟練度、100 で 1 ポイント、スキル解放、kill 時加算、同期 payload 生成 |
| `ProficiencyUnlockTable.java` | `data/<ns>/proficiency/<category>.json` の読み込みと `unlock_proficiency` への適用 |

---

## 7. `playerlevel` / `skilllevel`

| ファイル | 役割 |
| --- | --- |
| `playerlevel/PlayerLevelManager.java` | プレイヤーレベル、XP、SP ボーナス、ステータスポイント、`enabled()` ゲート、保存 |
| `playerlevel/CombatStats.java` | STR 攻撃倍率、AGI 移動速度、敵レベル差補正 |
| `playerlevel/EnemyLevel.java` | 敵レベルの永続データ |
| `playerlevel/StatType.java` | `STR` / `AGI` |
| `skilllevel/SkillLevelManager.java` | スキルごとの Lv1–10、倍率カーブ、保存・同期 |

---

## 8. `client/handler`

| ファイル | 役割 |
| --- | --- |
| `ClientModHandler.java` | クライアント側 Mod バス登録 |
| `ClientForgeHandler.java` | キー入力、スキル発動要求の送信、クライアント側チェック |
| `ClientTickHandler.java` | クライアント tick、チャージ処理、SP 表示 |
| `ClientSkillSlotHandler.java` | スロット選択キー |
| `ClientUnlockedSkillsHandler.java` | 解放済みスキルのクライアントキャッシュ |
| `ClientProficiencyHandler.java` | 熟練度キャッシュ |
| `ClientSkillLevelHandler.java` | スキルレベルキャッシュ |
| `ClientPlayerLevelHandler.java` | プレイヤーレベルキャッシュ |
| `WeaponTrailHandler.java` | 武器トレイルのサンプリング（PlayerAnimator 適用後）、スパーク生成。スキルを振るマネキンのトレイルもここで描画 |
| `SkillHitEffectHandler.java` | ヒットエフェクト描画 |
| `ShockwaveHandler.java` | 衝撃波描画 |
| `HitstopHandler.java` | クライアント側ヒットストップ |
| `SkillClashHandler.java` | 鍔迫り合いの火花（`spark_streak` の光の筋）・アニメーション・ゲージ進捗 |
| `MannequinSkillAnimations.java` | マネキンのスキルアニメーションを `player_animation` から自前評価して `PlayerModel` に適用（無い場合は簡易斬撃） |

---

## 9. `client/gui`

| ファイル | 役割 |
| --- | --- |
| `SwordSkillSelectionScreen.java` | スキル選択画面。下部スロット常時表示、テーマ切替、派生表示 |
| `ProficiencyOverviewScreen.java` | 熟練度一覧（`K`） |
| `StatusScreen.java` | ステータス画面（`STATUS_SCREEN=false` で未使用） |
| `DecideSkillScreen.java` | スキル決定／確認系画面 |
| `Hud_Registry.java` | HUD 登録 |
| `HudPositionSettingScreen.java` | HUD 位置調整 |
| `SaoTheme.java` | ライト／ダークテーマ（色、オレンジアクセント） |
| `SaoShapes.java` | 角丸・図形描画ヘルパー |
| `SaoButton.java` | バニラ風ボタン |
| `SmithingForgeScreen.java` | 鍛冶台 GUI（`SMITHING=false`） |
| `MeltingForgeScreen.java` | 溶解炉 GUI（`SMITHING=false`） |

---

## 10. `client/screen`, `client/particle`

| ファイル | 役割 |
| --- | --- |
| `client/screen/Keybindings.java` | キー割り当て定義（`SwordSkill_Use_Key`, `_0`–`_4`, `status_key`） |
| `client/particle/SkillSparkParticle.java` | スキルスパーク |
| `client/particle/SparkStreakParticle.java` | 先端白熱のスパークストリーク |
| `client/particle/SkillParticleRenderTypes.java` | パーティクル描画タイプ |

---

## 11. `data`（データローダ）

| ファイル | 役割 |
| --- | --- |
| `SkillDataLoader.java` | `data/<ns>/skilldata/*.json` の読み込み・検証・同期フラグ |
| `SkillDataFetcher.java` | 解放判定などの取得ヘルパー |
| `DataManager.java` | データ全般の管理 |
| `WeaponTypeDataLoader.java` | `weapon_types/*.json` の読み込み |
| `WeaponTypeDetector.java` | アイテムから武器種を判定 |
| `WeaponTypeUtils.java` | 武器種ユーティリティ |
| `WeaponData.java` | 武器データ record |
| `AutoWeaponDataSetter.java` | 武器データの自動設定 |

---

## 12. `item` / `entity` / `effects` / `particle` / `config` / `command` / `mixin`

| ファイル | 役割 |
| --- | --- |
| `item/SampleItemRegistry.java` | サンプル武器・アイテム登録。スキルオーブは削除済み |
| `item/SampleWeapon.java` | サンプル武器アイテム |
| `item/SkillUnlockItem.java` | スキル解放アイテム |
| `entity/SwordSkill_Entities.java` | EntityType 登録（`attack_effect`, `skill_mannequin`） |
| `entity/custom/AttackEffectEntity.java` | 飛翔斬撃エンティティ |
| `entity/custom/SkillMannequinEntity.java` | 訓練用マネキン（鍔迫り合いの実装テスト用。右クリックで押し込み力切替、`skill` コマンドで常時使用スキルを設定し、スキル間隔ごとにアニメーション／トレイルを再生しつつ鍔迫り合いを仕掛ける） |
| `entity/model/AttackEffectModel.java` | 飛翔斬撃モデル |
| `entity/renderer/AttackEffectRenderer.java` | 飛翔斬撃レンダラ |
| `entity/renderer/SkillMannequinRenderer.java` | 訓練用マネキンのレンダラ（PlayerModel + wide/steve、ロック中は腕を突き出す、スキル使用中はアニメーションを適用） |
| `entity/renderer/skillRenderer/BlueEffectRenderer.java` | 青系エフェクト描画 |
| `effects/EffectRegistry.java` | MobEffect 登録 |
| `effects/NoFallDamageEffect.java` | 落下ダメージ無効 |
| `effects/SwordSkillAttribute.java` | 最大 SP などの Attribute |
| `particle/SkillParticles.java` | パーティクル登録 |
| `particle/SkillSparkParticleType.java` / `Options.java` | スキルスパーク型 |
| `particle/SparkStreakParticleType.java` / `Options.java` | スパークストリーク型 |
| `config/ServerConfig.java` | サーバー設定（`UnlockedSkill`, `hitstopTicks`, `levelSystemEnabled`, 鍛冶バランスなど） |
| `config/ClientConfig.java` | クライアント設定（HUD, テーマ, キー表示など） |
| `command/SwordSkillCommands.java` | `/swordskill` コマンド（mannequin, level, playerlevel, stat, enemylevel, forge など） |
| `mixin/ItemRendererMixin.java` | アイテム描画フック（トレイル用の持ち手姿勢サンプリングなど） |
| `network/toClient/ClientSPData.java` | クライアント側 SP キャッシュ |

---

## 13. `smithing`（現在は無効）

`SwordSkillFeatures.SMITHING=false` のため登録されない。コードは残っている。

| ファイル | 役割 |
| --- | --- |
| `SmithingRegistry.java` | 鍛冶ブロック・アイテム・メニュー登録（フラグで停止） |
| `SmithingDataLoader.java` | 素材／部品／パターン JSON 読み込み |
| `SmithingDefinitions.java` | 鍛冶定義の保持 |
| `SmithingMaterial.java` | 素材定義 |
| `SmithingPart.java` / `SmithingPartKind.java` | 部品定義 |
| `WeaponPattern.java` | 5×5 の武器パターン |
| `SmithingCombos.java` | 組み合わせ → モデル index |
| `SmithingForge.java` / `SmithingForgeBlock.java` / `SmithingForgeMenu.java` | 鍛冶台 |
| `MeltingForgeBlock.java` / `MeltingForgeMenu.java` | 溶解炉 |
| `ForgedWeaponItem.java` / `ForgedWeaponData.java` | 鍛造武器とデータコンポーネント |
| `ForgedIngotItem.java` | 溶解インゴット |
| `ForgedWeaponItem` 関連: `WeaponMemory.java`, `WeaponMemoryCategory.java`, `HiddenParams.java` | 武器記憶・隠しパラメータ |
| `SmithingCombat.java` | 鍛造武器の戦闘補正 |
| `SmithingEquipmentHandler.java` | STR 装備ゲート、重量ペナルティ、スキル速度倍率 |
| `SmithingRole.java` | 刀身／鍔／柄の役割 |
| `SmithingColorSlot.java` | 着色スロット |
| `SmithingTooltip.java` | ツールチップ表示 |

---

## 14. リソース配置

### `src/main/resources/assets/swordskill/`

| パス | 内容 |
| --- | --- |
| `player_animation/*.json` | プレイヤーアニメーション（約 100 ファイル）。skilldata の `animation` と対応。`clash_lock.json` は鍔迫り合い用の暫定ポーズ |
| `particles/*.json` | スパークなどのパーティクル定義 |
| `lang/en_us.json`, `lang/ja_jp.json` | 表示名・ツールチップ |
| `blockstates/`, `models/`, `textures/` | 鍛冶ブロック・鍛造武器・UI 用 |

### `src/main/resources/data/swordskill/`

| パス | 内容 |
| --- | --- |
| `skilldata/*.json` | スキル定義（92 ファイル。85 スキル + `how_to_use` / `test` など） |
| `proficiency/*.json` | 武器カテゴリ別の解放コスト上書き（11 カテゴリ） |
| `weapon_types/*.json` | 武器種判定データ（46 ファイル） |
| `weapon_types_sample/*.json` | サンプル武器種（10 ファイル） |
| `smithing_material/*.json` | 鍛冶素材（10 ファイル、無効） |
| `smithing_part/*.json` | 鍛冶部品（16 ファイル、無効） |
| `weapon_pattern/*.json` | 武器パターン（10 ファイル、無効） |
| `loot_table/` | ルートテーブル |
| `../minecraft/tags/` | バニラ tags への追加（剣判定など） |

### 保存データ

- プレイヤーデータ: `world/swordskill_data/<uuid>.json`
  - `skilllevels`
  - `player_level`, `player_level_xp`, `player_str`, `player_agi`
  - 熟練度・解放状況など（実装は `ProficiencyManager` / `PlayerLevelManager`）

---

## 15. ツール

### `tools/skilldata/`

| ファイル | 用途 |
| --- | --- |
| `validate_v2.py` | skilldata スキーマ検証（主要） |
| `validate_proficiency.py` | 熟練度上書き検証 |
| `validate_json.py` | JSON 構文検証 |
| `convert_v2.py` | v2 形式への変換 |
| `convert_java_skills.py` / `test_convert_java_skills.py` | 旧 Java スキルからの移行（完了済み・履歴用） |
| `generate_all_skilldata.py` | skilldata 一括生成 |
| `restore_skill_data.py` | 復元 |
| `make_checklist.py` | チェックリスト生成 |
| `analyze_report.py` | 分析レポート |
| `check_unparsed.py` / `debug_unparsed.py` | 未パース検出 |
| `debug_hiougi.py` / `debug_hiougi2.py` / `debug_talon.py` | 個別デバッグ（履歴用） |
| `clean_claw_json.py` | 爪スキルの整形 |
| `refine_claw_and_skills.py` | 爪スキル調整 |
| `fix_duration.py` | duration 修正 |
| `patch_datadriven_skill.py` / `patch_loader.py` | パッチ適用（履歴用） |
| `skill_interp.py` | 補間確認 |
| `update_target_motions.py` | モーション更新 |
| `update_zekku_howling.py` | 個別更新 |
| `verify_motions.py` | モーション検証 |

> `validate_*.py` 以外は一度きりの移行・デバッグ用。再実行する前に中身を確認する。

### `tools/blockbench/`

| ファイル | 用途 |
| --- | --- |
| `swordskill_skill_creator.js` | Blockbench プラグイン本体（v2.19.4） |
| `README.md` | インストール・操作説明 |
| `examples/` | サンプルデータ |

---

## 16. Blockbench / モデル関連の既知事項

- プラグインは `update_selection` を購読しない（無限再帰防止）。
- 旧マーカーは末尾の `;` や空白を許容。アニメーション選択時に正規化。
- インポート時に最初の攻撃／飛翔斬撃キーフレームを自動選択し、カメラをヒットボックスにフィット。
- プレビューボックスは `DoubleSide`。
- 未使用ヒットボックスフィールド `rotation`, `duration_ticks`, `travel_distance`, `particle` を保持。
- GeckoLib コーデックは import/export で X/Y 回転と X 位置を反転。
- `snake_bite` の右腕不一致は古いキーフレームが原因。
- `rightItem` のプレビュー軸問題は未解決（`rightItemFix` 親グループ版を検証中）。
- ゲームは `.bbmodel` を読まず、エクスポートした `player_animation/*.json` を読む。

---

## 17. ビルド・実行

```powershell
# ビルド
cmd /c '"D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\gradlew.bat" -p "D:\MOD\Mods\NeoForge[1.21.1]\Swordskill" build --console=plain'

# クライアント実行前
cmd /c '"D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\gradlew.bat" -p "D:\MOD\Mods\NeoForge[1.21.1]\Swordskill" prepareClientRun'

# データ検証
python tools/skilldata/validate_v2.py
python tools/skilldata/validate_proficiency.py
```

- `gradlew clean` は `build/moddev/*/` を削除する。その後 `prepareClientRun` が必要。
- 実行ログ: `run/logs/latest.log`, `run/logs/debug.log`
