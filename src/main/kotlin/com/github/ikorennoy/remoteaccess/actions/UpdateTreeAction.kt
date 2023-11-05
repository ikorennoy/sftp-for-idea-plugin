package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class UpdateTreeAction : DumbAwareAction(
    { "Update Tree" },
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val remoteOperations = project.service<RemoteOperations>()
        val tryConnect = prepareConfiguration(project)
        if (tryConnect) {
            remoteOperations.initWithModalDialogue(project)
        }
        notifyRebuildTree()
    }
}
