package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

class EofMarkRenderer(private val editor: Editor) : EditorCustomElementRenderer {

    companion object {
        const val EOF_TEXT = "[EOF]"
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val font = getFont()
        val metrics = editor.contentComponent.getFontMetrics(font)
        return metrics.stringWidth(EOF_TEXT)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val font = getFont()
        g.font = font
        g.color = getColor()
        val metrics = g.fontMetrics
        val y = targetRegion.y + metrics.ascent
        g.drawString(EOF_TEXT, targetRegion.x, y)
    }

    private fun getFont(): Font {
        return editor.colorsScheme.getFont(EditorFontType.PLAIN)
    }

    fun getColor(): Color {
        val scheme = editor.colorsScheme
        return scheme.getColor(EditorColors.LINE_NUMBERS_COLOR) ?: Color.GRAY
    }
}
