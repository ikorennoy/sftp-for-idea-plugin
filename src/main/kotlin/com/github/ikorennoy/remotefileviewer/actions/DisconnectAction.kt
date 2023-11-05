package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class DisconnectAction : DumbAwareAction(
    { "Disconnect" },
    AllIcons.CodeWithMe.CwmTerminate
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProcessIOExecutorService.INSTANCE.execute {
            project.service<RemoteOperations>().close()
            ApplicationManager.getApplication().messageBus.syncPublisher(RemoteConnectionListener.TOPIC)
                .connectionStatusChanged()
        }
    }
}
