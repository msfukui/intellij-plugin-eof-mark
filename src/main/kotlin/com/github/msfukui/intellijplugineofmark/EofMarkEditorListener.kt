package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class EofMarkEditorListener(private val project: Project) : EditorFactoryListener {

    private val editorInlays = mutableMapOf<Editor, Inlay<*>>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        addEofInlay(editor)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateEofInlay(editor)
            }
        }, project)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        editorInlays.remove(editor)?.dispose()
    }

    fun addEofInlay(editor: Editor) {
        if (editor.isDisposed) return
        val offset = editor.document.textLength
        val inlay = editor.inlayModel.addAfterLineEndElement(
            offset,
            true,
            EofMarkRenderer(editor)
        )
        if (inlay != null) {
            editorInlays[editor] = inlay
        }
    }

    private fun updateEofInlay(editor: Editor) {
        editorInlays.remove(editor)?.dispose()
        addEofInlay(editor)
    }
}

class EofMarkProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeAndWait {
            val listener = EofMarkEditorListener(project)
            // 既に開かれているエディタにもマーカーを追加
            for (editor in EditorFactory.getInstance().allEditors) {
                if (editor.project == project) {
                    listener.addEofInlay(editor)
                }
            }
            // 今後作成されるエディタ用にリスナーを登録
            EditorFactory.getInstance().addEditorFactoryListener(listener, project)
        }
    }
}
