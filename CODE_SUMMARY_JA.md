# swordskill コード概要

このリポジトリは Minecraft Forge 1.20.1 向けの Mod です。Mod ID は `swordskill`、バージョンは `2.0.1-alpha` で、Java 17 と ForgeGradle を使ってビルドします。プレイヤーが武器種ごとの「ソードスキル」を選択・解放・実行できるようにし、攻撃判定、アニメーション、剣の軌跡、SP 消費/回復、HUD/選択画面を追加します。

## 技術スタック

- Minecraft: `1.20.1`
- Forge: `47.4.0`
- Java: `17`
- Mixin: Sponge Mixin
- 追加ライブラリ: `player-animation-lib-forge`
- ビルド: Gradle / ForgeGradle

主な設定は `gradle.properties` と `build.gradle` にあります。リソース生成物は `src/generated/resources` も main resources に含まれます。

## 全体構成

- `src/main/java/com/zoma1101/swordskill/SwordSkill.java`
  - Mod のエントリポイントです。
  - エンティティ、アイテム、タブ、属性、エフェクト、エンチャント、Loot Modifier、Config、ネットワークを登録します。
  - サーバー tick ごとにスキル実行処理と SP 回復処理を呼び出します。
  - リソースリロード時に `weapon_types` JSON を読み込みます。

- `swordskills/`
  - スキル定義とスキル実装の中心です。
  - `SwordSkillRegistry` に 90 件のスキルが登録されています。
  - `SkillData` がスキル ID、名前、クールダウン、SP コスト、武器種、実行クラス、軌跡色/テクスチャなどを保持します。
  - 個別スキルは `skill/sword`, `skill/axe`, `skill/katana` など武器種別パッケージに分かれています。

- `server/handler/`
  - サーバー側の実行管理です。
  - `SkillExecutionManager` がスキルの開始、tick ごとの継続実行、終了処理を管理します。
  - `SPManager` が SP 回復、攻撃時回復、SP 消費、クライアント同期を行います。

- `client/`
  - キー入力、HUD、スキル選択画面、スキルホイール、剣の軌跡描画、アニメーション再生を扱います。
  - `ClientForgeHandler` がキー入力からスキル使用パケット送信までの主要な入口です。
  - `ClientModHandler` がキー設定、GUI overlay、エンティティレンダラー、プレイヤーへの軌跡レイヤー追加を登録します。

- `network/`
  - Forge `SimpleChannel` によるクライアント/サーバー通信です。
  - スキル使用、スキル選択、スロット保存/読み込み、解放状況確認、SP 同期、アニメーション再生、軌跡設定同期などのパケットがあります。

- `capability/`
  - プレイヤーごとの永続データです。
  - 解放済みスキル、武器ごとの 5 スロット、選択中スロット、SP、戦闘タイマー、軌跡表示設定、体術解放フラグを NBT に保存します。

- `data/`
  - 武器種判定とデータパック読み込みを扱います。
  - `WeaponTypeDataLoader` が `data/*/weapon_types/*.json` を読み込みます。
  - `WeaponTypeDetector` が JSON、独自タグ、バニラタグを使って持っているアイテムの武器種を判定します。

- `entity/`, `effects/`, `loot/`, `item/`, `enchantment/`, `command/`
  - 攻撃判定エンティティ、状態効果、Loot Modifier、解放アイテム/サンプル武器、エンチャント、コマンドを定義します。

## スキル使用の流れ

1. クライアントでキー入力を受け取る
   - `ClientForgeHandler.clientTick` が使用キーや各スロットキーを監視します。

2. 現在の武器種とスロットからスキル ID を決める
   - `ClientSkillSlotHandler` と `WeaponTypeDetector` の情報を使います。
   - スキルが現在の武器種に対応しているかを確認します。

3. クライアントからサーバーへ `UseSkillPacket` を送る
   - スキル ID と `finalTick` を送信します。

4. サーバーで使用可否を検証する
   - `UseSkillPacket.handle` が、スキル存在、解放済みかどうか、Creative かどうか、SP が足りるかを確認します。
   - Creative 以外では SP を消費します。

5. アニメーション再生を同期する
   - `PlayAnimationPacket` を周囲のクライアントへ送ります。

6. スキルを実行する
   - 即時スキルはその場で `ISkill.execute` を呼びます。
   - 継続スキルは `SkillExecutionManager.startSkillExecution` に登録され、サーバー tick ごとに `execute` されます。

7. 攻撃判定やエフェクトを生成する
   - 個別スキルは `BaseSkill` のヘルパーや `SkillUtils` を通じて攻撃判定エンティティ、飛ぶ斬撃、軌跡、効果音などを生成します。

## スキルデータ

`SwordSkillRegistry` でスキルを静的登録しています。登録データには以下が含まれます。

- スキル ID
- 内部名
- クールダウン tick
- SP コスト
- スキル種別
  - `SIMPLE`
  - `TRANSFORM`
  - `TRANSFORM_FINISH`
  - `RUSH`
- 実行クラス
- 対応武器種
- 隠しスキルかどうか
- 実行終了 tick
- 連携入力の制限 tick
- 軌跡色、軌跡テクスチャ、軌跡長、オフセット

武器種は `ONE_HANDED_SWORD`, `TWO_HANDED_SWORD`, `KATANA`, `AXE`, `RAPIER`, `CLAW`, `SPEAR`, `MACE`, `SCYTHE`, `DAGGER`, `DUALSWORD`, `MARTIAL_ARTS`, `NONE` です。

## 武器種判定とデータパック

`src/main/resources/data/swordskill/weapon_types` には 46 個の武器種 JSON があり、外部 Mod 武器への対応を定義しています。サンプルは `weapon_types_sample` に 10 個あります。

判定優先度はおおむね以下です。

1. `weapon_types` JSON
2. `swordskill_extender` 名前空間の独自アイテムタグ
3. バニラの `ItemTags.SWORDS` / `ItemTags.AXES`

空手/体術系のため、`MARTIAL_ARTS` は常に候補に追加されます。

JSON 例:

```json
{
  "name": "axe",
  "item": ["example:steel_axe"],
  "weapontype": ["AXE"]
}
```

## プレイヤーデータ

`PlayerSkills` が以下を管理します。

- 解放済みスキル ID
- 武器名ごとのスキルスロット 5 個
- 現在選択中のスロット
- 現在 SP
- 戦闘タイマー
- 軌跡表示の有効/無効
- 体術解放フラグ
- データ移行済みフラグ

NBT には `UnlockedSkills`, `SkillSlots`, `SelectedSlot`, `CurrentSP`, `CombatTimer`, `TrailEnabled` などとして保存されます。

## SP とクールダウン

- SP 最大値と回復量は `SwordSkillAttribute` と `ServerConfig` で管理されます。
- サーバー tick ごとに `SPManager.handleSPRegen` が呼ばれます。
- 非戦闘時は SP 回復量が 3 倍になります。
- 攻撃時は最大 SP の 2% を回復し、戦闘タイマーを 100 tick に設定します。
- クールダウンはクライアント側 `ClientForgeHandler` の `cooldowns` で管理され、Config と属性値の影響を受けます。

## Config

`ServerConfig`:

- `damageMultiplier`
- `CoolDownMultiplier`
- `AutoWeaponSetting`
- `pvp`
- `UnlockedSkill`
- `attackNeutralMobs`
- `defaultMaxSP`
- `defaultSPRegen`

`ClientConfig`:

- `SwordSkillEffect_System`
- `hudPosX`
- `hudPosY`
- `hudScale`

## リソース

- プレイヤーアニメーション JSON: 90 個
- GUI スキルアイコン PNG: 83 個
- 武器種 JSON: 46 個
- 武器種サンプル JSON: 10 個
- 言語ファイル: `en_us.json`, `ja_jp.json`
- 攻撃/軌跡用テクスチャ、アイテムモデル、Loot Modifier、Advancement があります。

## 注意点

- 既存の日本語コメントや README の一部が文字化けしています。ソース自体は UTF-8 コンパイル設定ですが、過去に Shift_JIS/UTF-8 の変換崩れが起きた可能性があります。
- `WeaponTypeDetector.isReady()` は名前と戻り値の意味が少し直感と逆に見えます。現在は「未初期化または未ロードなら true」で、`detectWeaponTypes` はその場合に空集合を返します。
- スキル登録は `SwordSkillRegistry` に集中しており、追加時はアニメーション JSON、GUI アイコン、言語キー、実行クラス、必要なら武器種 JSON も合わせて追加する必要があります。
- 継続スキル実行中は開始時のホットバースロットに戻す処理があります。スキル中の持ち替えを制限する意図です。
- `SkillExecutionManager` はスキルインスタンスを ID ごとにキャッシュします。個別スキルクラスに mutable な状態を持たせる場合はプレイヤー間で共有される可能性に注意が必要です。

## 改造するときの入口

- 新しいスキルを追加する:
  - 個別スキルクラスを `swordskills/skill/<weapon>/` に作る
  - `SwordSkillRegistry` に `SkillData` を追加する
  - `assets/swordskill/player_animation` にアニメーション JSON を追加する
  - `assets/swordskill/textures/gui` にアイコンを追加する
  - 言語ファイルへ表示名を追加する

- 外部 Mod 武器を対応させる:
  - `data/swordskill/weapon_types/*.json` を追加/編集する
  - または `swordskill_extender` のアイテムタグを使う

- HUD や操作を変える:
  - `client/gui/`
  - `client/handler/ClientForgeHandler.java`
  - `client/screen/Keybindings.java`

- SP やバランスを変える:
  - `config/ServerConfig.java`
  - `effects/SwordSkillAttribute.java`
  - `server/handler/SPManager.java`
  - `SwordSkillRegistry` の各スキル定義
