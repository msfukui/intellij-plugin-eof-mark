package com.github.msfukui.intellijplugineofmark

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.annotations.TestOnly

class EofMarkEditorListener(private val project: Project) : EditorFactoryListener {

    private val editorInlays = mutableMapOf<Editor, Inlay<*>>()
    private val editorCaretListeners = mutableMapOf<Editor, CaretListener>()

    companion object {
        /**
         * 実ファイルを開いたメインエディタのみを対象とする。
         *
         * コミットメッセージ欄などの EditorTextField は UNTYPED、コンソールは CONSOLE、
         * 差分ビューアは DIFF、検索結果プレビューは PREVIEW となり除外される。
         * 読み取り専用のファイル（ライブラリのソース、逆コンパイルした .class など）は
         * MAIN_EDITOR なので対象に含まれる。
         *
         * editorKind は EditorImpl のコンストラクタ引数なので editorCreated の時点で確定している。
         * EditorUtil.isRealFileEditor() は TextEditorImpl がコンストラクタ内で putTextEditor() を
         * 呼ぶより前に editorCreated が発火するため、本物のファイルエディタでも false を返す。
         * 同じ理由で editor.virtualFile もこの時点では未設定であり、いずれも判定には使えない。
         */
        fun isEofMarkTarget(editor: Editor): Boolean =
            !editor.isDisposed && editor.editorKind == EditorKind.MAIN_EDITOR
    }

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        if (!isEofMarkTarget(editor)) return
        setupEditor(editor)
    }

    /**
     * マーカーとカーソル制御を追加する。
     *
     * 同一エディタへの二重適用を防ぐ。二重に呼ばれると前回の inlay を dispose せずに
     * マップを上書きしてしまい、回収されない orphan が残る。CaretListener も同様に
     * 二重登録され、editorReleased では最後の 1 つしか解除できない。
     */
    fun setupEditor(editor: Editor) {
        if (editorInlays.containsKey(editor)) return
        // マーカーを追加できなかった場合はカーソル制御も登録しない。
        // 自分が何も描いていないのにカーソルを拘束するのを避ける。
        if (!addEofInlay(editor)) return
        addCaretGuard(editor)
    }

    /** カーソル制御が登録されているか。マーカーとの整合性を検証するためにテストへ公開している。 */
    @TestOnly
    internal fun hasCaretGuard(editor: Editor): Boolean = editorCaretListeners.containsKey(editor)

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editor.project != project) return
        editorInlays.remove(editor)?.dispose()
        editorCaretListeners.remove(editor)?.let {
            editor.caretModel.removeCaretListener(it)
        }
    }

    /** @return マーカーを追加できた場合 true */
    private fun addEofInlay(editor: Editor): Boolean {
        if (editor.isDisposed) return false
        val offset = editor.document.textLength
        val inlay = editor.inlayModel.addInlineElement(
            offset,
            true,
            EofMarkRenderer(editor)
        ) ?: return false
        editorInlays[editor] = inlay
        return true
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
                if (editor.project == project && EofMarkEditorListener.isEofMarkTarget(editor)) {
                    listener.setupEditor(editor)
                }
            }
            // 今後作成されるエディタ用にリスナーを登録
            EditorFactory.getInstance().addEditorFactoryListener(listener, project)
        }
    }
}
