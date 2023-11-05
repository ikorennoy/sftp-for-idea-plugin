package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.operations.ConnectionListener
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.prepareConfiguration
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages

class UpdateTreeAction : DumbAwareAction(
    { "Update Tree" },
    AllIcons.Actions.Refresh
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val remoteOperations = project.service<RemoteOperations>()
        val tryConnect = prepareConfiguration(project)
        if (tryConnect) {
            remoteOperations.init()
        }
        ApplicationManager.getApplication().messageBus.syncPublisher(ConnectionListener.TOPIC)
            .connectionStatusChanged()
    }
}
