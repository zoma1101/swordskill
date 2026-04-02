# 一人称アニメーション同期の試行錯誤記録

このファイルは、一人称視点（First Person）において、三人称と同じようにプレイヤーの武器をアニメーションさせるために行った試行錯誤のアプローチと、それぞれの問題点を記録したものです。今後の開発の参考にしてください。

## アプローチ1: 絶対座標による再構築（AnimationUtils 内での固定位置+反転）
バニラの `ItemInHandRenderer` または `RenderHandEvent` における PoseStack に対して、以下のような絶対的な座標空間への変換を行ってから三人称用の行列（`getMatrix4f`）をかける方法。
```java
// 1. バニラの手ブレ等を消す
poseStack.last().pose().identity();
// 2. カメラ前方を+Zに反転させる（バニラのモデルと合わせるため）
poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
// 3. カメラから右下奥（体の中央）へ仮想の原点を置く
poseStack.translate(0.35f, -0.7f, 1.0f);
// 4. 三人称の getMatrix4f をかける
```
**結果・問題点:**
- 完全に三人称の軌道を描画できるが、バニラが後から掛ける `FIRST_PERSON_RIGHT_HAND` のアイテム表示角（X=-90度等）と干渉し、剣の上下や前後が逆転してしまう問題が発生。
- アニメーションでPos（移動）が設定されていないスキルが、カメラ上の強制的な固定位置（右下）に飛んでしまう。

## アプローチ2: RenderHandEventでの独自のTHIRD_PERSON描画
`ClientForgeHandler.java` の `onRenderHand` イベント内でバニラの描画をキャンセルし、自前で `renderStatic` を呼ぶ方法。
```java
// バニラの描画をキャンセル
event.setCanceled(true);
poseStack.pushPose();
// 上記アプローチ1の AnimationUtils.applyFirstPersonAnimation() を適用
// ...
mc.getItemRenderer().renderStatic(
        player.getMainHandItem(), 
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, // バニラの一人称変形を回避
        packedLight, OverlayTexture.NO_OVERLAY, poseStack, event.getMultiBufferSource(), mc.level, 0
);
```
**結果・問題点:**
- `FIRST_PERSON_RIGHT_HAND` による角度干渉は完全に解決し、三人称と全く同じダイナミックな剣の軌道が一人称カメラに描写される。
- しかし、バニラの「腕（Arm）」が見えなくなり、空中を剣だけが舞う状態になる。また、やはりPos指定のないスキルでのカメラ奥への配置に違和感。

## アプローチ3: バニラの初期位置に対する純粋な相対オフセット加算
`ItemInHandRenderer.renderItem` の直前で、バニラの斜め構え（初期位置）は維持しつつ、そこにアニメーションの「回転と移動」のみを相対的に足し合わせる方法。
```java
// 腕やアイテムの回転・移動だけを現在の PoseStack に足す
poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(armRotDeg.x));
// ...
poseStack.translate(itemPosPx.x / 16f, itemPosPx.y / 16f, itemPosPx.z / 16f);
poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(itemRotDeg.x));
```
**結果・問題点:**
- 「Posが設定されていないもの等は初期位置を利用する」仕様になり、自然な位置から振られるようになる。
- ただし、バニラの複雑な手首の回転や腕の描画と、PlayerAnimatorの回転軸（ローカル・グローバル基準が違う）とがうまく噛み合わず、直感と異なる方向に剣が振られるなどの「正常ではないアニメーション差異」が残る可能性がある。

---
現在は、これらの処理をすべて外し、一人称視点においては「バニラの通常スイングアニメーション」に戻すことで安定性を確保しています。
