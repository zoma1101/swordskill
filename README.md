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
- 0 にしたスキルは「熟練度不要」＝ログイン時に自動解放されます（アイテム不要）。
- 同じカテゴリのファイル内に、そのカテゴリに存在しないスキル名を書いた場合はログに警告が出ます。

コマンド (要 OP 権限レベル2):
```
/swordskill proficiency get <targets>
/swordskill proficiency set <targets> <category> <amount>
/swordskill proficiency add <targets> <category> <amount>
```
`<category>` は武器種名（例: `ONE_HANDED_SWORD`, `DUALSWORD`, `AXE` …）。
例: `/swordskill proficiency add @s DUALSWORD 100`



## Selection Screen UI (選択画面)
SAO 風の UI になっています。SAO-styled skill selection screen.
- 武器種タブ: 上部の武器種アイコンをクリックすると、その武器種のスキルだけを表示します。もう一度同じタブをクリックすると装備中の武器種に戻ります。武器種が増えて入りきらない場合は `<` `>` の矢印クリック、またはタブ列の上でホイールすることでスクロールできます。
- 攻撃倍率: カード右側のパネルに、段（ヒット）ごとの倍率を `1段目 100%` の形式で縦に並べます。レベル倍率がかかっている場合は `1段目 100%→300%` のように適用後も併記します。9段以上など縦に収まらない場合は、パネルの上でホイールするとスクロールでき、右端にスクロールバーが出ます。
- 装備武器種の制限: タブで別の武器種のスキルを表示していても、**今装備している武器種では使えないスキルはスロットにセットできません**（スロットが灰色になり、クリックすると警告が出ます）。サーバー側でも同じ検証を行うため、改造クライアントからも設定できません。
- 派生スキル: TRANSFORM 系スキルでは、続くスキルが「派生:」のノード＋接続線のリンク図で表示されます（未習得は暗く表示、ホバーで名前を表示）。派生先はスロットに直接セットできません。派生元スキルの使用後、`transform_limit_ticks` 以内に同じキーを再入力すると派生し、派生時にはチャイム音が鳴ります。
- チャージ系の派生: horizontal / vertical 系列は、受付時間内なら短押しでも元スキルを再生し直さず、必ず次の段へ進みます。受付時間外やクールダウン中で発動できないときは、構え・発光・チャージ音を含むチャージ演出自体を開始しません。チャージを開始したあとに受付時間が過ぎても、開始時に有効だった派生は離したときに実行されます。受付中の派生は同じ軸のチェーンを進むため、離すときのしゃがみによる軸切り替えは行いません。
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



