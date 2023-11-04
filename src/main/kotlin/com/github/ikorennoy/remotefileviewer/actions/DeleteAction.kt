package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.UIBundle

class DeleteAction :
    DumbAwareAction(ActionsBundle.messagePointer("action.FileChooser.Delete.text"), AllIcons.Actions.Cancel) {

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        val fileToDelete = fsTree.getSelectedFile() ?: return

        val confirmationMessage = createConfirmationMessage(fileToDelete)

        val returnValue = Messages.showOkCancelDialog(
            confirmationMessage, UIBundle.message("delete.dialog.title"), ApplicationBundle.message("button.delete"),
            CommonBundle.getCancelButtonText(), Messages.getQuestionIcon()
        )
        if (returnValue != Messages.OK) return

        val failReason = fsTree.deleteFile(fileToDelete)
    }

    private fun createConfirmationMessage(file: VirtualFile): String {
        return if (file.isDirectory) {
            UIBundle.message(
                "are.you.sure.you.want.to.delete.selected.folder.confirmation.message",
                file.getName()
            )
        } else {
            UIBundle.message(
                "are.you.sure.you.want.to.delete.selected.file.confirmation.message",
                file.getName()
            )
        }
    }

}