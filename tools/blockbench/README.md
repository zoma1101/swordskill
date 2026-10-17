# SwordSkill Blockbench スキルデータ作成プラグイン

このプラグインはSwordSkillの`skilldata` JSONをBlockbenchへ読み込み、タイムライン上で調整して再出力できます。新規作成にも対応し、攻撃判定や移動イベントを配置できます。突進スキルでは発動前・突進中・突進後のアニメーションsuffixと切替tickも設定できます。スキル情報とイベントは **Effects > Timeline** のカスタム指示キーフレームに保存されるため、アニメーションと同じプロジェクト内で管理できます。ゲーム用アニメーションの書き出しと、スキルデータJSONの書き出しは別々に行ってください。

## インストールと基本操作

1. `swordskill_skill_creator.js`を保存します。
2. Blockbenchで **File > Plugins > Load Plugin from File** を選び、保存したプラグインファイルを読み込みます。
3. 既存skilldataを編集する場合は、**Tools > SwordSkill: Import skilldata JSON** で対象JSONを読み込みます。skilldataの`animation`名と一致するアニメーションがプロジェクト内にあれば自動で選ばれます。一致するアニメーションがなければ、選択中のアニメーションに読み込みます。新規作成する場合は **Tools > SwordSkill: Set Skill Metadata** を実行し、名前や武器タイプなどを設定します。
4. 攻撃を置きたいフレームに再生ヘッドを移動し、**Tools > SwordSkill: Add Attack at Playhead** を実行します。攻撃判定の有効/無効、判定範囲、ダメージ倍率（通常攻撃力または移動量依存の突進ダメージ）、ノックバック、効果、任意の攻撃音を設定します。RUSHでは **Repeat each tick until a RUSH hit lands** を有効にすると、指定tickから命中するまで毎tick判定します。攻撃音はプリセット一覧から選ぶか、カスタム欄に任意の登録済みサウンドIDを入力できます。カスタム欄に入力した場合はプリセットより優先されます。判定を無効にしても、そのイベントの攻撃音は再生されます。音だけを鳴らす場合は **Add Sound at Playhead** を使用し、サウンドID・音量・ピッチを指定してください。イベントはBlockbench標準の **Effects > Timeline** キーフレームとして表示されます。キーフレームを選択・ドラッグ・削除すると、プレビューとルーラー上端のSwordSkillマーカーも同期します。時刻の編集にはキーフレーム本体をドラッグしてください。ルーラーマーカーはイベント時刻から自動生成される表示用で、直接ドラッグするデータではありません。skilldataへの書き出し時はキーフレーム時刻を20tick/秒の整数tickに変換します。
5. 再生ヘッドを攻撃または飛ぶ斬撃イベントのtickに合わせるか、Effects > Timelineでイベントキーフレームを選択すると、アニメーション画面に半透明の範囲ボックスが表示されます。飛ぶ斬撃のボックスにはskilldataのサイズと回転も反映されます。ボックスはアニメーション中のbody/rootボーンの移動には追従せず、ルートグループの固定ピボットを基準に表示します。赤・緑・青のハンドルをドラッグして対応する軸のサイズを直接変更できます。同時刻に複数の範囲がある場合は、Effects > Timeline上で編集したいイベントを選択してからサイズを調整してください。オフセット、ダメージ、ノックバックなどはイベントキーフレームを選択して **Edit Selected Event** から編集できます。
6. 使用者自身に移動を加える場合は、そのフレームで **Tools > SwordSkill: Add Motion at Playhead** を実行します。異なるフレームに複数の移動イベントを置けます。攻撃に結び付かない演出音は **Tools > SwordSkill: Add Sound at Playhead** から追加できます。
7. 飛ぶ斬撃を追加する場合は、そのフレームで **Tools > SwordSkill: Add Flying Slash at Playhead** を実行します。出現位置、向き、範囲、ダメージ倍率、ダメージ基準（通常攻撃力または移動量を加味した突進ダメージ）、寿命、飛距離、テクスチャ、任意の攻撃音を指定できます。描画は横方向−75°から＋75°まで斬撃トレイルを振り、イベントで指定した角度を加えて表示します。サウンドはsweepなどのプリセットから選ぶか、登録済みIDを入力できます。1イベントで斬撃エフェクトを1つ生成し、同じ対象への重複ヒットを防ぎます。
Flying Slash preview controls: drag the red, green, and blue rings to rotate around X, Y, and Z. The slash updates live while dragging, and the resulting RXYZ values are shown when you release. Drag the colored box handles to resize it.
8. Effects > Timeline上のSwordSkillイベントを選び、**Tools > SwordSkill: Edit Selected Event** を使うと設定を編集できます。イベントキーフレームの削除にはBlockbench標準のタイムライン操作を使います。各種マーカーは対応するイベントに合わせて同期されます。
9. **Tools > SwordSkill: Export skilldata JSON** を実行し、生成したファイルを`src/main/resources/data/swordskill/skilldata/`に保存します。
10. 既存の手順で対応するPlayerAnimatorアニメーションを`src/main/resources/assets/swordskill/player_animation/`に書き出します。メタデータのアニメーション名は、ゲーム内のアニメーションリソース名と一致させてください。
11. スキルデータを追加・変更した後、ワールド内で`/reload`を実行します。
12. ゲーム内では、skilldata式スキルに割り当てた発動キーを長押しし、離すと発動します。チャージ中はアニメーションの0秒時点のポーズを維持し、武器にエンチャントと同じ光沢を表示します。チャージ終了時には元の光沢設定へ戻ります。ビーコンの環境音もチャージ音として鳴ります。チャージ倍率は長押し開始時の`0.75`から上昇し、最大`40`tick（2秒）で`2.0`になります。倍率は攻撃判定と飛ぶ斬撃のダメージに適用されます。horizontal／vertical系列の変形スキルは、チャージ`0–13`tickで通常段、`14–26`tickで`_arc`段、`27–40`tickで`_square`段を選びます。離すときにしゃがんでいると、horizontal系列とvertical系列を切り替えます。チャージ倍率も通常どおり適用されます。この系列以外のskilldata式スキルでは、チャージ時間は従来どおり威力倍率にのみ使われます。組み込みスキルは従来どおりクリックで発動します。

JSONを読み込むと、現在選択中のアニメーションにメタデータとイベントを取り込みます。既にSwordSkillマーカーが存在する場合は確認後に置き換えます。SwordSkill以外のタイムライン指示とアニメーションキーは保持されます。インポートした攻撃・移動・飛ぶ斬撃イベントはタイムライン上で編集でき、再出力したJSONに反映されます。継続時間は **Set Skill Metadata** の **Skill duration (ticks)** から編集できます。`legacy_fallback: true`のJSONでは元のJava処理がイベントより優先されます。イベントを追加・編集、または範囲ボックスを直接リサイズすると、プラグインは自動的にフォールバックを無効にし、データ駆動モードへ切り替えます。旧Java処理に固有の動きも必要な場合は、必要な攻撃・移動イベントをタイムライン上で追加してください。

## 既存Javaスキルの移行

`tools/skilldata/convert_java_skills.py`は、`SwordSkillRegistry`内の既存Javaスキル登録を読み取り、skilldata JSONの移行用ひな形と`conversion-report.json`を生成します。旧`spawnAttackEffect`は移動量が`Vec3.ZERO`なら通常のhitbox判定に変換し、移動量がある呼び出しだけ`projectiles`イベントへ変換します。`SimpleSkillSound`、`StrongSkillSound`、`GodSkillSound`もtickと既定の音量・ピッチから抽出します。同じtickに攻撃イベントがある音は、その`hitboxes`または`projectiles`イベントの`sound`へまとめ、独立した`sounds`イベントを作りません。攻撃イベントのないtickの音だけを`sounds`へ出力します。接触して攻撃するRUSHは開始時の移動をmotionイベントに、接触判定を`repeat_while_rushing`付きhitboxに変換します。hitboxは毎tick再試行し、命中するとRUSHを終了します。RUSH hitboxは`damage_type: "RUSH"`で移動量依存ダメージを選べます。rage_spikeとsonic_reapはこの変換に対応します。変換後も元Javaの無敵時間、状態効果、接地時終了、命中時サウンドなど一部挙動は移植されません。RUSHの繰り返しhitboxは当たり範囲編集用で、元の斬撃ビジュアルとは別です。conversion-reportの警告と範囲を確認してください。対象を絞る場合は次のように実行できます。

```powershell
python tools/skilldata/convert_java_skills.py --skills rage_spike sonic_reap --output build/skilldata_rush --force
```

通常変換で条件分岐などを安全に移植できないスキルは警告し、`legacy_fallback: true`で元のJavaスキル処理を維持します。生成後はconversion-reportの警告と攻撃範囲を確認してください。Blockbenchで「Keep original Java skill behavior」を無効にすると、元実装ではなくskilldataイベントが実行されます。既存スキルと同じIDを指定するため、JSONをデータパックへ導入すると該当IDのJavaスキルがskilldata版へ置き換わり、スロット設定や解放状態を維持できます。Python 3.10以降を用意し、リポジトリのルートで次を実行してください。

```powershell
python tools/skilldata/convert_java_skills.py
```

既定の出力先は`build/skilldata_migration/`です。JSONには既存ID、名前、タイプ、武器、クールダウン、SP、非表示設定などを移します。突進スキルはPlayerAnimator JSONに`<skill>.start`があればその長さを、なければJSON内の最長アニメーションの長さを`rush_start_ticks`として推定します。`.start`がない場合はレポートの警告を確認し、値とフェーズ用アニメーションsuffixを手動調整してください。対応パターン外の攻撃やJava固有処理は自動移植されず、未変換の処理があるJSONは`legacy_fallback: true`でJava側の処理を維持します。Blockbenchで編集したイベントをゲームに反映するときはフォールバックを無効にしてください。既存の出力ファイルは標準では上書きされません。意図的に上書きする場合のみ`--force`を指定してください。入力先・出力先は`--registry`、`--skills-dir`、`--animations-dir`、`--output`で変更できます。

## IDと武器タイプ

通常のカスタムスキルIDは、組み込みスキルの後の`85`から始まります。IDを入力した場合は、その値をJSONに使用します。`0`または空欄の場合、JSONのIDは`0`となり、MODがデータ読み込み時に現在のスキル一覧を基に空いている連番へ自動割り当てします。既存JavaスキルのIDを使った置き換えは、上記変換ツールが出力する`replace_builtin: true`付きJSONに限り利用してください。このJSONを読み込むと、同じIDの登録だけがskilldata版に置き換わります。複数プロジェクトのJSONを同時に読み込む場合も、MOD側で自動IDを割り当てます。固定IDの重複やカスタムIDの連番の欠落は読み込みエラーになります。

武器タイプ名はゲームが対応する名前に限ります。例：`ONE_HANDED_SWORD`、`KATANA`。任意の新しい武器タイプ名を追加することはできません。

## 突進スキル

メタデータの **Skill type** を`RUSH`にすると、JSONに`rush_start_ticks`と3種類のアニメーションsuffixが出力されます。既定suffixは`start`、`move`、`finish`で、たとえば基底名が`sonic_reap`なら`sonic_reap.start`、`sonic_reap.move`、`sonic_reap.finish`のPlayerAnimatorアニメーションを用意します。開始アニメーションを再生した後、`rush_start_ticks`経過時に突進中アニメーションへ切り替わります。突進後アニメーションは、突進スキルの攻撃判定が対象に命中したときに再生されます。

突進の速度・方向はタイムラインの移動イベントで設定します。開始tickを`0`にすると発動時に移動が適用されます。攻撃判定イベントは対象に命中するtickへ配置してください。命中しなかった場合は`duration_ticks`終了時にスキルアニメーションが解除されます。`rush_start_ticks`は`duration_ticks`以下にしてください。

Javaスキル移行ツールは、既定で`src/main/resources/assets/swordskill/player_animation/`内の`<skill>.json`から開始アニメーション長を読み取り、`rush_start_ticks`を推定します。別の場所にアニメーションJSONがある場合は`--animations-dir`でディレクトリを指定してください。

## タイムラインマーカーとMolang

SwordSkillのメタデータとイベントは、Molangとして有効な「値が0になる数値式」に符号化してタイムラインへ保存されます。以前のプラグインが保存した`// swordskill_meta:`や`// swordskill_event:`形式は、プラグイン読み込み時に新形式へ変換されます。SwordSkill以外のタイムライン指示には影響しません。

## 判定範囲と移動

攻撃イベントは`round(アニメーション時刻[秒] * 20)`のゲームtickに一度処理します。「Enable damage hitbox」を無効にするとダメージ判定は行わず、攻撃音だけを鳴らすイベントとして使えます。有効な判定は各イベントtickで個別にヒットし、連撃時には対象の通常の無敵時間をリセットして次の判定ダメージを適用します。判定箱のオフセットとサイズの単位はブロックで、座標は攻撃者の視点基準の「右・上・前」です。画面のプレビューでは1ブロックを16 Blockbenchピクセルとして表示し、モデルの最上位ボーンを基準にします。

移動イベントも対応するタイムライン時刻に1回だけ実行されます。`forward`はプレイヤーの視線方向、`strafe`はその右方向、`vertical`はワールド上方向への速度加算量です。正の`forward`は前進で、後退ではありません。対象へのノックバックとは別の設定です。

飛ぶ斬撃イベントは`projectiles`配列へ出力され、指定tickに移動・ダメージ判定を行う発光トレイルを生成します。`offset`と`rotation`はそれぞれ右・上・前の出現位置と、pitch・yaw・rollの追加回転です。`horizontal: true`を指定するとプレイヤーの視線pitchを相殺し、飛翔方向とトレイルを水平に保ちます（既定値は`false`）。`size`はエフェクトの判定範囲、`travel_distance`は前方への飛距離、`duration_ticks`は寿命です。任意の`movement`は`[right, up, forward]`の移動量で、指定すると`travel_distance`の代わりに適用されます。`particle`には互換用のエフェクトキーを指定します。`sound`は省略可能な登録済みサウンドIDです。`player_pitch_x_multiplier`/`player_pitch_z_multiplier`は視線pitchを回転へ反映する係数（既定値`0`）、`render_pitch`は描画の基準pitch（度、既定値`0`）です。

スキル色は`#54D8FF`のようなRGB値です。ゲーム内の武器トレイルにもこの色が使われます。ダメージはプレイヤーの現在の攻撃力に`damage_multiplier`を掛けて計算します。トレイルのBlockbenchプレビュー機能は一時的に無効です。

## トレイル・衝撃波・ヒットストップ

- `trail_length`：武器トレイルの長さ倍率（既定`1.5`）。突き系スキルでは`4.0`〜`5.0`程度にすると突きが強調されます。
- `shockwave_ticks`：攻撃判定（hitbox／飛ぶ斬撃）が発生した瞬間に、突きのビームを装飾する衝撃波（リング状のパルス）を表示するtick数（既定`0`で無効）。`10`〜`12`程度が目安です。色はスキルの`color`（トレイル色）と同じものが使われます。
- `hitstop_ticks`：命中した瞬間に攻撃者と対象を停止させるヒットストップのtick数。既定は`-1`でサーバー設定`hitstopTicks`（既定`3`）を使用します。`0`で無効、`1`〜`40`で個別指定です。停止中は対象のノックバックが保留され、停止が明けたtickに対象へ付与されます。攻撃者のキー入力は止めないため、ダッシュ等の操作は妨げられません。打撃感はカメラシェイク（サーバー設定`hitstopShakeStrength`）と、命中させた本人への一瞬のFOVパンチ（サーバー設定`hitstopFovPunch`）で演出します。

## 効果

標準で利用できる効果：

- `undead_bonus`：Minecraftのアンデッドエンティティタグに属する対象へのダメージを倍率（`multiplier`）で増加します。
- `low_hp_bonus`：対象の体力が最大値の半分以下（`getMaxHealth()/2 >= getHealth()`）のとき、ダメージを倍率（`multiplier`）で増加します。
- `magic_bonus`：命中ダメージに`multiplier`を掛けた追加の魔法ダメージを与えます。
- `stun`：`duration_ticks`の間、移動速度低下を`amplifier`段階（0始まり）で付与します。
- `lifesteal`：命中ダメージを基準に`multiplier`分だけ使用者を回復します。

攻撃イベントの **Effect on hit** では1つ目の効果を編集できます。1つのhitboxに複数の効果（例：`undead_bonus`と`lifesteal`の併用）を持たせたい場合は、**Additional effect entries (JSON array)** 欄に2つ目以降をJSON配列で記述してください。例：`[{"type":"lifesteal","multiplier":0.25}]`。

追加の効果をJSONに記述することもできますが、新しい動作には`SkillHitEffectRegistry`へのJavaハンドラー登録が必要です。登録後に`/reload`してください。例：

```java
SkillHitEffectRegistry.register("my_mod:freeze", (context, target) -> {
    target.addEffect(new MobEffectInstance(MyEffects.FREEZE, 60, 0));
});
```

未登録の効果を参照したスキル定義は読み込み時に拒否され、サーバーログにファイル名と理由が記録されます。

## ゲーム側の制約

- カスタムスキルIDは`85`以上で、組み込みスキルに続く連番である必要があります。
- カスタムスキル定義はログイン時とデータパック再読み込み後に接続中のクライアントへ同期されます。
- 不正なJSON、未対応のenum値、範囲外の判定箱、未登録の効果を含むスキルはサーバーログにエラーを記録して読み込みをスキップします。
- JSONの`type`は`SIMPLE`、`TRANSFORM`、`TRANSFORM_FINISH`、`RUSH`に対応します。コンボの挙動はMODの既存ルールに従います。
- 1つの攻撃または移動イベントにつき、ゲーム内処理は1回です。複数回攻撃させる場合は攻撃イベントを複数配置してください。



### Contact-triggered RUSH combos

RUSH definitions can include rush_combo settings and mark attacks as trigger CONTACT; each event tick is relative to detected contact. Importing a combo JSON preserves these settings. The metadata dialog edits contact radius, rush and recoil motion, animation timing, timeout, and charge scaling. Attack and Flying Slash event dialogs offer From skill start and After contact timing. Timeline markers label contact events as Contact +Nt, and export writes the relative tick back to JSON.
