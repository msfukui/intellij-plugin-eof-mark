package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Inlay
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

    fun testInlayRendererIsEofMarkRenderer() {
        myFixture.configureByText("test.txt", "Hello")

        val inlays = getEofInlays()
        assertTrue(inlays.isNotEmpty())
        assertTrue(inlays[0].renderer is EofMarkRenderer)
    }
}
