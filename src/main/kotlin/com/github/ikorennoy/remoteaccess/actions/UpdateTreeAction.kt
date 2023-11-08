package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class UpdateTreeAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.updateTree.text"),
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fsTree = e.getData(RemoteFileSystemTree.DATA_KEY) ?: return
        val remoteOperations = RemoteOperations.getInstance(project)
        // if we're not connected ask for password and reconnect
        if (!remoteOperations.isInitializedAndConnected()) {
            val tryConnect = prepareConfiguration(project)
            if (tryConnect) {
                remoteOperations.initWithModalDialogue()
            }
        }
        fsTree.update()
    }
}
