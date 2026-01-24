# リポジトリ概要

IntelliJ Platform 向けプラグインで、エディタのファイル末尾に `[EOF]` マーカーを表示する。
Kotlin で実装し、Gradle でビルドする小規模プロジェクト。

## 技術スタック

- 言語: Kotlin 2.0.21
- JDK: Java 21
- ビルド: Gradle 9.3.0
- プラグイン基盤: IntelliJ Platform Gradle Plugin 2.10.5
- 対象 IDE: IntelliJ IDEA 2024.2 以降 (platformVersion = 2024.2, sinceBuild = 242)
- テスト: JUnit 4 + IntelliJ Platform TestFramework (`BasePlatformTestCase`)
- カバレッジ: Kover 0.9.1

## ビルドとテスト

```bash
# ビルド
./gradlew build

# テスト実行
./gradlew test

# カバレッジレポート生成
./gradlew koverXmlReport

# テスト用 IDE 起動（動作確認用）
./gradlew runIde
```

## プロジェクト構成

```
src/main/kotlin/com/github/msfukui/intellijplugineofmark/
  EofMarkEditorListener.kt  # EditorFactoryListener + ProjectActivity
  EofMarkRenderer.kt        # EditorCustomElementRenderer（描画処理）

src/main/resources/META-INF/
  plugin.xml                 # プラグイン定義（postStartupActivity を登録）

src/test/kotlin/com/github/msfukui/intellijplugineofmark/
  EofMarkRendererTest.kt         # レンダラーのユニットテスト
  EofMarkEditorListenerTest.kt   # リスナーの統合テスト
```

## アーキテクチャ

- `EofMarkProjectActivity` が `postStartupActivity` としてプロジェクト起動時に実行される
- `EditorFactory.addEditorFactoryListener()` で `EofMarkEditorListener` をプロジェクトスコープで登録
- エディタ生成時に `InlayModel.addInlineElement()` で末尾に `[EOF]` の inlay を追加
- `relatesToPrecedingText = true` により、テキスト編集時にマーカー位置が自動追従する
- リスナーはプロジェクト単位でスコープされ、マルチプロジェクト環境での重複を防止

## コーディング規約

- コミットメッセージは日本語で記述する
- テストは `BasePlatformTestCase` を継承し、IntelliJ Platform のテストサンドボックス内で実行する
- `postStartupActivity` がテスト時にも自動実行されるため、統合テストでは手動でリスナーを呼び出さず、プラグインの自動動作を検証する
- プロダクションコードに影響しないよう、Kover はビルドスクリプトに含めるがリリースには関与しない
