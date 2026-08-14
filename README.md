[![Build and Test](https://github.com/msfukui/intellij-plugin-eof-mark/actions/workflows/build.yml/badge.svg)](https://github.com/msfukui/intellij-plugin-eof-mark/actions/workflows/build.yml)

# IntelliJ Plugin EOF Mark

IntelliJ Platform 向けプラグインです。エディタで開いたファイルの末尾に `[EOF]` マーカーを表示します。

## 機能

- ファイルの最終位置に `[EOF]` をインラインで表示
- 表示色はエディタの行番号色に合わせた控えめなスタイル

## 対応環境

- IntelliJ IDEA 2024.2 以降
- IntelliJ Platform ベースの IDE（PhpStorm, WebStorm, PyCharm 等）

## ビルド

### 前提条件

- JDK 21

### コマンド

```bash
# ビルド
./gradlew build

# テスト実行
./gradlew test

# カバレッジレポート生成
./gradlew koverXmlReport

# テスト用 IDE で動作確認
./gradlew runIde
```

## リリース

draft release を publish することでリリースします。バージョン番号は draft release のタグ名が正となります。

### 手順

1. PR に `release:major`、`release:minor`、`release:patch` のいずれかのラベルを付与してマージ
2. マージのたびに Draft Release ワークフローが動き、次バージョンの **draft release が作り直される**。タグはこの時点では作られない
3. リリースしたいタイミングで [Releases](https://github.com/msfukui/intellij-plugin-eof-mark/releases) から draft を **publish** する
4. Release ワークフローが動き、`gradle.properties` 更新 → ビルド → 互換性検証 → zip 添付 → JetBrains Marketplace へ公開

リリースするバージョンを変えたい場合は、publish する前に draft のタグ名とタイトルを編集してください。

### バージョンバンプの優先度

| ラベル | バンプ |
|--------|--------|
| `release:major` | X.Y.Z → (X+1).0.0 |
| `release:minor` | X.Y.Z → X.(Y+1).0 |
| `release:patch` | X.Y.Z → X.Y.(Z+1) |

複数 PR がある場合、最も影響の大きいラベルが採用されます。draft は毎回作り直されるため、前回リリース以降にマージされた全 PR とラベルが常に反映されます。

### 公開だけをやり直す

Marketplace への公開のみが失敗した場合（トークン失効、Marketplace 側の障害など）、リリース全体をやり直す必要はありません。既存のタグを指定して Release ワークフローを再実行してください。

```bash
gh workflow run release.yml -f tag=v0.1.5
```

`gradle.properties` のコミットと zip の添付は冪等なので、公開のみが再試行されます。

## インストール

現在は Marketplace 未公開です。ローカルビルドからインストールできます。

1. `./gradlew buildPlugin` を実行
2. `build/distributions/` 配下の zip ファイルを取得
3. IntelliJ IDEA の Settings > Plugins > ⚙ > Install Plugin from Disk... から zip を選択

## ライセンス

[MIT](https://opensource.org/licenses/MIT)

## その他

このプロダクトは [Claude Code](https://claude.com/claude-code) を使用して作成されています。
