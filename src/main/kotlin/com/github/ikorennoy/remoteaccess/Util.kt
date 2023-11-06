package com.github.ikorennoy.remoteaccess

import com.github.ikorennoy.remoteaccess.operations.ConnectionListener
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.settings.ui.RemoteFileAccessConfigurable
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

fun notifyRebuildTree() {
    ApplicationManager.getApplication().messageBus.syncPublisher(ConnectionListener.TOPIC)
        .connectionStatusChanged()
}

fun prepareConfiguration(project: Project): Boolean {
    // first try to connect on window open
    val configuration = RemoteFileAccessSettingsState.getInstance()
    var tryConnect = true

    if (configuration.isNotValid()) {
        // show full configuration dialogue
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            RemoteFileAccessConfigurable::class.java
        )
        tryConnect = !configuration.isNotValid() // user cancelled settings dialog
    } else {
        if (configuration.password.isEmpty()) {
            // show password prompt dialogue
            val password = Messages.showPasswordDialog(
                RemoteFileAccessBundle.message("dialog.RemoteFileAccess.enterPassword.message"),
                RemoteFileAccessBundle.message(
                    "dialog.RemoteFileAccess.enterPassword.title",
                    configuration.username,
                    configuration.host,
                    configuration.port
                )
            )

            if (password != null) {
                configuration.password = password.toCharArray()
            } else {
                // it means user cancelled password enter dialog
                tryConnect = false
            }
        }
    }
    return tryConnect
}
