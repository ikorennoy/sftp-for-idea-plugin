package com.github.ikorennoy.remoteaccess

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

fun prepareConfiguration(project: Project): Boolean {
    // first try to connect on window open
    val configuration = service<RemoteFileAccessSettingsState>()
    var tryConnect = true

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
    return tryConnect
}
