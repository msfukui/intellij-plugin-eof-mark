[![Build and Test](https://github.com/msfukui/intellij-plugin-eof-mark/actions/workflows/build.yml/badge.svg)](https://github.com/msfukui/intellij-plugin-eof-mark/actions/workflows/build.yml)

# EOF Mark

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

## インストール

現在は Marketplace 未公開です。ローカルビルドからインストールできます。

1. `./gradlew build` を実行
2. `build/distributions/` 配下の zip ファイルを取得
3. IntelliJ IDEA の Settings > Plugins > ⚙ > Install Plugin from Disk... から zip を選択

## ライセンス

MIT
