package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

class NewFileAction : DumbAwareAction(
    ActionsBundle.messagePointer("action.FileChooser.NewFile.text"),
    AllIcons.FileTypes.AddAny
) {

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        var parent = fsTree.getNewFileParent() ?: return

        if (!parent.isDirectory()) {
            parent = parent.getParent() ?: return
        }

        var name: String?
        while (true) {
            name = Messages.showInputDialog(
                UIBundle.message("create.new.file.enter.new.file.name.prompt.text"),
                UIBundle.message("new.file.dialog.title"),
                Messages.getQuestionIcon()
            )
            if (name == null) {
                break
            }
            name = name.trim()
            if (name.isEmpty()) {
                Messages.showMessageDialog(
                    UIBundle.message("create.new.file.file.name.cannot.be.empty.error.message"),
                    UIBundle.message("error.dialog.title"),
                    Messages.getErrorIcon()
                )
                continue
            }
            fsTree.createNewFile(parent, name)
            break
        }
    }
}
