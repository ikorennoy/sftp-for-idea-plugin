package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.LargeFileEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorNotificationPanel
import java.awt.BorderLayout

class RemoteEditEditorPanel(
    editor: FileEditor,
) : EditorNotificationPanel(editor, getToolbarBackground(), null, Status.Info) {

    init {
        val saveAction = UploadAction(editor)
        saveAction.registerCustomShortcutSet(
            ActionManager.getInstance().getAction("SaveAll").shortcutSet,
            editor.component
        )
        myLinksPanel.layout = BorderLayout()
        val toolbarGroup = DefaultActionGroup()
        toolbarGroup.add(saveAction)
        val actionToolbar: ActionToolbarImpl = ActionManager.getInstance()
            .createActionToolbar("RemoteEditorEditPanelToolbar", toolbarGroup, true) as ActionToolbarImpl
        actionToolbar.targetComponent = myLinksPanel
        myLinksPanel.add(actionToolbar)
    }

    private class UploadAction(private val editor: FileEditor) : DumbAwareAction(
        RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.upload.text"),
        AllIcons.Actions.MenuSaveall
    ) {

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun update(e: AnActionEvent) {
            if (editor is LargeFileEditorProvider.LargeTextFileEditor) {
                e.presentation.isEnabled = false
            } else {
                val project = e.project ?: return
                FileDocumentManager.getInstance()
                val files = FileEditorManager.getInstance(project).selectedFiles
                for (file in files) {
                    if (file is TempVirtualFile) {
                        e.presentation.isEnabled = file.isWritable
                    }
                }
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val documentManager = FileDocumentManager.getInstance()
            val files = FileEditorManager.getInstance(project).selectedFiles
            for (file in files) {
                if (file is TempVirtualFile) {
                    val confirmationMessage = createConfirmationMessage(file.remoteFile)
                    val returnValue = Messages.showOkCancelDialog(
                        confirmationMessage,
                        RemoteFileAccessBundle.message("dialog.RemoteFileAccess.upload.confirmation.title"),
                        RemoteFileAccessBundle.message("dialog.RemoteFileAccess.upload.confirmation.okText"),
                        CommonBundle.getCancelButtonText(), Messages.getQuestionIcon()
                    )
                    if (returnValue == Messages.OK) {
                        val document = documentManager.getCachedDocument(file)
                        if (document != null) {
                            documentManager.saveDocument(document)
                        }
                        RemoteEditService.getInstance(project).uploadFileToRemote(file)
                    }
                }
            }
        }

        private fun createConfirmationMessage(file: RemoteFileInformation): String {
            return RemoteFileAccessBundle.message("dialog.RemoteFileAccess.upload.confirmation.message", file.getName())
        }
    }
}
