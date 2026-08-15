package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EofMarkEditorListenerTest : BasePlatformTestCase() {

    private fun eofInlaysOf(editor: Editor): List<Inlay<*>> =
        editor.inlayModel.getInlineElementsInRange(0, editor.document.textLength)
            .filter { it.renderer is EofMarkRenderer }

    private fun getEofInlays(): List<Inlay<*>> = eofInlaysOf(myFixture.editor)

    /**
     * ファイルを開き、[EOF] マーカーが付いた状態にする。
     *
     * ヘッドレステストでは FileEditorManager が TestEditorManagerImpl に差し替わり、
     * createEditor(document, project) でエディタを生成するため myFixture.editor は
     * EditorKind.UNTYPED になる（本番のファイルタブは MAIN_EDITOR）。
     * そのため本番の postStartupActivity が登録したリスナーからは対象外と判定される。
     * インレイやカーソル制御の挙動そのものを検証するために、ここで明示的に付与する。
     *
     * 将来 TestEditorManagerImpl が MAIN_EDITOR を使うようになった場合は既にマーカーが
     * 付いているため、二重付与にならないよう条件付きにしてある。
     */
    private fun configureWithEofMark(fileName: String, text: String) {
        myFixture.configureByText(fileName, text)
        if (getEofInlays().isEmpty()) {
            EofMarkEditorListener(project).setupEditor(myFixture.editor)
        }
    }

    // 開いたことによる自動付与の検証は testMainEditorKindGetsEofInlay が担う。
    // ここではマーカーが重複せず 1 つだけ存在することを確認する。
    fun testEditorHasExactlyOneEofInlay() {
        configureWithEofMark("test.txt", "Hello")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
    }

    fun testInlayPositionAtEndOfDocument() {
        configureWithEofMark("test.txt", "Hello")

        val inlays = getEofInlays()
        assertEquals(myFixture.editor.document.textLength, inlays[0].offset)
    }

    fun testInlayOnEmptyFile() {
        configureWithEofMark("test.txt", "")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(0, inlays[0].offset)
    }

    fun testInlayOnMultilineFile() {
        configureWithEofMark("test.txt", "Line1\nLine2\nLine3")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(myFixture.editor.document.textLength, inlays[0].offset)
    }

    fun testInlayMovesAfterTextInsert() {
        configureWithEofMark("test.txt", "Hello")

        val originalLength = myFixture.editor.document.textLength
        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.insertString(originalLength, "\nNewLine")
        }

        val newLength = myFixture.editor.document.textLength
        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(newLength, inlays[0].offset)
    }

    fun testInlayMovesAfterTextDelete() {
        configureWithEofMark("test.txt", "Hello\nWorld\nEnd")

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.deleteString(5, 11) // "\nWorld" を削除
        }

        val newLength = myFixture.editor.document.textLength
        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(newLength, inlays[0].offset)
    }

    fun testCursorCannotMovePastEofMarker() {
        configureWithEofMark("test.txt", "Hello")
        val editor = myFixture.editor
        // マーカーが無いとカーソルは元々末尾より先へ進まず、ガードの有無を検証できない
        assertEquals("前提: [EOF] マーカーが存在すること", 1, getEofInlays().size)
        val textLength = editor.document.textLength

        editor.caretModel.moveToOffset(textLength)
        val visualColumnAtEnd = editor.caretModel.visualPosition.column

        myFixture.performEditorAction("EditorRight")

        assertEquals(
            "Cursor visual position should not move past the end of document text",
            visualColumnAtEnd, editor.caretModel.visualPosition.column
        )
    }

    fun testInlayRendererIsEofMarkRenderer() {
        configureWithEofMark("test.txt", "Hello")

        val inlays = getEofInlays()
        assertTrue(inlays.isNotEmpty())
        assertTrue(inlays[0].renderer is EofMarkRenderer)
    }

    fun testCursorCannotMovePastEofMarkerWithTabs() {
        configureWithEofMark("test.txt", "Line1\n\tEnd")
        val editor = myFixture.editor
        // マーカーが無いとカーソルは元々末尾より先へ進まず、ガードの有無を検証できない
        assertEquals("前提: [EOF] マーカーが存在すること", 1, getEofInlays().size)
        val textLength = editor.document.textLength

        editor.caretModel.moveToOffset(textLength)
        val visualColumnAtEnd = editor.caretModel.visualPosition.column

        // タブ文字がある場合、visual column と文字数は異なる
        val lastLine = editor.document.lineCount - 1
        val lineStartOffset = editor.document.getLineStartOffset(lastLine)
        val charCount = textLength - lineStartOffset
        assertTrue(
            "With tabs, visual column ($visualColumnAtEnd) should differ from char count ($charCount)",
            visualColumnAtEnd > charCount
        )

        myFixture.performEditorAction("EditorRight")

        assertEquals(
            "Cursor visual position should not move past the end of document text with tabs",
            visualColumnAtEnd, editor.caretModel.visualPosition.column
        )
    }

    fun testEditorReleaseCleansUpInlayAndListener() {
        configureWithEofMark("test.txt", "Hello")
        val editor = myFixture.editor

        // 手動で listener を追加（postStartupActivity 由来とは別のインスタンス）
        val listener = EofMarkEditorListener(project)
        listener.setupEditor(editor)

        // setupEditor により inlay が追加されていることを確認
        val inlaysBefore = getEofInlays()
        assertEquals("setupEditor should add an additional inlay", 2, inlaysBefore.size)

        // editorReleased を呼んでクリーンアップ
        val event = EditorFactoryEvent(EditorFactory.getInstance(), editor)
        listener.editorReleased(event)

        // inlay が dispose されていることを確認
        val inlaysAfter = getEofInlays()
        assertEquals("editorReleased should dispose the inlay", 1, inlaysAfter.size)
    }

    fun testSetupEditorIsIdempotent() {
        val factory = EditorFactory.getInstance()
        val editor = factory.createEditor(factory.createDocument("Hello"), project, EditorKind.MAIN_EDITOR)
        try {
            val listener = EofMarkEditorListener(project)
            listener.setupEditor(editor)
            val afterFirst = eofInlaysOf(editor).size

            listener.setupEditor(editor)
            assertEquals(
                "setupEditor を 2 回呼んでもマーカーは増えない",
                afterFirst, eofInlaysOf(editor).size
            )

            // 2 回目の呼び出しで上書きされた inlay が orphan として残らないこと
            listener.editorReleased(EditorFactoryEvent(factory, editor))
            assertEquals(
                "editorReleased で listener が付与した分が完全に回収される",
                afterFirst - 1, eofInlaysOf(editor).size
            )
        } finally {
            factory.releaseEditor(editor)
        }
    }

    fun testNoCaretGuardWhenInlayCannotBeAdded() {
        val factory = EditorFactory.getInstance()
        val editor = factory.createEditor(factory.createDocument("Hello"), project, EditorKind.MAIN_EDITOR)
        // エディタを破棄してからマーカーの追加を試みる。addEofInlay は isDisposed で
        // 早期 return するため、マーカーは付かない。
        factory.releaseEditor(editor)
        assertTrue("前提: エディタが破棄されていること", editor.isDisposed)

        val listener = EofMarkEditorListener(project)
        listener.setupEditor(editor)

        assertFalse(
            "マーカーを追加できなかった場合はカーソル制御も登録しない",
            listener.hasCaretGuard(editor)
        )
    }

    fun testCaretGuardRegisteredWhenInlayAdded() {
        val factory = EditorFactory.getInstance()
        val editor = factory.createEditor(factory.createDocument("Hello"), project, EditorKind.MAIN_EDITOR)
        try {
            val listener = EofMarkEditorListener(project)
            listener.setupEditor(editor)

            assertTrue(
                "マーカーを追加できた場合はカーソル制御も登録する",
                listener.hasCaretGuard(editor)
            )
        } finally {
            factory.releaseEditor(editor)
        }
    }

    fun testDisposeCleansUpAllEditors() {
        val factory = EditorFactory.getInstance()
        val e1 = factory.createEditor(factory.createDocument("Hello"), project, EditorKind.MAIN_EDITOR)
        val e2 = factory.createEditor(factory.createDocument("World"), project, EditorKind.MAIN_EDITOR)
        try {
            val listener = EofMarkEditorListener(project)
            listener.setupEditor(e1)
            listener.setupEditor(e2)
            val before1 = eofInlaysOf(e1).size
            val before2 = eofInlaysOf(e2).size
            assertTrue("前提: 両方のエディタにマーカーが付いていること", before1 > 0 && before2 > 0)
            assertTrue("前提: 両方にカーソル制御が登録されていること",
                listener.hasCaretGuard(e1) && listener.hasCaretGuard(e2))

            // プラグインのアンロード時に相当する後片付け
            Disposer.dispose(listener)

            assertEquals("dispose で e1 のマーカーが回収される", before1 - 1, eofInlaysOf(e1).size)
            assertEquals("dispose で e2 のマーカーが回収される", before2 - 1, eofInlaysOf(e2).size)
            assertFalse("dispose で e1 のカーソル制御が解除される", listener.hasCaretGuard(e1))
            assertFalse("dispose で e2 のカーソル制御が解除される", listener.hasCaretGuard(e2))
        } finally {
            factory.releaseEditor(e1)
            factory.releaseEditor(e2)
        }
    }

    /**
     * dispose が CaretModel から実際に CaretListener を解除しているかを挙動で検証する。
     *
     * hasCaretGuard() は内部マップを見るだけなので、removeCaretListener を呼ばずに
     * マップを clear するだけでも通ってしまう。しかしアンロードのリークは
     * 「CaretModel にリスナーが残っていること」自体が原因なので、そこを直接確かめる。
     */
    fun testDisposeUnregistersCaretListenerFromEditor() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
        val textLength = editor.document.textLength

        val listener = EofMarkEditorListener(project)
        listener.setupEditor(editor)

        // 前提: ガードが効いており、末尾より先へ進めない
        editor.caretModel.moveToOffset(textLength)
        val clampedColumn = editor.caretModel.visualPosition.column
        myFixture.performEditorAction("EditorRight")
        assertEquals(
            "前提: dispose 前はカーソルがマーカーの先へ進まない",
            clampedColumn, editor.caretModel.visualPosition.column
        )

        Disposer.dispose(listener)

        // listener とは無関係にマーカーを付け直す。ガードが残っていればここでも clamp される。
        editor.inlayModel.addInlineElement(textLength, true, EofMarkRenderer(editor))
        editor.caretModel.moveToOffset(textLength)
        myFixture.performEditorAction("EditorRight")

        assertTrue(
            "dispose 後は CaretModel からリスナーが外れ、カーソルがマーカーの先へ進む",
            editor.caretModel.visualPosition.column > clampedColumn
        )
    }

    fun testUntypedEditorGetsNoEofInlay() {
        val factory = EditorFactory.getInstance()
        val document = factory.createDocument("Hello")
        // 引数なしの createEditor は EditorTextField.createEditor() と同じ呼び出しで、
        // EditorKind.UNTYPED になる。コミットメッセージ欄はこの経路で生成される。
        val editor = factory.createEditor(document, project)
        try {
            assertEquals(EditorKind.UNTYPED, editor.editorKind)

            EofMarkEditorListener(project).editorCreated(EditorFactoryEvent(factory, editor))

            assertEquals(
                "コミットメッセージ欄相当のエディタには [EOF] を表示しない",
                0, eofInlaysOf(editor).size
            )
        } finally {
            factory.releaseEditor(editor)
        }
    }

    fun testMainEditorKindGetsEofInlay() {
        val factory = EditorFactory.getInstance()
        val document = factory.createDocument("Hello")
        val editor = factory.createEditor(document, project, EditorKind.MAIN_EDITOR)
        try {
            // 本番の postStartupActivity が登録したリスナーが editorCreated を受けて付与する
            val inlays = eofInlaysOf(editor)
            assertEquals("メインエディタには [EOF] を表示する", 1, inlays.size)
            assertEquals(document.textLength, inlays[0].offset)
        } finally {
            factory.releaseEditor(editor)
        }
    }

    fun testIsEofMarkTargetAcceptsOnlyMainEditor() {
        val factory = EditorFactory.getInstance()
        for (kind in EditorKind.values()) {
            val editor = factory.createEditor(factory.createDocument("Hello"), project, kind)
            try {
                assertEquals(
                    "EditorKind.$kind",
                    kind == EditorKind.MAIN_EDITOR,
                    EofMarkEditorListener.isEofMarkTarget(editor)
                )
            } finally {
                factory.releaseEditor(editor)
            }
        }
    }
}
