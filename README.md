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
`K`（`key.swordskill.status_key`）、または選択画面の「ステータス」ボタンで **ステータス画面** が開きます。レベル・XP バー・未使用ポイントを表示し、STR / AGI に好きなだけ割り振れます（`+` ボタン、Shift で 10 ずつ）。右側には **簡易的な戦闘ステータス**（攻撃力・防御力・移動速度・最大HP・最大SP）を表示します。ここから「熟練度一覧」ボタンで熟練度メニューへ移動できます。

| ステータス | 効果 | config |
| --- | --- | --- |
| STR | ソードスキルの与ダメージ倍率（`+strDamagePerPoint`/点、`strMaxBonus` で頭打ち）。**予定**: 鍛冶システムと連動した装備重量の緩和 | `strDamagePerPoint` (0.005) / `strMaxBonus` (0.5) |
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
武器を何百種類も作らず、**部品（刀身・鍔・柄）と素材の組み合わせ**で武器を作るシステムです。武器アイテムは `swordskill:forged_weapon` の 1 種類だけで、中身は 1.21 のデータコンポーネント（`swordskill:forge`）に持たせます。見た目と着色は部品テクスチャ＋ `tintindex 0/1`（メインカラー／サブカラー）で行います。
Data-driven forging: one item (`swordskill:forged_weapon`) whose stats, colours, STR requirement and memory all live in its data components.

- 定義: `data/<namespace>/smithing_material/<id>.json`（素材）/ `smithing_part/<id>.json`（部品）/ `smithing_recipe/<id>.json`（レシピ）。データパックで追加でき、**敵実装後に素材アイテムを足すときは `items` / `tags` に追記するだけ**でコード変更は不要です。
- 作成（1 段階・レシピ駆動）: 鍛冶台の**メイン素材スロット**に素材を入れると、その素材で作れる**レシピ（= 武器 1 種）の一覧**が出ます。一覧から選ぶと**対応素材**がゴースト表示され、揃うと**完成スロット**に武器が出ます。
  - 一覧は素材の `tier` で絞られます（`レシピの tier ≦ 素材の tier`）。弱い素材では作れる武器が少なく、良い素材ほど上位のレシピが選べます。
  - レシピ 1 つ = 刀身 + 鍔（省略可）+ 柄の組み合わせで、`count`（メイン素材の必要数）と `materials`（対応素材: アイテム or タグ + 個数）を持ちます。
  - 既定のレシピは 刀身 10 × 鍔 4（無し含む）× 柄 3 = **120 種**です（生成スクリプトで作成。不要なものは JSON を消すだけ）。
- 見た目: `assets/swordskill/textures/item/forged_part/<部品>.png`（16×16、合成用レイヤー）を**モデルの要素として重ね**、`tintindex 0 = メインカラー / 1 = サブカラー` で着色します。組み合わせごとのモデル `models/item/forged/<組み合わせ>.json` を `forged_weapon.json` の `overrides`（`custom_model_data`）で切り替えます。番号は `SmithingCombos#modelIndex`（刀身 ID 順 × 鍔 × 柄の辞書順）で決まり、サーバーとクライアントで必ず一致します。
- 強さ: `素材の power × 部品の power 係数 × 品質 × 強化値`。素材の `tier` が高いほど強い武器になります。
- 品質: 鍛冶スキル（その武器種の熟練度）から算出します（`quality = 熟練度 / qualityProficiencyPerPoint`、`qualityMax` で頭打ち）。
- 重さと必要 STR: `素材の weight × 部品の weight 係数` に `strPerWeight` を掛けた値（切り上げ）が **必要 STR** です。
- 装備ゲート:
  - `STR ≧ 必要量` … 通常どおり装備できます。
  - `必要量 ÷ 2 ≦ STR < 必要量` … 攻撃速度（`overweightAttackSpeedPenalty`、既定 −30%）と**ソードスキル速度**（`overweightSkillSpeedMultiplier`、既定 0.7＝3割遅い）が低下します。
  - `STR < 必要量 ÷ 2` … 装備できず、持った瞬間に弾かれて落ちます。
- 武器記憶: 鍛造武器は与えた／受けたダメージと攻撃の傾向を記録します。傾向はスキルのヒットボックス形状（突き／凪払い）と飛翔斬撃（`AttackEffectEntity`）、スキルのクールダウン（短CT）から自動分類します。
  - 導出される隠しパラメータ: **突き威力 / 凪払い威力 / 飛翔斬撃威力 / クリティカルダメージ / クールダウン短縮**（`memory*PerHit` と `memory*Cap`）。
  - クリティカルは鍛造武器に新設した機構です（`critBaseChance`、`critBaseMultiplier`）。
- 溶解と再鍛造: 武器を溶かすと `swordskill:forged_ingot` になり、**素材と武器記憶を保持**します。良い素材と組み合わせて鍛造し直す（再鍛造）と、育った武器記憶と隠しパラメータをそのまま引き継ぎます。
- 強化: `forgeEnhanceMax` まで強化でき、1 段階ごとに作成素材の `enhancementCostRatio`（既定 1 割）を消費します。
- 特殊素材: 素材 JSON に `"special": true` を付けたものは、ラストアタックボーナス系・特殊報酬系の武器に使う想定です（色を上書きし、`power` がダメージ倍率になります）。
- モデル/テクスチャ: 部品テクスチャを重ねた組み合わせモデル（120 種）を `models/item/forged/` に生成済みです。色は `tintindex` で乗算するので、部品テクスチャは明るいグレースケールで描くほど素材色が濃く出ます。

### 鍛冶台 GUI (`swordskill:smithing_forge`)

- ブロック: `swordskill:smithing_forge`。右クリックで GUI が開きます。ブロックエンティティは持たず、材料は GUI（`SmithingForgeMenu`）側で管理します。
- 入手: クラフトレシピ（鉄インゴット8 + 作業台1）または `/give @s swordskill:smithing_forge`。
- 部品: 部品はレシピ定義の中の見た目・性能データです（**素材の段階でスロットに入れるものではありません**）。旧・部品アイテム `swordskill:smithing_part` は廃止し、`/swordskill forge part` コマンド・テクスチャ・モデルも削除しました。
- モード切替: 画面上部のボタンで **作成 / 強化 / 溶解 / 再鍛造** を切り替えます（`SetSmithingModePayload`）。
- スロット: 作成 = `メイン素材` + `対応素材 ×4` + `完成` / 強化 = `対象` + `強化素材` + `完成` / 溶解 = `対象` + `完成` / 再鍛造 = `インゴット` + `メイン素材` + `完成`。使わないスロットは暗く表示され、物を置けません。結果はサーバー側で計算し、通常のスロット同期でクライアントへ配ります（品質が熟練度依存のため）。
- レシピ選択: 一覧クリックで `SelectSmithingRecipePayload` を送り、選択番号はサーバーが `ContainerData` で返します（常にサーバーが正）。
- 定義の同期: ログイン時とデータパック再読み込み時に `SyncSmithingDefsPayload` で素材・部品・レシピの定義をクライアントへ配るので、マルチプレイでも一覧と部品名が正しく出ます。

コマンド (要 OP 権限レベル2):
```
/swordskill forge create <targets> <recipe> [material]
/swordskill forge reforge <targets> <recipe> [material]
/swordskill forge enhance <targets> <amount>
/swordskill forge melt <targets>
/swordskill forge memory <targets>
/swordskill forge defs
```
`<material>` は省略すると `iron` です。`<recipe>` / `<material>` は `swordskill:` を省略した ID でも指定できます。
例: `/swordskill forge create @s blade_straight__guard_plain__handle_wrapped` → `/swordskill forge memory @s`

作成の手順（ゲーム内）:
1. `/give @s swordskill:smithing_forge` で入手して設置し、右クリックで GUI を開きます。
2. **作成**モードでメイン素材（例: 鉄インゴット）を入れると、レシピ一覧が出ます。
3. 一覧から武器を選ぶと、対応素材（例: 木材・革）がゴースト表示されます。
4. 対応素材を入れると完成スロットに武器が出るので取り出します。

## Proficiency (熟練度)
武器種などのカテゴリごとに貯まる熟練度です。将来的に料理・投擲・鍛冶などのカテゴリを追加する前提の作りになっています。
Per-player proficiency stored per category (weapon type for now; cooking / throwing / smithing are planned).

- 上昇: そのカテゴリのスキルでモンスターを撃破すると熟練度が加算されます（`proficiencyPerKill`、既定 1）。`proficiencyFromMonstersOnly` を false にすると動物などでも加算されます。料理・鍛冶などのカテゴリは、それぞれの行動に成功したときに `ProficiencyManager.addProficiency(...)` を呼ぶ形で追加できます。
- レベルアップ: 熟練度が `proficiencyPerLevelUp`（既定 100）貯まるごとに「レベルアップポイント」を1つ獲得します。選択画面の「レベルアップ (熟練度ポイント n)」ボタンで、**そのカテゴリのスキル**を1レベル上げられます（最大レベル・未解放・説明枠は対象外）。ポイントは「獲得済み − 使用済み」で管理し、レベルアップ以外では消費されません。
- 解放条件: スキル JSON の `unlock_proficiency` に数値を書くと、そのカテゴリの熟練度がその値以上でないと解放できなくなります（0 または未指定なら熟練度不要）。解放ボタンには必要な熟練度が表示され、足りない場合は警告が出ます（クリエイティブは免除）。
  - 既定値の目安: 各スキルの攻撃倍率から `score = 最大単発倍率 × 1.5 + 全ヒット合計倍率` を計算し、`score < 5 → 0` / `< 8 → 50` / `< 11 → 100` / `< 15.5 → 150` / それ以上 `→ 200` で設定しています。武器種ごとに必ず1つは必要熟練度0の入門スキルが残るようにしています（SPEAR・RAPIER・KATANA は自動補正）。
- 保存先: `world/swordskill_data/<uuid>.json` の `proficiencies`（カテゴリ→熟練度）と `proficiency_spent`（カテゴリ→使用済みポイント）。
- 表示: 選択画面のフッター右側に `片手剣 熟練度 142 (次まで 42/100) ▲1` の形式で表示されます。
- 熟練度一覧画面: ステータス画面の「熟練度一覧」ボタンから開けます（`key.swordskill.status_key`、既定 `K` → ステータス画面）。左にカテゴリごとの熟練度・ポイント、右にそのカテゴリのスキルごとのレベル・解放状況・解放に必要な熟練度を一覧表示します（ホイールでスクロール、行は領域内でクリップされます）。

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
| `assignable` | bool | true | false にするとスロットにセットできない（説明用エントリなど） |

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



