package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.remoteEdit.TempVirtualFile
import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

class SelectOpenedFile : DumbAwareAction(
    ActionsBundle.messagePointer("action.SelectOpenedFileInProjectView.text"),
    AllIcons.General.Locate
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        val files = FileEditorManager.getInstance(project).selectedFiles
        for (file in files) {
            if (file is TempVirtualFile) {
                fsTree.select(file.remoteFile)
                return
            }
        }
    }
}
