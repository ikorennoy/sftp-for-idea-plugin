package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.EditorNotificationPanel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.FlowLayout

class ProjectionFileEditorPanel(
    private val file: LocalVirtualFile,
    private val editor: FileEditor
) : EditorNotificationPanel(editor, getToolbarBackground(), null, Status.Info) {

    init {
        myLinksPanel.layout = FlowLayout()
        val saveAction = UploadAction()
        // todo get rid of it if it's possible
        saveAction.registerCustomShortcutSet(ActionManager.getInstance().getAction("SaveAll").shortcutSet, editor.component)
        myLinksPanel.add(createButton(UploadAction()))

    }

    private fun createButton(action: AnAction): ActionButton {
        val icon = action.templatePresentation.icon
        val width = icon.iconWidth
        val height = icon.iconHeight
        val button = ActionButton(action, null, "unknown", Dimension(width, height))
        button.setIconInsets(JBUI.insets(0, 3))
        return button
    }


    private class UploadAction : DumbAwareAction() {
        init {
            ActionUtil.copyFrom(this, "RemoteFileSystem.UploadCurrentFileProjection")
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val documentManager = FileDocumentManager.getInstance()
            val files = FileEditorManager.getInstance(project).selectedFiles
            for (file in files) {
                if (file is LocalVirtualFile) {
                    val document = documentManager.getCachedDocument(file)
                    if (document != null) {
                        documentManager.saveDocument(document)
                    }
                    val syncService = service<FileProjectionStateService>()
                    syncService.uploadFileToRemote(project, file)
                }
            }
        }

    }
}
