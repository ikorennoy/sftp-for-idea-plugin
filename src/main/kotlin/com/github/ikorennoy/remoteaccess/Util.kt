package com.github.ikorennoy.remoteaccess

import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.tree.TreeStateListener
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessConfigurable
import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState.*
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

fun notifyUpdateFullTree() {
    ApplicationManager.getApplication().messageBus.syncPublisher(TreeStateListener.TOPIC)
        .updateFullTree()
}

fun notifyUpdateNode(node: RemoteFileInformation) {
    ApplicationManager.getApplication().messageBus.syncPublisher(TreeStateListener.TOPIC)
        .updateTreeNode(node)
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
        // show password or passphrase prompt dialogue
        if (conf.authenticationType == AuthenticationType.PASSWORD) {
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
        } else {
            val password = Messages.showPasswordDialog(
                RemoteFileAccessBundle.message("dialog.RemoteFileAccess.enterPassphrase.message"),
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
    }
    return tryConnect
}

fun convertBytesToHumanReadable(bytes: Long): String {
    return if (bytes < 1000) {
        "$bytes bytes"
    } else if (bytes > 1000 && bytes < 1000 * 1000) {
        "${bytes / 1000} kb"
    } else {
        "${bytes / 1000 / 1000} mb"
    }
}
