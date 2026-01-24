package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EofMarkEditorListenerTest : BasePlatformTestCase() {

    private fun getEofInlays(): List<Inlay<*>> {
        val editor = myFixture.editor
        return editor.inlayModel.getInlineElementsInRange(0, editor.document.textLength)
            .filter { it.renderer is EofMarkRenderer }
    }

    fun testEditorHasEofInlayAfterOpen() {
        myFixture.configureByText("test.txt", "Hello")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
    }

    fun testInlayPositionAtEndOfDocument() {
        myFixture.configureByText("test.txt", "Hello")

        val inlays = getEofInlays()
        assertEquals(myFixture.editor.document.textLength, inlays[0].offset)
    }

    fun testInlayOnEmptyFile() {
        myFixture.configureByText("test.txt", "")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(0, inlays[0].offset)
    }

    fun testInlayOnMultilineFile() {
        myFixture.configureByText("test.txt", "Line1\nLine2\nLine3")

        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(myFixture.editor.document.textLength, inlays[0].offset)
    }

    fun testInlayMovesAfterTextInsert() {
        myFixture.configureByText("test.txt", "Hello")

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
        myFixture.configureByText("test.txt", "Hello\nWorld\nEnd")

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.deleteString(5, 11) // "\nWorld" を削除
        }

        val newLength = myFixture.editor.document.textLength
        val inlays = getEofInlays()
        assertEquals(1, inlays.size)
        assertEquals(newLength, inlays[0].offset)
    }

    fun testCursorCannotMovePastEofMarker() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
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
        myFixture.configureByText("test.txt", "Hello")

        val inlays = getEofInlays()
        assertTrue(inlays.isNotEmpty())
        assertTrue(inlays[0].renderer is EofMarkRenderer)
    }

    fun testCursorCannotMovePastEofMarkerWithTabs() {
        myFixture.configureByText("test.txt", "Line1\n\tEnd")
        val editor = myFixture.editor
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
        myFixture.configureByText("test.txt", "Hello")
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
}
