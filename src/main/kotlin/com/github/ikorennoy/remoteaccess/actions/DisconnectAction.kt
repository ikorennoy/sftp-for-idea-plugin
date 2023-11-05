package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.notifyRebuildTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class DisconnectAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.disconnect.text"),
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
