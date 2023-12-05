package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.LargeFileEditorProvider
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

class RemoteEditEditorPanel(editor: FileEditor) : JPanel(BorderLayout(0, 0)) {

    init {
        val toolbarGroup = DefaultActionGroup()
        val saveAction = UploadFileAction()
        toolbarGroup.add(saveAction)

        saveAction.registerCustomShortcutSet(
            ActionManager.getInstance().getAction("SaveAll").shortcutSet,
            editor.component
        )

        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, toolbarGroup, true)

        actionToolbar.targetComponent = this
        actionToolbar.layoutPolicy = ActionToolbar.AUTO_LAYOUT_POLICY
        add(JBUI.Panels.simplePanel(actionToolbar.component), BorderLayout.EAST)
    }

    class UploadFileAction : DumbAwareAction(
        RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.upload.text"),
        AllIcons.Actions.MenuSaveall
    ) {

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun update(e: AnActionEvent) {
            val project = e.project ?: return
            val editor = FileEditorManager.getInstance(project).selectedEditor

            if (editor is LargeFileEditorProvider.LargeTextFileEditor) {
                e.presentation.isEnabled = false
            } else {
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
