package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class EofMarkRendererTest : BasePlatformTestCase() {

    fun testCalcWidthInPixelsReturnsPositiveValue() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
        val renderer = EofMarkRenderer(editor)

        val inlay = editor.inlayModel.addInlineElement(
            editor.document.textLength,
            true,
            renderer
        )
        assertNotNull(inlay)
        val width = renderer.calcWidthInPixels(inlay!!)
        assertTrue("Width should be positive, got: $width", width > 0)
    }

    fun testGetColorReturnsNonNull() {
        myFixture.configureByText("test.txt", "Hello")
        val editor = myFixture.editor
        val renderer = EofMarkRenderer(editor)

        val color = renderer.getColor()
        assertNotNull("Color should not be null", color)
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
