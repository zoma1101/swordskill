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



