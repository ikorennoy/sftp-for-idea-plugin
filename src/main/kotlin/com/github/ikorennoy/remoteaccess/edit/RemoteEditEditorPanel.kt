package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.EditorNotificationPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension

class RemoteEditEditorPanel(
    editor: FileEditor
) : EditorNotificationPanel(editor, getToolbarBackground(), null, Status.Info) {

    init {
        val saveAction = UploadAction()
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

    private fun createButton(action: AnAction): ActionButton {
        val icon = action.templatePresentation.icon
        val width = icon.iconWidth
        val height = icon.iconHeight
        val button = ActionButton(action, null, "unknown", Dimension(width, height))
        button.setIconInsets(JBUI.insets(0, 3))
        return button
    }

    private class UploadAction : DumbAwareAction(
        RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.upload.text"),
        AllIcons.Actions.MenuSaveall
    ) {

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun update(e: AnActionEvent) {
            val project = e.project ?: return
            FileDocumentManager.getInstance()
            val files = FileEditorManager.getInstance(project).selectedFiles
            for (file in files) {
                if (file is TempVirtualFile) {
                    e.presentation.isEnabled = file.isWritable
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
