# swordskill
SwordSkillはZomaによって作成されたMinecraftのModです。
「ソードアートオンライン」にて登場するソードスキルを使用することができます。
SwordSkill is a Minecraft mod created by Zoma.
You can use the sword skills that appear in "Sword Art Online".

## Future plans
他の武器種のソードスキルの追加。
別Modの武器でもソードスキルを使用できるように調整
Added sword skills for other weapon types.
Adjusted so that sword skills can be used with weapons from other mods.

## HUD Setting
設定にあるHUD設定キーを押しWASDで位置を調整可能
configのbackupが生成されるため何度の調整することは推奨しません。
You can adjust the position by pressing the HUD setting key in the settings and using WASD.
Since a backup of the configuration is generated, it is not recommended to adjust it repeatedly.

## Skill Level (スキルレベル)
スキルごとに 1〜10 のレベルを持ち、レベルに応じてダメージ倍率が上昇します（Lv1 = ×1.00、Lv10 = ×3.00 の線形成長）。
Each skill has its own level (1–10). The damage multiplier grows linearly from ×1.00 at Lv1 to ×3.00 at Lv10.

- レベルはプレイヤーごと・スキルごとに `world/swordskill_data/<uuid>.json` の `skilllevels` に保存されます。
- レベル倍率は武器攻撃力を含む基礎ダメージにのみ掛かります（SPコストやクールダウンは変化しません）。
- 倍率カーブは config の `skillLevelMax` / `skillLevelMinMultiplier` / `skillLevelMaxMultiplier` で変更できます。

コマンド (要 OP 権限レベル2):
```
/swordskill level get <targets>
/swordskill level set <targets> <skill_name> <level>
/swordskill level add <targets> <skill_name> <amount>
```
例: `/swordskill level set @s double_circular 10`

選択画面 (SwordSkillSelectionScreen) では、現在のスキルレベル・倍率と、段（ヒット）ごとの攻撃倍率（`1段目 200%→300%` の形式）を確認できます。
HUD の SP バー下にも、選択中スロットのスキルの `Lv.x ×y.yy` が表示されます（config の `hudShowSkillLevel` で無効化可能）。
選択画面・ステータス画面・熟練度一覧画面はライト／ダークの 2 テーマに対応しており、各画面右上のランプボタンで切り替えられます（状態は config の `guiDarkTheme` に保存）。

## Player Level (プレイヤーレベル)
SAO のキャラクターレベルに相当する、プレイヤーごとのレベルです。レベルアップで得られるのは **ステータスポイント**と最大 SP だけで、レベルそのものは HP も火力も増やしません。攻撃力は STR、移動速度は AGI に割り振って伸ばし、防御は防具（バニラの防御力）が担当します。
Player level (SAO-style character level). Levelling grants **stat points** and max SP only — never raw HP or damage. Offence comes from STR, movement from AGI, and defence is handled by armour (vanilla armour value).

- 上昇: モンスター撃破で XP を獲得します（`levelXpPerKill`、既定 25）。`levelXpFromMonstersOnly` を false にすると動物などでも獲得できます。XP は剣スキル以外で倒しても入ります。
- 必要 XP: レベル `L` を抜けるのに `levelXpBase * L^levelXpExponent`（既定 100 × L^1.25）。
- 報酬: レベルアップごとにステータスポイント（`statPointsPerLevel`、既定 3）と最大 SP（`levelSpPerLevel` ずつ、`levelSpBonusCap` で頭打ち）。レベルによる最大 HP ボーナスはありません。
- 上限: `playerLevelMax`（既定 100）。
- 保存先: `world/swordskill_data/<uuid>.json` の `player_level` / `player_level_xp` / `player_str` / `player_agi`。
- 割り振り: 未使用ポイントはレベルから決まる総ポイント（`statPointsPerLevel × (レベル − 1)`）から割り振り済みを引いた値。使い切ったかは `getAvailablePoints` で判定します。
- 予定: 階層・クエスト・スキルスロットの解放ゲート。

### Status (ステータス)
**一次リリースではステータス（レベル／STR・AGI）を無効化しています**（`SwordSkillFeatures.STATUS_SCREEN = false`、`levelSystemEnabled` 既定 `false`）。モンスター撃破でのレベルアップ、ステータスポイント、レベル由来の最大SP増加、STRの与ダメージ補正、AGIの移動速度補正はすべて停止します。**熟練度（プロフィシエンシー）はレベルとは独立しているため、そのまま動作します。** 無効時は `K`（`key.swordskill.status_key`）で **熟練度一覧画面** が直接開きます。

有効化する場合は `SwordSkillFeatures.STATUS_SCREEN` を `true` に、`config/swordskill-server.toml` の `levelSystemEnabled` を `true` にしてください。有効時の内容: レベル・XP バー・未使用ポイントを表示し、STR / AGI に好きなだけ割り振れます（`+` ボタン、Shift で 10 ずつ）。右側には **簡易的な戦闘ステータス**（攻撃力・防御力・移動速度・最大HP・最大SP）を表示します。ここから「熟練度一覧」ボタンで熟練度メニューへ移動できます。

| ステータス | 効果 | config |
| --- | --- | --- |
| STR | ソードスキルの与ダメージ倍率（`+strDamagePerPoint`/点、`strMaxBonus` で頭打ち）。鍛造武器の装備ゲート（重さ → 必要 STR）も兼ねる | `strDamagePerPoint` (0.005) / `strMaxBonus` (0.5) |
| AGI | 移動速度（`agilityMoveSpeedPerPoint`/点、`agilityMaxMoveSpeedBonus` で頭打ち） | `agilityMoveSpeedPerPoint` (0.003) / `agilityMaxMoveSpeedBonus` (0.3) |

STR はバニラの攻撃力属性ではなく、独自の計算式（`CombatStats`）に組み込んでいます。**レベル差補正**（`levelDiffPerPoint`/差1、`levelDiffCap` で打ち切り）も同じ式に入っており、`EnemyLevel`（敵レベル）を持つエンティティに対してだけ作用します。レベル未設定のエンティティ（他 MOD のモブやバニラのモブ）には一切かかりません。
- 与ダメージ: `1 + STR補正` × `1 + レベル差 × levelDiffPerPoint`（ソードスキルは `SkillUtils.BaseDamage`、レベル差は被弾時に適用）。
- 被ダメージ: ステータスによる軽減はありません（防具の防御力のみ）。
- 敵レベルはエンティティの永続データ（`SwordSkillEnemyLevel`）に保存され、`/swordskill enemylevel set` で付与できます（階層・モブ定義からの自動付与は今後）。

コマンド (要 OP 権限レベル2):
```
/swordskill playerlevel get <targets>
/swordskill playerlevel set <targets> <level>
/swordskill playerlevel add <targets> <amount>
/swordskill playerlevel xp <targets> <amount>
/swordskill stat get <targets>
/swordskill stat set <targets> <stat> <amount>
/swordskill stat add <targets> <stat> <amount>
/swordskill enemylevel get <targets>
/swordskill enemylevel set <targets> <level>
```
例: `/swordskill playerlevel xp @s 500` / `/swordskill stat add @s STR 5` / `/swordskill enemylevel set @e[type=zombie,limit=1] 10`

## Smithing (鍛冶システム)
> **一次リリースでは無効化しています。** `SwordSkillFeatures.SMITHING = false` の間は、鍛冶台・溶解炉・鍛造武器・インゴットを**登録しない**ため、アイテムとしても存在しません（レシピ・コマンドも無効）。再有効化するときは `SwordSkillFeatures.SMITHING` を `true` に戻し、`recipe/smithing_forge.json` / `recipe/melting_forge.json` を復元してください。以下の記述は有効化したときの仕様です。

武器を何百種類も作らず、**部品（刀身・鍔・柄）と素材の組み合わせ**で武器を作るシステムです。武器アイテムは `swordskill:forged_weapon` の 1 種類だけで、中身は 1.21 のデータコンポーネント（`swordskill:forge`）に持たせます。見た目と着色は部品テクスチャ＋ `tintindex 0/1`（メインカラー／サブカラー）で行います。
Data-driven forging: one item (`swordskill:forged_weapon`) whose stats, colours, STR requirement and memory all live in its data components.

- 定義: `data/<namespace>/smithing_material/<id>.json`（素材）/ `smithing_part/<id>.json`（部品）/ `weapon_pattern/<id>.json`（武器の形）。データパックで追加でき、**敵実装後に素材アイテムを足すときは `items` / `tags` に追記するだけ**でコード変更は不要です。
- 作成（作業台ベース・1 段階）: 鍛冶台の **5×5 のマス**に素材を並べるだけで作れます。マスの並び（形）が武器種を決め、**下の補強スロット 3 つ**が仕上げを決めます。
  - 形は `weapon_pattern/<id>.json` で定義します。`pattern`（5×5 の文字列）と `key`（文字 → 役割 `blade` / `guard` / `handle`）を持ち、どのマスも素材定義に載っているアイテムで埋まっていれば成立します。
  - 一致判定はバニラの shaped レシピと同じで、**上下左右のずらし**と**左右反転**を許します。形の外に物があると一致しません。ID 順に見て最初に一致した形を採用します。
  - 既定の形は武器種ごとに 1 つ、**10 種**です（`one_handed_sword` / `two_handed_sword` / `katana` / `axe` / `rapier` / `dagger` / `claw` / `spear` / `mace` / `scythe`）。両手剣は刀身 3 + 鍔 2 + 柄 1 のように、強い武器ほど必要な素材が多くなります。
- 見た目（素材が部品テクスチャを直接指定）: 素材 JSON の `blades` / `guard` / `handle` が使う部品を決めます。優先順位は **素材 > 形の既定 > ID 順で最初の部品** です。
  - `blades` は武器種ごとの上書き（例 `"blades": { "axe": "swordskill:blade_axe" }`）、`guard` / `handle` は全武器種共通の指定です。
  - すでに `wood` = 巻柄、`obsidian` / `crystal` = 装飾鍔、`crystal` = 長柄 を指定してあります。
  - 役割（刀身／鍔／柄）のマスが無い形では、その役割の素材は刀身の素材で代用します。
- 部品テクスチャ: `assets/swordskill/textures/item/forged_part/<部品>.png`（16×16、合成用レイヤー）を**モデルの要素として重ね**、`tintindex 0 = メインカラー / 1 = サブカラー` で着色します。組み合わせごとのモデル `models/item/forged/<組み合わせ>.json` を `forged_weapon.json` の `overrides`（`custom_model_data`）で切り替えます。番号は `SmithingCombos#modelIndex`（刀身 ID 順 × 鍔 × 柄の辞書順）で決まり、サーバーとクライアントで必ず一致します。
- 強さ: `(forgeBaseDamage + 素材の power × forgeMaterialPowerScale) × 部品の power 係数 × 品質 × 強化値 × 特殊素材`。素材の `tier` が高いほど強い武器になります。
- 品質: 鍛冶スキル（その武器種の熟練度）から算出します（`quality = 熟練度 / qualityProficiencyPerPoint`、`qualityMax` で頭打ち）。
- 重さと必要 STR: `素材の weight × 部品の weight 係数の合計` に `strPerWeight`（既定 0.1）を掛けた値（切り上げ）が **必要 STR** です。既定値では片手剣で 木 1 / 石 2 / 銅・鉄 3 / ダイヤ 4 / ネザライト・黒曜石 5 程度で、**鉄武器までは STR 0〜3** に収まります（厳しくしたいときは `strPerWeight` を上げてください）。
- 補強スロット（下の 3 つ）: 置いたアイテムで効果が変わります。
  - **溶解インゴット** (`swordskill:forged_ingot`) … 武器記憶を引き継ぎます（複数可・合算）。溶かして鍛え直すと、育った隠しパラメータをそのまま受け継げます。
  - **特殊素材**（素材 JSON に `"special": true`）… 能力（色とダメージ倍率）を付与します。複数置いたときは最初の 1 つだけ有効です。
  - **それ以外の素材** … 威力の底上げです。`素材の power × reinforcementPowerRatio`（既定 0.15）を加算します（重量は変わらないので STR 要件は据え置き）。
- 装備ゲート:
  - `STR ≧ 必要量` … 通常どおり装備できます。
  - `必要量 ÷ 2 ≦ STR < 必要量` … 攻撃速度（`overweightAttackSpeedPenalty`、既定 −30%）と**ソードスキル速度**（`overweightSkillSpeedMultiplier`、既定 0.7＝3割遅い）が低下します。
  - `STR < 必要量 ÷ 2` … 装備できず、持った瞬間に弾かれて落ちます。
- 武器記憶: 鍛造武器は与えた／受けたダメージと攻撃の傾向を記録します。傾向はスキルのヒットボックス形状（突き／凪払い）と飛翔斬撃（`AttackEffectEntity`）、スキルのクールダウン（短CT）から自動分類します。
  - 導出される隠しパラメータ: **突き威力 / 凪払い威力 / 飛翔斬撃威力 / クリティカルダメージ / クールダウン短縮**（`memory*PerHit` と `memory*Cap`）。
  - クリティカルは鍛造武器に新設した機構です（`critBaseChance`、`critBaseMultiplier`）。
  - 確認は `swordskill forge memory` のみで、数値は武器の正体（`swordskill:forge` タグ）に保存されます。
- 強化: `forgeEnhanceMax` まで強化でき、1 段階ごとに作成素材の `enhancementCostRatio`（既定 1 割）を消費します（`/swordskill forge enhance`）。

### 鍛冶台 GUI (`swordskill:smithing_forge`)

- ブロック: `swordskill:smithing_forge`。右クリックで GUI が開きます。ブロックエンティティは持たず、材料は GUI（`SmithingForgeMenu`）側で管理します。
- 入手: クラフトレシピ（鉄インゴット8 + 作業台1）または `/give @s swordskill:smithing_forge`。
- スロット（**1 画面に統合・モード切替なし**）: `5×5 = 25`（作業グリッド）+ `補強 ×3` + `完成 ×1` + プレイヤーインベントリ 36。結果はサーバー側で計算し、通常のスロット同期でクライアントへ配ります（品質が熟練度依存のため）。
- 操作: グリッドに形どおり素材を置き、補強スロットに仕上げのアイテムを入れると完成スロットに武器が出ます。取り出すとマスと補強スロットから 1 個ずつ消費します。
- 部品: 部品は素材 JSON の中の見た目・性能データです（**素材の段階でスロットに入れるものではありません**）。旧・部品アイテム `swordskill:smithing_part` は廃止し、`/swordskill forge part` コマンド・テクスチャ・モデルも削除しました。
- 表示名: 鍛造武器の名前は「素材 + 武器種」です（例: `鉄の斧` / `Iron Axe`）。武器種が分からないデータのときだけ刀身の部品名にフォールバックします。刀身・鍔・柄はツールチップの `部品: 直刀身・素朴な鍔・巻柄` に表示します。
- ツールチップ: バニラの `AbstractContainerScreen` はスロットのアイテムツールチップを自動では描画しないため、`SmithingForgeScreen.render` から `renderTooltip(...)` を明示的に呼んでいます。
- 定義の同期: ログイン時とデータパック再読み込み時に `SyncSmithingDefsPayload` で素材・部品の定義をクライアントへ配ります（形はサーバー専用なので配りません）。

### 溶解炉 (`swordskill:melting_forge`)

- ブロック: `swordskill:melting_forge`。鍛造武器を 1 つ入れると `swordskill:forged_ingot` になり、**素材と武器記憶を保持**します。取り出したインゴットを鍛冶台の補強スロットに入れると、記憶を引き継いで鍛え直せます。
- 入手: クラフトレシピ（鉄インゴット4 + 石レンガ4 + かまど1）または `/give @s swordskill:melting_forge`。
- コマンドでも `swordskill forge melt` で同じことができます（手持ちの武器を溶かします）。

コマンド (要 OP 権限レベル2):
```
/swordskill forge create <targets> <pattern> [material]
/swordskill forge enhance <targets> <amount>
/swordskill forge melt <targets>
/swordskill forge memory <targets>
/swordskill forge defs
```
`<material>` は省略すると `iron` です。`<pattern>` / `<material>` は `swordskill:` を省略した ID でも指定できます。
例: `/swordskill forge create @s one_handed_sword netherite` → `/swordskill forge memory @s`

作成の手順（ゲーム内）:
1. `/give @s swordskill:smithing_forge` で入手して設置し、右クリックで GUI を開きます。
2. **形どおり**に素材を 5×5 のマスへ置きます（例: 片手剣は中央縦 3 マス + その下の左右に鍔 + 中央下に柄）。
3. 必要なら**補強スロット**にインゴット（記憶の継承）や特殊素材（能力付与）、底上げ素材を入れます。
4. 完成スロットに武器が出るので取り出します。

既定の形（`B` = 刀身 / `G` = 鍔 / `H` = 柄。`·` は空きマス。左右反転・上下左右のずらしは可）:

| 形 (pattern) | 武器種 | 形 |
| --- | --- | --- |
| `one_handed_sword` | 片手剣 | `··B··` / `··B··` / `··B··` / `G·B·G` / `··H··` |
| `two_handed_sword` | 両手剣 | `··B··` / `··B··` / `··B··` / `GBBBG` / `··H··` |
| `katana` | 刀 | `···B·` / `··B··` / `··B··` / `G·B··` / `··H··` |
| `axe` | 斧 | `BB···` / `BBB··` / `·BB··` / `··H··` / `··H··` |
| `rapier` | レイピア | `··B··` / `··B··` / `··B··` / `··B··` / `G·H·G` |
| `dagger` | 短剣 | `··B··` / `··B··` / `G·B·G` / `··H··` |
| `claw` | 爪 | `B·B·B` / `·B·B·` / `··H··` / `··H··` |
| `spear` | 槍 | `··B··` / `··B··` / `··H··` / `··H··` / `··H··` |
| `mace` | 棍棒 | `·BB··` / `BBBB·` / `·BB··` / `··H··` / `··H··` |
| `scythe` | 大鎌 | `BBBBB` / `···B·` / `··H··` / `··H··` / `··H··` |

**注意**: 形はどれか 1 つにだけ一致させる必要があります。別の形と素材を混ぜると一致しません（例: 鍔マスに鍔を 1 つだけ置くと両手剣ではなく片手剣になります）。追加したい形は `weapon_pattern/<id>.json` を足すだけです。

## Proficiency (熟練度)
武器種などのカテゴリごとに貯まる熟練度です。将来的に料理・投擲・鍛冶などのカテゴリを追加する前提の作りになっています。
Per-player proficiency stored per category (weapon type for now; cooking / throwing / smithing are planned).

- 上昇: そのカテゴリのスキルでモンスターを撃破すると熟練度が加算されます（`proficiencyPerKill`、既定 1）。`proficiencyFromMonstersOnly` を false にすると動物などでも加算されます。料理・鍛冶などのカテゴリは、それぞれの行動に成功したときに `ProficiencyManager.addProficiency(...)` を呼ぶ形で追加できます。
- レベルアップ: 熟練度が `proficiencyPerLevelUp`（既定 100）貯まるごとに「レベルアップポイント」を1つ獲得します。選択画面の「レベルアップ (熟練度ポイント n)」ボタンで、**そのカテゴリのスキル**を1レベル上げられます（最大レベル・未解放・説明枠は対象外）。ポイントは「獲得済み − 使用済み」で管理し、レベルアップ以外では消費されません。
- 解放条件: スキル JSON の `unlock_proficiency` に数値を書くと、そのカテゴリの熟練度がその値以上でないと解放できなくなります（0 または未指定なら熟練度不要）。解放ボタンには必要な熟練度が表示され、足りない場合は警告が出ます（クリエイティブは免除）。
  - **データパックでカテゴリごとに上書きできます**（下記「修得熟練度の設定」）。書かなかったスキルは skilldata の `unlock_proficiency` が使われます。
  - 既定値の目安: 各スキルの攻撃倍率から `score = 最大単発倍率 × 1.5 + 全ヒット合計倍率` を計算し、`score < 5 → 0` / `< 8 → 50` / `< 11 → 100` / `< 15.5 → 150` / それ以上 `→ 200` で設定しています。武器種ごとに必ず1つは必要熟練度0の入門スキルが残るようにしています（SPEAR・RAPIER・KATANA は自動補正）。
  - **初期スキル**: 各武器カテゴリの単発スキル1つに `"default_unlocked": true` が付いており、ログイン時に **アンロックアイテムなしで自動解放** されます（片手剣 `slant` / 両手剣 `cascade` / 刀 `sakura_mai` / 斧 `wall_wind` / レイピア `spark_thrust` / 双剣 `end_revolver` / 槍 `thrusts` / 短剣 `canine` / 大鎌 `barrick` / 爪 `talon` / メイス `grand`）。熟練度はスキルでの撃破でのみ増えるため、最初の1つは無条件で使えるようにしています。
- 保存先: `world/swordskill_data/<uuid>.json` の `proficiencies`（カテゴリ→熟練度）と `proficiency_spent`（カテゴリ→使用済みポイント）。
- 表示: 選択画面のフッター右側に `片手剣 熟練度 142 (次まで 42/100) ▲1` の形式で表示されます。
- 熟練度一覧画面: `K`（`key.swordskill.status_key`、既定 `K`）で直接開けます（ステータス画面が有効なときは、そこの「熟練度一覧」ボタンからも移動できます）。左にカテゴリごとの熟練度・ポイント、右にそのカテゴリのスキルごとのレベル・解放状況・解放に必要な熟練度を一覧表示します（ホイールでスクロール、行は領域内でクリップされます）。

### 修得熟練度の設定（データパック）
`data/<namespace>/proficiency/<category>.json` に、スキル名＝必要熟練度を書きます。ファイル名がカテゴリ名（`one_handed_sword` / `two_handed_sword` / `katana` / `axe` / `rapier` / `claw` / `spear` / `mace` / `scythe` / `dagger` / `dualsword`）です。

```json
{
  "skills": {
    "slant": 50,
    "horizontal": 0,
    "sharp_neil": 100
  }
}
```

- 既定で 11 カテゴリ分のファイルを同梱しており、現在の `unlock_proficiency` と同じ値が入っています。数値を書き換えるだけで調整できます。
- ここに書いた値が優先されます（`skilldata` の `unlock_proficiency` より強い）。項目を消す／ファイルを消すと `skilldata` の値に戻ります。
- 値は 0〜1000000。`/reload` で反映され、サーバーからクライアントへも同期されます（表示中の必要熟練度も更新されます）。
- 同じカテゴリのファイル内に、そのカテゴリに存在しないスキル名を書いた場合はログに警告が出ます。

コマンド (要 OP 権限レベル2):
```
/swordskill proficiency get <targets>
/swordskill proficiency set <targets> <category> <amount>
/swordskill proficiency add <targets> <category> <amount>
```
`<category>` は武器種名（例: `ONE_HANDED_SWORD`, `DUALSWORD`, `AXE` …）。
例: `/swordskill proficiency add @s DUALSWORD 100`

## Skill Effects (斬撃エフェクト)
スキルのトレイル（weapon trail）と同色の火花・光輪・斬痕・フラッシュで命中を強調します。すべて config（`Spark Settings`）で個別に無効化できます。

- 発光粒子 `swordskill:skill_spark` を加算合成で描画します。色はスキル JSON の `color`（＝トレイル色）から取ります。
- 飛び道具の三日月（`AttackEffectEntity`）の描画色も、同じくスキル JSON の `color` に追従します（芯は白寄りに発光）。
- 火花 `swordskill:spark_streak`: 速度方向に伸びた光の筋として描画する火花です。色はトレイル色と同じで、先端だけ白熱させています。重力で落ちながら地面では跳ね返ります。
- 武器の火花 (`trailSparks`): スキルのアニメーション中、刃に沿って火花を散らします。刃の移動方向に乗せて飛ばすため、振り抜くほど流れます。
- 火花の筋 (`sparkStreaks`): 刃と命中位置に、上記の伸びる火花を追加します。
- 命中バースト (`hitSparks`): 命中位置（攻撃者側を向いた面）から同色の火花をバーストさせます。ダメージ倍率が高い技ほど量とサイズが増えます。
- 光輪 (`hitRing`): 命中位置で、攻撃方向に直交する面に沿って拡がる発光リングを出します。
- 斬痕 (`slashDecals`): 命中位置に短命の発光する斬撃マークを残します。ヒットストップで対象が止まるため、斬った場所に残って見えます。
- 画面フラッシュ (`hitFlash`): 命中させた本人にだけ、一瞬の白フラッシュを出します。
- `sparkDensity`（既定 1.0、0〜3）で火花の量をまとめて調整できます。0 で火花を止められます（光輪・斬痕・フラッシュは別設定）。
- 命中演出はサーバーから `swordskill:skill_hit_effect` で対象を追跡しているクライアントへ通知します（色・強さ・命中位置・方向）。同一tickの多重ヒットは火花のみで、光輪・斬痕・フラッシュは1回にまとめます。

### skilldata の追加キー
| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `proficiency_category` | string | `weapons` の先頭 | 熟練度カテゴリ。`weapons` と別のカテゴリにしたいときだけ指定 |
| `unlock_proficiency` | int | 0 | 解放に必要な熟練度（0 = 不要） |
| `default_unlocked` | bool | false | true にするとログイン時にアンロックアイテムなしで自動解放（各カテゴリの初期スキル用） |
| `assignable` | bool | true | false にするとスロットにセットできない（説明用エントリなど） |
| `invulnerability_ticks` | int | 0 | スキル開始時に与える無敵時間（`rush_combo` の値と大きい方を採用） |
| `end_skill_on_ground` | bool | false | 着地したtickでスキルを終了する（`finish` アニメーションを再生。RUSH のみ） |
| `animations` | array | 空 | 途中で切り替えるアニメーション（下記） |
| `player_effects` | array | 空 | 自分に付与するポーション効果（下記） |

`animations` の要素:

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `tick` | int | 必須 | 実行tick（`trigger` が `CONTACT` のときは接触tickからの経過） |
| `animation` | string | `""` | アニメーション名。`<skilldata の animation>.<名前>` を再生する。空文字で解除 |
| `trigger` | string | `TICK` | `TICK` / `CONTACT`（接触tick） / `GROUND`（着地tick） |
| `random_group` | string | 空 | 同じ名前のイベント群から1つだけを実行する |
| `random_index` | int | 0 | そのグループ内での番号（0 から連番） |

`player_effects` の要素:

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `tick` | int | 必須 | 実行tick（`trigger` が `CONTACT` のときは接触tickからの経過） |
| `effect` | string | 必須 | 効果ID（例 `swordskill:no_fall_damage`） |
| `duration_ticks` | int | 1 | 効果時間 |
| `amplifier` | int | 0 | 効果の強さ |
| `show_particles` | bool | false | 効果のパーティクルを表示する |
| `trigger` | string | `TICK` | `TICK` / `CONTACT` / `GROUND` |

### skilldata のイベント共通キー
`motions` / `hitboxes` / `projectiles` / `sounds` の要素は、種類を問わず次のキーを使えます。

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `trigger` | string | `TICK` | `TICK` / `CONTACT`（突進の接触tick）/ `GROUND`（着地tick） |

`motions` の要素は追加で:

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `set` | bool | false | true なら加算ではなく移動ベクトルを置き換える（一定速度で落下させる等） |
| `repeat` | bool | false | true なら `tick` 以降ずっと毎tick適用する |

`projectiles` の要素は追加で:

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `fall_damage_scale` | double | 0 | 0 より大きければ、落下距離（スキル中の最高到達点からの差）を倍率に掛ける |
| `path` | object | なし | 突進しながら伸びる斬撃（下記） |
| `random_group` / `random_index` | string / int | 空 / 0 | `animations` と同じ。同じグループなら**同じ出目**を共有する |
| `single_target` | bool | false | true なら範囲内で最も近い 1 体にだけダメージを与える |
| `aim_at_contact` | bool | false | `trigger: CONTACT` のとき、接触した相手の中心を自動で狙う。`rotation` は無視され、`offset[2]` が相手からの距離、`offset[1]` が足元からの高さになる |

`path` の要素（剣閃を始点から現在位置までの線に沿って伸ばす）:

| キー | 型 | 既定 | 内容 |
| --- | --- | --- | --- |
| `start_tick` | int | 1 | 始点を記録するtick |
| `position` | double | 0 | 始点→現在位置のどこに出すか（0 = 現在位置、0.5 = 中間） |
| `rotation` | bool | false | true なら向きを進行方向から決める |
| `size_scale` | double | 0 | 0 より大きければ `size` のZ成分を「移動量の平均 × この値」にする |

> `legacy_fallback`（旧 Java 実装を呼び出す仕組み）は廃止しました。`true` を書くと読み込みエラーになります。判断に迷う挙動は上記のキーか、`hitboxes` / `projectiles` の近似で表現します。

旧実装からデータ駆動へ移したときに近似した箇所:

- `rapid_bite`: 命中時にクールダウンを 1/5 にする旧処理は、クールダウンがクライアント側の表示用マップ（`ClientForgeHandler`）にしか無いため移植していません。斬撃は `aim_at_contact` で接触した相手を自動で狙い、`single_target` で 1 体にだけ当たります。
- `flashing_penetrator` / `mace_impact`: 発生位置はプレイヤーのローカル座標（右・上・前）基準のため、見上げ・見下ろし時は旧実装のワールドY基準とわずかにずれます。
- `gengetu`: 上段/下段の分岐は `random_group` + `random_index` で再現しています（アニメーションと斬撃が同じ出目を共有します）。

### Java でスキルを追加する（例外）
標準のスキルはすべて JSON（`data/<namespace>/skilldata/*.json`）で定義します。旧来の「1スキル1Javaクラス」形式は削除済みです。
どうしても Java でしか書けない挙動が必要な場合だけ、`ISkill` を実装したクラスを登録できます。

```java
// 共通初期化（クライアント・サーバーの両方で 1 回だけ）で登録する
SwordSkillRegistry.registerJavaSkill(new SkillData(
        85,                                   // ID。JSON 定義と合わせて 0 から連番になるようにする
        "my_skill",                           // 名前（アイコンは textures/gui/my_skill.png）
        100, 10.0,                            // クールダウン(tick), SP
        SkillData.SkillType.SIMPLE,           // SIMPLE / RUSH / TRANSFORM / TRANSFORM_FINISH
        MySkill.class,                        // ISkill の実装
        List.of(SkillData.WeaponType.ONE_HANDED_SWORD),
        false,                                // hidden
        20,                                   // duration ticks
        20));                                 // transform limit ticks
```

- スキル ID は選択画面が一覧の並び順として使うため、**0 から連番**にしてください（データ定義と併用するときはデータ側の最大 ID の次を使います）。
- 同じ ID のデータ定義（JSON）がある場合、`replace_builtin: true` なら JSON が優先されます。
- Java スキルはクライアントへ同期されません（同じ MOD jar が両側に入っている前提）。

## Selection Screen UI (選択画面)
SAO 風の UI になっています。SAO-styled skill selection screen.
- 武器種タブ: 上部の武器種アイコンをクリックすると、その武器種のスキルだけを表示します。もう一度同じタブをクリックすると装備中の武器種に戻ります。武器種が増えて入りきらない場合は `<` `>` の矢印クリック、またはタブ列の上でホイールすることでスクロールできます。
- 攻撃倍率: カード右側のパネルに、段（ヒット）ごとの倍率を `1段目 100%` の形式で縦に並べます。レベル倍率がかかっている場合は `1段目 100%→300%` のように適用後も併記します。9段以上など縦に収まらない場合は、パネルの上でホイールするとスクロールでき、右端にスクロールバーが出ます。
- 装備武器種の制限: タブで別の武器種のスキルを表示していても、**今装備している武器種では使えないスキルはスロットにセットできません**（スロットが灰色になり、クリックすると警告が出ます）。サーバー側でも同じ検証を行うため、改造クライアントからも設定できません。
- 派生スキル: TRANSFORM 系スキルでは、続くスキルが「派生:」のノード＋接続線のリンク図で表示されます（未習得は暗く表示、ホバーで名前を表示）。
- スロット: 下部の 5 つのひし形パネルがスキルスロットです。クリック（または 1〜5 キー）で選択中のスキルを割り当てます。現在使用中のスロットは明るい枠で強調されます。
- 説明文: 収まりきらない場合は `< 1 / 3 >` のようにページ表示され、マウスホイール・PageUp/PageDown・↑/↓・[ / ] でページを送れます。
- 熟練度: フッター右側に選択中スキルのカテゴリ熟練度と、レベルアップ可能ポイントを表示します。ポイントがあるときは菱形の上に「レベルアップ」ボタンが出ます。
- 説明（ガイド）: 先頭の「説明」エントリ（`how_to_use`）はスキルではなく、遊び方と熟練度の説明を表示する項目です（`assignable: false` のためスロットにはセットできません）。
- 切替演出: A/D でスキルを切り替えるとスライド＋フェード、スロット割り当てやアンロック、テーマ切替時にフラッシュが入ります。

## Supports weapon types from other mods
ConfigでアイテムIDに含まれる文字から武器種を獲得する仕組みに変更できます。
You can change the config to obtain the weapon type from the letters contained in the item ID.
- "great_sword" or "greatsword" or "claymore"なら両手剣
- "katana" or "cutlass" なら刀
- "axe"　なら斧
- "rapier"　ならレイピア
- "claw"　なら爪
- "spear"　または トライデントアイテム　なら　槍
- "mace" or "hammer" なら　メイス
- "scythe"　なら　鎌
- "dagger" または "short_sword"なら短剣
- "sword"　なら　片手剣
- これらに当てはまらないSwordItemは片手剣に分類するようになっています。
  Sword items that do not fit these criteria are classified as one-handed swords.

## Using DataPack
You can create data packs and make any weapon compatible.
Steps
1. Create a data pack
2. Create a weapon type json based on the [guide](https://github.com/zoma1101/swordskill/blob/main/src/main/java/com/zoma1101/SwordSkill/swordskills/SkillData.java)
   Please select weapon type from the list below.

`DataPackName/data/swordskill/weapon_types/youritems.json`
```
{
  "name" : "modname:youritems",
  "item" : ["modname:supersupersword",
    "modname:dark_repulsor",
    "modname:elucidator",
    "modname:aneal_blade",
    "modname2:god_sword",
    "modname3:yajuu_sword"
  ],
  "weapontype" : ["ONE_HANDED_SWORD","TWO_HANDED_SWORD",…]
}
```

From swordskill v1.1 onwards, if you only want to add default weapon types,
you can easily create data packs by just modifying the "item" list [here](https://github.com/zoma1101/swordskill/tree/main/src/main/resources/data/swordskill/weapon_types_sample):

```
{
  "name" : "axe",
  "item" : ["superitem:steel_axe","perfect:ruby_axe"...],
  "weapontype" : ["AXE"]
}
```



