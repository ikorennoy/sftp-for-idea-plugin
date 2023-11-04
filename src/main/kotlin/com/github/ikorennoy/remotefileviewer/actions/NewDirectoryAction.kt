package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

class NewDirectoryAction :
    DumbAwareAction(ActionsBundle.messagePointer("action.FileChooser.NewFolder.text"), AllIcons.Actions.NewFolder) {

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        var parent = fsTree.getNewFileParent() ?: return

        if (!parent.isDirectory) {
            parent = parent.parent
        }

        val newFolderName = Messages.showInputDialog(
            UIBundle.message("create.new.folder.enter.new.folder.name.prompt.text"),
            UIBundle.message("new.folder.dialog.title"),
            Messages.getQuestionIcon(),
        ) ?: return

        val failReason = fsTree.createNewDirectory(parent, newFolderName)
        if (failReason != null) {
            Messages.showMessageDialog(
                UIBundle.message(
                    "create.new.folder.could.not.create.folder.error.message",
                    newFolderName
                ), UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
            )
        }
    }
}