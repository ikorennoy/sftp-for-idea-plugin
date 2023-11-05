package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class DisconnectAction : DumbAwareAction(
    { "Disconnect" },
    AllIcons.CodeWithMe.CwmTerminate
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProcessIOExecutorService.INSTANCE.execute {
            RemoteOperations.getInstance(project).close()
            notifyRebuildTree()
        }
    }
}
