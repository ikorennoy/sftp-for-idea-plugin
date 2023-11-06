package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.edit.TempVirtualFile
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

class SelectOpenedFileAction : DumbAwareAction(
    ActionsBundle.messagePointer("action.SelectOpenedFileInProjectView.text"),
    AllIcons.General.Locate
) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabled = RemoteOperations.getInstance(project).isInitializedAndConnected()
    }

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
