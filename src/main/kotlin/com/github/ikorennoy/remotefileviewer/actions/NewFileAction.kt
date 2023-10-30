package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

class NewFileAction : AnAction({ "New File" }, AllIcons.Actions.New) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(FileSystemTree.DATA_KEY) ?: return
        val parent = fsTree.newFileParent
        var name: String?
        while (true) {
            name = Messages.showInputDialog(
                UIBundle.message("create.new.file.enter.new.file.name.prompt.text"),
                UIBundle.message("new.file.dialog.title"),
                Messages.getQuestionIcon()
            )
            if (name == null) {
                return
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
            val type = FileTypeManager.getInstance().getFileTypeByFileName(name)
            val failReason =
                (fsTree as FileSystemTreeImpl).createNewFile(parent, name, type, null)
            if (failReason != null) {
                Messages.showMessageDialog(
                    UIBundle.message(
                        "create.new.file.could.not.create.file.error.message",
                        name
                    ), UIBundle.message("error.dialog.title"), Messages.getErrorIcon()
                )
                continue
            }
            return
        }
    }

    override fun update(e: AnActionEvent) {
        val fsTree = e.getData(FileSystemTree.DATA_KEY) ?: return
        val parent = fsTree.newFileParent
        e.presentation.setVisible(true)
        e.presentation.isEnabled = parent != null && parent.isWritable && parent.isWritable
    }
}