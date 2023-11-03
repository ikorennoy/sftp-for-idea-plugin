package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileSystemTree
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

class NewFileAction :
    DumbAwareAction(ActionsBundle.messagePointer("action.FileChooser.NewFile.text"), AllIcons.FileTypes.AddAny) {

    override fun actionPerformed(e: AnActionEvent) {
        val fsTree = e.getData(FileSystemTree.DATA_KEY) ?: return
        var parent = fsTree.newFileParent ?: return

        if (!parent.isDirectory) {
           parent = parent.parent
        }

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
            var type = FileTypeManager.getInstance().getFileTypeByFileName(name)
            // we need this because the code in FileSystemTreeImpl for UnknownFileType creates a filename with '.' at the end
            if (type == UnknownFileType.INSTANCE) {
                type = PlainTextFileType.INSTANCE
            }
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
}
