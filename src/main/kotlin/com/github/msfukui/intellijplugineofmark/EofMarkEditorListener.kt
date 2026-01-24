package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class EofMarkEditorListener(private val project: Project) : EditorFactoryListener {

    private val editorInlays = mutableMapOf<Editor, Inlay<*>>()
    private val editorCaretListeners = mutableMapOf<Editor, CaretListener>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        setupEditor(editor)
    }

    fun setupEditor(editor: Editor) {
        addEofInlay(editor)
        addCaretGuard(editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        editorInlays.remove(editor)?.dispose()
        editorCaretListeners.remove(editor)?.let {
            editor.caretModel.removeCaretListener(it)
        }
    }

    fun addEofInlay(editor: Editor) {
        if (editor.isDisposed) return
        val offset = editor.document.textLength
        val inlay = editor.inlayModel.addInlineElement(
            offset,
            true,
            EofMarkRenderer(editor)
        )
        if (inlay != null) {
            editorInlays[editor] = inlay
        }
    }

    private fun addCaretGuard(editor: Editor) {
        var adjusting = false
        val listener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                if (adjusting) return
                val caret = event.caret ?: return
                val textLength = editor.document.textLength
                if (caret.offset != textLength) return

                val expectedColumn = editor.offsetToVisualPosition(textLength).column
                if (caret.visualPosition.column > expectedColumn) {
                    adjusting = true
                    try {
                        caret.moveToVisualPosition(VisualPosition(caret.visualPosition.line, expectedColumn))
                    } finally {
                        adjusting = false
                    }
                }
            }
        }
        editor.caretModel.addCaretListener(listener)
        editorCaretListeners[editor] = listener
    }
}

class EofMarkProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeAndWait {
            val listener = EofMarkEditorListener(project)
            // 既に開かれているエディタにもマーカーとカーソル制御を追加
            for (editor in EditorFactory.getInstance().allEditors) {
                if (editor.project == project) {
                    listener.setupEditor(editor)
                }
            }
            // 今後作成されるエディタ用にリスナーを登録
            EditorFactory.getInstance().addEditorFactoryListener(listener, project)
        }
    }
}
