package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class EofMarkEditorListener : EditorFactoryListener {

    private val editorInlays = mutableMapOf<Editor, Inlay<*>>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        addEofInlay(editor)

        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateEofInlay(editor)
            }
        })
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        editorInlays.remove(editor)?.dispose()
    }

    private fun addEofInlay(editor: Editor) {
        val offset = editor.document.textLength
        val inlay = editor.inlayModel.addAfterLineEndElement(
            offset,
            false,
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
        val listener = EofMarkEditorListener()
        EditorFactory.getInstance().addEditorFactoryListener(listener, project)
    }
}
