package com.github.ikorennoy.remoteaccess

import com.github.ikorennoy.remoteaccess.tree.ConnectionStatusListener
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessConfigurable
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

fun notifyConnectionStatusChanged() {
    ApplicationManager.getApplication().messageBus.syncPublisher(ConnectionStatusListener.TOPIC)
        .connectionStatusChanged()
}

fun prepareConfiguration(project: Project): Boolean {
    // first try to connect on window open
    val conf = RemoteFileAccessSettingsState.getInstance(project)
    var tryConnect = true

    if (conf.isNotValid()) {
        // show full configuration dialogue
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            RemoteFileAccessConfigurable::class.java
        )
        tryConnect = !conf.isNotValid() // user cancelled settings dialog
    } else {
        // show password prompt dialogue
        val password = Messages.showPasswordDialog(
            RemoteFileAccessBundle.message("dialog.RemoteFileAccess.enterPassword.message"),
            RemoteFileAccessBundle.message(
                "dialog.RemoteFileAccess.enterPassword.title",
                conf.username,
                conf.host,
                conf.port
            )
        )

        if (password != null) {
            conf.password = password.toCharArray()
        } else {
            // it means user cancelled password enter dialog
            tryConnect = false
        }
    }
    return tryConnect
}
