package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EofMarkRendererTest : BasePlatformTestCase() {

    fun testCalcWidthInPixelsReturnsPositiveValue() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
        val renderer = EofMarkRenderer(editor)

        val inlay = editor.inlayModel.addAfterLineEndElement(
            editor.document.textLength,
            true,
            renderer
        )!!
        val width = renderer.calcWidthInPixels(inlay)
        assertTrue("Width should be positive, got: $width", width > 0)
    }

    fun testGetColorMatchesLineNumbersColorOrGray() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
        val renderer = EofMarkRenderer(editor)

        val expectedColor = editor.colorsScheme.getColor(EditorColors.LINE_NUMBERS_COLOR)
            ?: java.awt.Color.GRAY
        val actualColor = renderer.getColor()
        assertEquals(expectedColor, actualColor)
    }

    fun testEofTextConstant() {
        assertEquals("[EOF]", EofMarkRenderer.EOF_TEXT)
    }
}
