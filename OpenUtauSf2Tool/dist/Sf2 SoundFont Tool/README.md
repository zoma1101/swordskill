# OpenUtau SF2 Tool

OpenUtau で **SoundFont 2 (.sf2)** を読み込み、プリセット・インストゥルメント・ゾーン・サンプルを確認し、
試聴・WAV 書き出し・OpenUtau 用ボイスバンクへの書き出しができるプラグインです。

```
OpenUtauSf2Tool/
├─ src/                 C# ソース (.NET Framework 4.x / WinForms)
├─ plugin/plugin.txt    OpenUtau レガシープラグイン定義
├─ tools/               テスト用 SF2 生成ツール
├─ build.ps1            ビルド (+ インストール)
└─ README.md
```

## インストール

### ビルドして自動インストール

```powershell
# このフォルダで
powershell -ExecutionPolicy Bypass -File .\build.ps1 -Install
```

`%USERPROFILE%\Documents\OpenUtau\Plugins\Sf2 SoundFont Tool\` に配置されます。
OpenUtau を再起動してください。

### 手動

`dist\Sf2 SoundFont Tool\` フォルダを
`ドキュメント\OpenUtau\Plugins\` にコピーして OpenUtau を再起動します。

## 使い方

1. OpenUtau でピアノロールを開き、ノートを 1 つ以上置きます。
2. ピアノロールを右クリック → **Plugins** → **SF2 SoundFont Tool (S)**
   （プラグインはノートが 1 つも無いと起動できません）
3. ツールが起動したら **「SF2 を開く...」** でサウンドフォントを選択します。
4. 左のツリーでプリセット／ゾーン／サンプルを選び、音程とベロシティを決めて **▶ 試聴**。
5. 必要に応じて書き出します。

OpenUtau から起動した場合、ピアノロールで選択中のノートの歌詞が
自動的に読み込まれ、ボイスバンクのエイリアスに追加されます。

### 画面の機能

| 機能 | 説明 |
| --- | --- |
| プリセットツリー | Bank/Program 順。検索ボックスで絞り込み |
| ゾーン表示 | キー範囲・ベロシティ範囲・ルートキー・ループ・ジェネレータ |
| 試聴 | 指定音程へピッチシフトして再生（プリセットはレイヤーをミックス） |
| WAV 書き出し | 選択サンプル、または試聴結果を WAV に保存 |
| ボイスバンク書き出し | OpenUtau の歌手フォルダ（character.txt + oto.ini + sample.wav）を生成 |
| 全サンプル書き出し | SF2 内の全サンプルを個別 WAV に保存 |

## 生成されるボイスバンクについて

- 選択中のプリセットから、指定音程を含むゾーンのサンプルを 1 つ選びます
  （同じ音域に複数ある場合は最も長いもの）。
- ループ設定された短い波形（ウェーブテーブル）は、そのまま書き出すと数ミリ秒に
  なってしまうため、ループを展開して 2〜8 秒の長さに整えてから書き出します。
- `character.txt`、`oto.ini`、`sample.wav`、`README.txt` を出力します。
- `oto.ini` のエイリアスは、主要な日本語 CV 音節（ローマ字・かな）と、
  起動時に読み込んだプロジェクトの歌詞です。
- 歌詞を変えたら、もう一度書き出すのが確実です。

### 注意

- 1 サンプルを音程に合わせて伸縮する簡易ボイスバンクです。旋律楽器（フルート、弦、管楽器など）向きです。
- 和音・ノイズ・複数音が混ざったサンプルは、ピッチ検出に失敗することがあります。
  その場合は OpenUtau のレンダラーを **Classic** などに変えて試してください。
- ステレオサンプルは左チャンネルのみ使用します。
- SF2 の 24bit サンプルは規格外です（本ツールは 16bit PCM として読みます）。

## ビルド要件

- Windows
- .NET Framework 4.x（`C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe`）
- dotnet SDK は不要です

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

## 動作確認用 SF2 の生成

```powershell
& "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:exe /out:tools\MakeTestSf2.exe tools\MakeTestSf2.cs
.\tools\MakeTestSf2.exe test.sf2
.\dist\Sf2` SoundFont` Tool\OpenUtauSf2Tool.exe --selftest test.sf2 selftest-out
```

`selftest-out\selftest.txt` に解析結果と書き出し結果が出力されます。

## ライセンス

MIT License
