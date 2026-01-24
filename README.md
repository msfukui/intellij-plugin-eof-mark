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

GitHub Actions の手動トリガーでセマンティックバージョニングに基づくリリースを行います。

### 手順

1. PR に `release:major`、`release:minor`、`release:patch` のいずれかのラベルを付与してマージ
2. リリースしたいタイミングで GitHub Actions の "Run workflow" または `gh workflow run release` を実行
3. 前回リリース以降のマージ済み PR のラベルから最も影響の大きいバンプレベルが自動決定される
4. `gradle.properties` 更新 → ビルド → タグ作成 → GitHub Release 作成（zip 添付）

### バージョンバンプの優先度

| ラベル | バンプ |
|--------|--------|
| `release:major` | X.Y.Z → (X+1).0.0 |
| `release:minor` | X.Y.Z → X.(Y+1).0 |
| `release:patch` | X.Y.Z → X.Y.(Z+1) |

複数 PR がある場合、最も影響の大きいラベルが採用されます。

## インストール

現在は Marketplace 未公開です。ローカルビルドからインストールできます。

1. `./gradlew buildPlugin` を実行
2. `build/distributions/` 配下の zip ファイルを取得
3. IntelliJ IDEA の Settings > Plugins > ⚙ > Install Plugin from Disk... から zip を選択

## ライセンス

[MIT](https://opensource.org/licenses/MIT)

## その他

このプロダクトは [Claude Code](https://claude.com/claude-code) を使用して作成されています。
