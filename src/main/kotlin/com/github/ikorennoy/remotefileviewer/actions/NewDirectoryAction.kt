package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

class NewDirectoryAction : DumbAwareAction(
    ActionsBundle.messagePointer("action.FileChooser.NewFolder.text"),
    AllIcons.Actions.NewFolder
) {

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        var parent = fsTree.getNewFileParent() ?: return

        if (!parent.isDirectory()) {
            parent = parent.getParent() ?: return
        }

        var newDirectoryName: String
        while (true) {
            newDirectoryName = Messages.showInputDialog(
                UIBundle.message("create.new.folder.enter.new.folder.name.prompt.text"),
                UIBundle.message("new.folder.dialog.title"),
                Messages.getQuestionIcon(),
            ) ?: return

            newDirectoryName = newDirectoryName.trim()

            if (newDirectoryName.isEmpty()) {
                Messages.showMessageDialog(
                    UIBundle.message("create.new.folder.folder.name.cannot.be.empty.error.message"),
                    UIBundle.message("error.dialog.title"),
                    Messages.getErrorIcon()
                )
                continue
            }
            fsTree.createNewDirectory(parent, newDirectoryName)
            break
        }
    }
}
