package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.remote.RemoteConnectionListener
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages

class UpdateTreeAction: DumbAwareAction({"Update Tree"}, AllIcons.Actions.Refresh) {

    override fun actionPerformed(e: AnActionEvent) {
        val configuration = service<RemoteFileViewerSettingsState>()
        val connManager = service<RemoteOperations>()
        val project = e.project
        var tryConnect = true

        if (!connManager.isInitializedAndConnected()) {
            if (configuration.isNotValid()) {
                // show full configuration dialogue
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
                )
                tryConnect = !configuration.isNotValid() // user cancelled settings dialog
            } else {
                if (configuration.password.isEmpty()) {
                    // show password prompt dialogue
                    val password = Messages.showPasswordDialog(
                        "Enter a password:",
                        "Connecting to: ${configuration.username}@${configuration.host}:${configuration.port}",
                    )

                    if (password != null) {
                        configuration.password = password.toCharArray()
                    } else {
                        // it means user cancelled password enter dialog
                        tryConnect = false
                    }
                }
            }
            if (tryConnect) {
                connManager.init()
            }
        }
        ApplicationManager.getApplication().messageBus.syncPublisher(RemoteConnectionListener.TOPIC).connectionStatusChanged()
    }
}
