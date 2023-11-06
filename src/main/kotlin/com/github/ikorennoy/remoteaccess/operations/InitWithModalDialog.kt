package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessSettingsState
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class InitWithModalDialogTask(
    private val project: Project,
) : Task.Modal(
    project,
    RemoteFileAccessBundle.message(
        "dialog.RemoteFileAccess.enterPassword.title",
        RemoteFileAccessSettingsState.getInstance(project).username,
        RemoteFileAccessSettingsState.getInstance(project).host,
        RemoteFileAccessSettingsState.getInstance(project).port,
    ),
    false
) {

    @Volatile
    var failReason: Exception? = null

    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true
        val connectionHolder = ConnectionHolder.getInstance(project)
        failReason = connectionHolder.connect()
    }

    override fun onFinished() {
        reportError()
    }

    private fun reportError() {
        val conf = RemoteFileAccessSettingsState.getInstance(project)
        if (failReason != null) {
            Messages.showMessageDialog(
                RemoteFileAccessBundle.message(
                    "dialog.RemoteFileAccess.connection.error.message",
                    conf.username,
                    conf.host,
                    conf.port,
                    failReason?.message ?: RemoteFileAccessBundle.unknownReason()
                ),
                RemoteFileAccessBundle.message("dialog.RemoteFileAccess.connection.error.title"),
                Messages.getErrorIcon()
            )
        }
    }
}
