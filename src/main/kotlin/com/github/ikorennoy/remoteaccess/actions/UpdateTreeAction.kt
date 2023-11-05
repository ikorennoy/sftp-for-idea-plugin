package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class UpdateTreeAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.updateTree.text"),
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val remoteOperations = RemoteOperations.getInstance(project)
        val tryConnect = prepareConfiguration(project)
        if (tryConnect) {
            remoteOperations.initWithModalDialogue(project)
        }
        notifyRebuildTree()
    }
}
